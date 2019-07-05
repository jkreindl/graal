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
package com.oracle.truffle.llvm.taint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleContext;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ContextsListener;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMTypes;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;

@TruffleInstrument.Registration(id = "taint", name = "taint", services = TaintInstrument.class)
public class TaintInstrument extends TruffleInstrument implements ContextsListener {

    @Option(name = "", help = "Enable a Simple Taint Tracking Instrument", category = OptionCategory.USER) //
    static final OptionKey<Boolean> Taint = new OptionKey<>(false);

    private static final HashSet<Object> TAINTED_OBJECTS = new HashSet<>();
    private static final HashMap<LLVMNativePointer, Long> POINTER_LENGTHS = new HashMap<>();

    @TruffleBoundary
    private static void taintValue(Object value) {
        TAINTED_OBJECTS.add(value);
    }

    @TruffleBoundary
    private static boolean isTainted(Object value) {
        return TAINTED_OBJECTS.contains(value);
    }

    @TruffleBoundary
    private static void untaintValue(Object value) {
        TAINTED_OBJECTS.remove(value);
    }

    @TruffleBoundary
    private static void untaintPointer(LLVMNativePointer ptr) {
        TAINTED_OBJECTS.remove(ptr);
        Long len = POINTER_LENGTHS.remove(ptr);
        if (len != null) {
            for (long i = 1; i < len; i++) {
                TAINTED_OBJECTS.remove(ptr.increment(i));
            }
        }
    }

    private Env environment = null;

    @Override
    protected void onCreate(Env env) {
        final SourceSectionFilter inputFilter = SourceSectionFilter.newBuilder().build();

        final SourceSectionFilter llvmIntrinsicsFilter = SourceSectionFilter.newBuilder().tagIs(LLVMTags.Intrinsic.class).build();
        env.getInstrumenter().attachExecutionEventFactory(llvmIntrinsicsFilter, inputFilter, new TaintIntrinsicEventNodeFactory());

        env.registerService(this);
        this.environment = env;

        env.getInstrumenter().attachContextsListener(this, true);
    }

    @Override
    protected void onDispose(Env env) {
        this.environment = null;
    }

    @Override
    public void onContextCreated(TruffleContext context) {
    }

    @Override
    public void onLanguageContextCreated(TruffleContext context, LanguageInfo language) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onLanguageContextInitialized(TruffleContext context, LanguageInfo language) {
        Map<String, Object> polyglotBindings = (Map<String, Object>) environment.getExportedSymbols();
        polyglotBindings.put("taint", new TaintStateObject());
    }

    @Override
    public void onLanguageContextFinalized(TruffleContext context, LanguageInfo language) {

    }

    @Override
    public void onLanguageContextDisposed(TruffleContext context, LanguageInfo language) {

    }

    @Override
    public void onContextClosed(TruffleContext context) {

    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new TaintInstrumentOptionDescriptors();
    }

    private static final class TaintIntrinsicEventNodeFactory implements ExecutionEventNodeFactory {

        @Override
        public ExecutionEventNode create(EventContext context) {
            final Object nodeObject = context.getNodeObject();
            if (nodeObject == null) {
                throw new IllegalStateException("Unknown Intrinsic");
            }

            final InteropLibrary library = InteropLibrary.getFactory().getUncached();
            if (!library.hasMembers(nodeObject)) {
                throw new IllegalStateException("Unknown Intrinsic");
            }

            final Object intrinsicName;
            try {
                intrinsicName = library.readMember(nodeObject, LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_NAME);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                throw new IllegalStateException("Unknown Intrinsic", e);
            }

            if (intrinsicName == null) {
                throw new IllegalStateException("Unknown Intrinsic");
            }

            if ("@polyglot_as_string".equals(intrinsicName)) {
                return new PolyglotAsStringTaintNode();
            } else if ("@polyglot_from_string_n".equals(intrinsicName)) {
                return new PolyglotFromStringTaintNode();
            } else if ("@llvm.memmove.p0i8.p0i8.i64".equals(intrinsicName) || "@llvm.memcpy.p0i8.p0i8.i64".equals(intrinsicName)) {
                return new BuiltinMemmoveTaintNode();
            } else if ("@isValueTainted".equals(intrinsicName)) {
                return new IsValueTaintedNode(context);
            } else if ("@taintValue".equals(intrinsicName)) {
                return new ExecutionEventNode() {
                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
                        assert inputIndex == 0;
                        taintValue(inputValue);
                    }

                    @Override
                    protected void onReturnValue(VirtualFrame frame, Object result) {
                        super.onReturnValue(frame, result);
                    }
                };
            } else if ("@untaintValue".equals(intrinsicName) || "@free".equals(intrinsicName)) {
                return new ExecutionEventNode() {

                    @Child private ClearTaintNode clearTaintNode = TaintInstrumentFactory.ClearTaintNodeGen.create();

                    @Override
                    protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
                        assert inputIndex == 0;
                        clearTaintNode.executeWithTarget(inputValue);
                    }
                };
            } else if ("@malloc".equals(intrinsicName)) {
                return new MallocTaintNode();
            } else {
                return new ExecutionEventNode() {
                };
            }
        }
    }

    @TypeSystemReference(LLVMTypes.class)
    abstract static class SafePointerSize extends Node {

        public abstract void executeWithTarget(Object ptr, Object len);

        @Specialization
        @TruffleBoundary
        protected void doNativePointer(LLVMNativePointer ptr, long len) {
            POINTER_LENGTHS.put(ptr, len);
        }
    }

    private static final class MallocTaintNode extends ExecutionEventNode {

        @Child private SafePointerSize safePointerSize = TaintInstrumentFactory.SafePointerSizeNodeGen.create();

        private Object len = null;

        @Override
        protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
            assert inputIndex == 0;
            len = inputValue;
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object result) {
            // @free ensures that the memory is not tainted
            safePointerSize.executeWithTarget(result, len);
            len = null;
        }
    }

    @TypeSystemReference(LLVMTypes.class)
    abstract static class TaintBufferFromStringNode extends Node {

        public abstract void executeGeneric(Object str, Object cBuffer, Object strSize);

        @Specialization
        protected void doGeneric(Object str, LLVMNativePointer cBuffer, long bufferSize) {
            if (isTainted(str)) {
                for (long i = 0; i < bufferSize; i++) {
                    taintValue(cBuffer.increment(i));
                }
            } else {
                for (long i = 0; i < bufferSize; i++) {
                    untaintValue(cBuffer.increment(i));
                }
            }
        }
    }

    private static final class PolyglotAsStringTaintNode extends ExecutionEventNode {

        @Child TaintBufferFromStringNode taintBufferNode;

        // trying to save the input values on the frame causes the compiler ot crash during PE for
        // some reason
        private Object strObj = null;
        private Object cBufferObj = null;

        private PolyglotAsStringTaintNode() {
            taintBufferNode = TaintInstrumentFactory.TaintBufferFromStringNodeGen.create();
        }

        @Override
        protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
            if (inputIndex == 2) {
                strObj = inputValue;
            }

            if (inputIndex == 3) {
                cBufferObj = inputValue;
            }
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object bytesWritten) {
            taintBufferNode.executeGeneric(strObj, cBufferObj, bytesWritten);
            strObj = null;
            cBufferObj = null;
        }
    }

    @TypeSystemReference(LLVMTypes.class)
    abstract static class IsMemoryTaintedNode extends Node {

        public abstract boolean executeGeneric(Object ptr, Object len);

        @Specialization
        protected boolean doGeneric(LLVMNativePointer ptr, long len) {
            for (long i = 0; i < len; i++) {
                if (isTainted(ptr.increment(i))) {
                    return true;
                }
            }
            return false;
        }

    }

    private static final class PolyglotFromStringTaintNode extends ExecutionEventNode {

        @Child private IsMemoryTaintedNode isMemoryTainted = TaintInstrumentFactory.IsMemoryTaintedNodeGen.create();

        private Object fromPtr = null;
        private Object len = null;

        @Override
        protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
            if (inputIndex == 0) {
                fromPtr = inputValue;
            }

            if (inputIndex == 1) {
                len = inputValue;
            }
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object result) {
            if (isMemoryTainted.executeGeneric(fromPtr, len)) {
                taintValue(result);
            }
            fromPtr = null;
            len = null;
        }
    }

    private static final class IsValueTaintedNode extends ExecutionEventNode {

        private static final class UnwindInfo {
            int isTainted = 0;
        }

        private final ThreadDeath unwind;

        private final UnwindInfo unwindInfo;

        IsValueTaintedNode(EventContext context) {
            unwindInfo = new UnwindInfo();
            unwind = context.createUnwind(unwindInfo);
        }

        @Override
        protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
            unwindInfo.isTainted = isTainted(inputValue) ? 1 : 0;
            throw unwind;
        }

        @Override
        protected Object onUnwind(VirtualFrame frame, Object info) {
            if (info instanceof UnwindInfo) {
                return ((UnwindInfo) info).isTainted;
            }
            return super.onUnwind(frame, info);
        }
    }

    @TypeSystemReference(LLVMTypes.class)
    abstract static class TaintMemmoveNode extends Node {

        public abstract void executeGeneric(Object toPtr, Object fromPtr, Object ptrSize);

        @Specialization
        protected void doGeneric(LLVMNativePointer toPtr, LLVMNativePointer fromPtr, long ptrSize) {
            for (long i = 0; i < ptrSize; i++) {
                if (isTainted(fromPtr.increment(i))) {
                    taintValue(toPtr.increment(i));
                } else {
                    untaintValue(toPtr.increment(i));
                }
            }
        }
    }

    private static final class BuiltinMemmoveTaintNode extends ExecutionEventNode {

        @Child private TaintMemmoveNode taintMemmove = TaintInstrumentFactory.TaintMemmoveNodeGen.create();

        private Object toPtr = null;
        private Object fromPtr = null;
        private Object len = null;

        @Override
        protected void onInputValue(VirtualFrame frame, EventContext inputContext, int inputIndex, Object inputValue) {
            if (inputIndex == 0) {
                toPtr = inputValue;
            }

            if (inputIndex == 1) {
                fromPtr = inputValue;
            }

            if (inputIndex == 2) {
                len = inputValue;
            }
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object result) {
            taintMemmove.executeGeneric(toPtr, fromPtr, len);
            toPtr = null;
            fromPtr = null;
            len = 0;
        }
    }

    @TypeSystemReference(LLVMTypes.class)
    abstract static class ClearTaintNode extends Node {

        public abstract void executeWithTarget(Object value);

        @Specialization
        protected void doNativePointer(LLVMNativePointer ptr) {
            untaintPointer(ptr);
        }

        @SuppressWarnings("static-method")
        protected boolean isNativePointer(Object val) {
            return LLVMNativePointer.isInstance(val);
        }

        @Specialization(guards = "!isNativePointer(val)")
        protected void doObject(Object val) {
            untaintValue(val);
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static class TaintStateObject implements TruffleObject {

        static final String ADD_TAINT = "taintValue";
        static final String CHECK_TAINT = "isValueTainted";
        static final String REMOVE_TAINT = "untaintValue";

        @ExportMessage
        boolean isMemberInvocable(String method) {
            return ADD_TAINT.equals(method) || CHECK_TAINT.equals(method) || REMOVE_TAINT.equals(method);
        }

        @ExportMessage
        boolean hasMembers() {
            return true;
        }

        @TruffleBoundary
        @ExportMessage
        Object getMembers(@SuppressWarnings("unused") boolean includeInternal) throws UnsupportedMessageException {
            throw UnsupportedMessageException.create();
        }

        @ExportMessage
        @SuppressWarnings("unused")
        static class InvokeMember {

            @Specialization(guards = {"ADD_TAINT.equals(method)", "args.length == 1"})
            static void addTaint(TaintStateObject receiver, String method, Object[] args) {
                taintValue(args[0]);
            }

            @Specialization(guards = {"CHECK_TAINT.equals(method)", "args.length == 1"})
            static boolean checkTaint(TaintStateObject receiver, String method, Object[] args) {
                return isTainted(args[0]);
            }

            static ClearTaintNode createClearTaint() {
                return TaintInstrumentFactory.ClearTaintNodeGen.create();
            }

            @Specialization(guards = {"REMOVE_TAINT.equals(method)", "args.length == 1"})
            static void removeTaint(TaintStateObject receiver, String method, Object[] args, @Cached(value = "createClearTaint()", allowUncached = true) ClearTaintNode clearTaint) {
                clearTaint.executeWithTarget(args[0]);
            }
        }
    }
}
