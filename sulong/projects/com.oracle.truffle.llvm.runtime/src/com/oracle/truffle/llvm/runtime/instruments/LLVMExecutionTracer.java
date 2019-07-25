/*
 * Copyright (c) 2019, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.runtime.instruments;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.LLVMIVarBitLarge;
import com.oracle.truffle.llvm.runtime.LLVMIVarBitSmall;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMTypes;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public final class LLVMExecutionTracer {

    private final TraceContext traceContext;

    public LLVMExecutionTracer() {
        this.traceContext = new TraceContext();
    }

    @SuppressWarnings("unchecked")
    public void initialize(TruffleLanguage.Env env) {
        final Instrumenter instrumenter = env.lookup(Instrumenter.class);
        assert instrumenter != null;

        final SourceSectionFilter.Builder builder = SourceSectionFilter.newBuilder();
        builder.includeInternal(true);

        builder.tagIs(LLVMTags.ALL_TAGS);

        final String selectedFunctions = env.getOptions().get(SulongEngineOption.TRACE_IR_FUNCTIONS);
        if (!"all".equals(selectedFunctions)) {
            if (selectedFunctions == null || selectedFunctions.isEmpty()) {
                throw new IllegalArgumentException("Invalid selection of trace functions: " + selectedFunctions);
            }
            final String[] enabledFunctions = Arrays.stream(selectedFunctions.split(":")).map(LLVMIdentifier::toGlobalIdentifier).toArray(String[]::new);
            builder.rootNameIs(rootName -> {
                if (rootName == null) {
                    return false;
                }

                final String llvmName = LLVMIdentifier.toGlobalIdentifier(rootName);
                for (String enabledRootName : enabledFunctions) {
                    if (llvmName.equals(enabledRootName)) {
                        return true;
                    }
                }
                return false;
            });
        }

        final SourceSectionFilter llvmNodeFilter = builder.build();
        final SourceSectionFilter llvmInputFilter = SourceSectionFilter.newBuilder().tagIs(LLVMTags.LLVMExpression.class).includeInternal(true).build();

        final HashSet<Class<? extends Tag>> llvmTags = new HashSet<>(Arrays.asList(LLVMTags.ALL_TAGS));
        EventBinding<ExecutionEventNodeFactory> eventBinding = instrumenter.attachExecutionEventFactory(llvmNodeFilter, llvmInputFilter, context -> {
            final String tags = instrumenter.queryTags(context.getInstrumentedNode()).stream().map(tag -> (Class<? extends Tag>) tag).filter(llvmTags::contains).map(
                            Tag::getIdentifier).collect(Collectors.joining(", "));
            return new TraceNode(tags, formatNodeProperties(context.getNodeObject()), traceContext);
        });

        traceContext.setBinding(eventBinding);
        setTargetStream(env, env.getOptions().get(SulongEngineOption.TRACE_IR), traceContext);
    }

    public void dispose() {
        final PrintWriter targetWriter = traceContext.getTargetWriter();
        targetWriter.flush();
        if (traceContext.isCloseTarget()) {
            targetWriter.close();
        }
    }

    @TruffleBoundary
    private static String formatNodeProperties(Object nodeObject) {
        if (nodeObject instanceof LLVMNodeObject) {
            return String.valueOf(nodeObject);
        } else {
            return "";
        }
    }

    private static final String FILE_TARGET_PREFIX = "file://";

    @TruffleBoundary
    private static void setTargetStream(TruffleLanguage.Env env, String target, TraceContext traceContext) {
        if (target == null) {
            throw new IllegalArgumentException("Target for trace unspecified!");
        }

        final OutputStream targetStream;
        final boolean closeTarget;
        switch (target.toLowerCase()) {
            case "true":
            case "out":
            case "stdout":
                targetStream = env.out();
                closeTarget = false;
                break;

            case "err":
            case "stderr":
                targetStream = env.err();
                closeTarget = false;
                break;

            default:
                if (target.startsWith(FILE_TARGET_PREFIX)) {
                    final String fileName = target.substring(FILE_TARGET_PREFIX.length());
                    try {
                        final TruffleFile file = env.getTruffleFile(fileName);
                        targetStream = new BufferedOutputStream(file.newOutputStream(StandardOpenOption.CREATE, StandardOpenOption.APPEND));
                        closeTarget = true;
                    } catch (IOException e) {
                        throw new IllegalArgumentException("Invalid file: " + fileName, e);
                    }
                } else {
                    throw new IllegalArgumentException("Invalid target for tracing: " + target);
                }
        }

        traceContext.setTargetWriter(new PrintWriter(targetStream), closeTarget);
    }

    private static final class TraceContext {

        @CompilationFinal private EventBinding<ExecutionEventNodeFactory> eventBinding;
        @CompilationFinal private PrintWriter targetWriter;
        @CompilationFinal private boolean closeTarget;

        void setBinding(EventBinding<ExecutionEventNodeFactory> newBinding) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.eventBinding = newBinding;
        }

        EventBinding<ExecutionEventNodeFactory> getBinding() {
            return eventBinding;
        }

        void setTargetWriter(PrintWriter targetWriter, boolean closeTarget) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            this.targetWriter = targetWriter;
            this.closeTarget = closeTarget;
        }

        PrintWriter getTargetWriter() {
            return targetWriter;
        }

        boolean isCloseTarget() {
            return closeTarget;
        }
    }

    static class TraceNode extends ExecutionEventNode {

        private static int nextID = 0;

        private final String id;
        private final String tags;
        private final String extraData;

        private final PrintWriter targetWriter;
        private final TraceContext traceContext;

        @Child private PrintValueNode printValueNode;

        @TruffleBoundary
        TraceNode(String tags, String extraData, TraceContext traceContext) {
            this.tags = tags;
            this.extraData = extraData;
            this.targetWriter = traceContext.getTargetWriter();
            this.traceContext = traceContext;
            this.id = String.valueOf(nextID++);

            this.printValueNode = LLVMExecutionTracerFactory.PrintValueNodeGen.create();
        }

        @TruffleBoundary
        private void print(String str) {
            targetWriter.print(str);
            targetWriter.flush();
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            print("<node id=\"" + id + "\" tags=\"" + tags + "\" properties=\"" + extraData + "\">");
        }

        @Override
        protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
            final TraceNode inputEventNode = (TraceNode) inputContext.lookupExecutionEventNode(traceContext.getBinding());
            assert inputEventNode != null;

            print("<input index=\"" + inputIndex + "\" source=\"" + inputEventNode.id + "\">");
            print(printValueNode.executeWithTarget(inputValue));
            print("</input>");
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object result) {
            if (result != null) {
                print("<output>" + printValueNode.executeWithTarget(result) + "</output>");
            }

            print("</node>");
        }

        @Override
        protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
            print("<exception>");
            print(exception.getMessage());
            print("</exception>");

            print("</node>");
        }
    }

    @TypeSystemReference(LLVMTypes.class)
    abstract static class PrintValueNode extends Node {

        abstract String executeWithTarget(Object value);

        @Specialization
        protected String doSmallIVarBit(LLVMIVarBitSmall value) {
            return String.valueOf(value.getLongValue());
        }

        @Specialization
        @TruffleBoundary
        protected String doLargeIVarBit(LLVMIVarBitLarge value) {
            return value.asBigInteger().toString();
        }

        @Specialization
        protected String doLLVMFunctionDescriptor(LLVMFunctionDescriptor value) {
            return value.getName();
        }

        protected static PrintValueNode createRecursive() {
            return LLVMExecutionTracerFactory.PrintValueNodeGen.create();
        }

        @Specialization
        protected String doLLVMManagedPointer(LLVMManagedPointer value, @Cached("createRecursive()") PrintValueNode childFormatter) {
            final String target = childFormatter.executeWithTarget(value.getObject());

            final long offset = value.getOffset();
            if (offset == 0L) {
                return "Managed Pointer(target = \'" + target + "\')";
            } else {
                return "Managed Pointer(target = \'" + target + "\', offset = " + offset + ")";
            }
        }

        protected static boolean hasCheckedToString(Object value) {
            return !(value instanceof LLVMIVarBit) && !(value instanceof LLVMFunctionDescriptor) && !LLVMManagedPointer.isInstance(value);
        }

        @Specialization(guards = "hasCheckedToString(value)")
        protected String doGeneric(Object value) {
            return String.valueOf(value);
        }
    }
}
