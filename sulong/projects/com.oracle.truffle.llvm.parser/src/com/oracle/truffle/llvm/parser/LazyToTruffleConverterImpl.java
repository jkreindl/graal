/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.LLVMLivenessAnalysis.LLVMLivenessAnalysisResult;
import com.oracle.truffle.llvm.parser.LLVMPhiManager.Phi;
import com.oracle.truffle.llvm.parser.metadata.debuginfo.DebugInfoFunctionProcessor;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute.Kind;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute.KnownAttribute;
import com.oracle.truffle.llvm.parser.model.blocks.InstructionBlock;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.functions.LazyFunctionParser;
import com.oracle.truffle.llvm.parser.nodes.LLVMBitcodeInstructionVisitor;
import com.oracle.truffle.llvm.parser.nodes.LLVMRuntimeDebugInformation;
import com.oracle.truffle.llvm.parser.nodes.LLVMSymbolReadResolver;
import com.oracle.truffle.llvm.runtime.GetStackSpaceFactory;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor.LazyToTruffleConverter;
import com.oracle.truffle.llvm.runtime.NodeFactory;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourceFunctionType;
import com.oracle.truffle.llvm.runtime.except.LLVMUserException;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.UniquesRegion;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNodeSourceDescriptor;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;

public class LazyToTruffleConverterImpl implements LazyToTruffleConverter {
    private final LLVMParserRuntime runtime;
    private final FunctionDefinition method;
    private final Source source;
    private final LazyFunctionParser parser;
    private final DebugInfoFunctionProcessor diProcessor;

    private RootCallTarget resolved;

    LazyToTruffleConverterImpl(LLVMParserRuntime runtime, FunctionDefinition method, Source source, LazyFunctionParser parser,
                    DebugInfoFunctionProcessor diProcessor) {
        this.runtime = runtime;
        this.method = method;
        this.source = source;
        this.parser = parser;
        this.diProcessor = diProcessor;
        this.resolved = null;
    }

    @Override
    public RootCallTarget convert() {
        CompilerAsserts.neverPartOfCompilation();

        synchronized (this) {
            if (resolved == null) {
                resolved = generateCallTarget();
            }
            return resolved;
        }
    }

    private RootCallTarget generateCallTarget() {
        // parse the function block
        parser.parse(diProcessor, source, runtime);

        // prepare the phis
        final Map<InstructionBlock, List<Phi>> phis = LLVMPhiManager.getPhis(method);

        // setup the frameDescriptor
        final FrameDescriptor frame = StackManager.createFrame(method);

        // setup the uniquesRegion
        UniquesRegion uniquesRegion = new UniquesRegion();
        GetStackSpaceFactory getStackSpaceFactory = GetStackSpaceFactory.createGetUniqueStackSpaceFactory(uniquesRegion);

        LLVMLivenessAnalysisResult liveness = LLVMLivenessAnalysis.computeLiveness(frame, runtime.getContext(), phis, method);
        LLVMSymbolReadResolver symbols = LLVMSymbolReadResolver.create(runtime, frame, getStackSpaceFactory);
        List<FrameSlot> notNullable = new ArrayList<>();

        LLVMRuntimeDebugInformation dbgInfoHandler = new LLVMRuntimeDebugInformation(frame, runtime.getContext(), notNullable, symbols);
        dbgInfoHandler.registerStaticDebugSymbols(method);

        LLVMBitcodeFunctionVisitor visitor = new LLVMBitcodeFunctionVisitor(runtime.getContext(), runtime.getLibrary(), frame, uniquesRegion, phis, method.getParameters().size(), symbols, method,
                        liveness, notNullable, dbgInfoHandler);
        method.accept(visitor);
        FrameSlot[][] nullableBeforeBlock = getNullableFrameSlots(liveness.getFrameSlots(), liveness.getNullableBeforeBlock(), notNullable);
        FrameSlot[][] nullableAfterBlock = getNullableFrameSlots(liveness.getFrameSlots(), liveness.getNullableAfterBlock(), notNullable);
        LLVMSourceLocation location = method.getLexicalScope();

        List<LLVMStatementNode> copyArgumentsToFrame = copyArgumentsToFrame(frame);
        LLVMStatementNode[] copyArgumentsToFrameArray = copyArgumentsToFrame.toArray(LLVMStatementNode.NO_STATEMENTS);
        LLVMExpressionNode body = runtime.getContext().getLanguage().getNodeFactory().createFunctionBlockNode(frame.findFrameSlot(LLVMUserException.FRAME_SLOT_ID), visitor.getBlocks(),
                        uniquesRegion.build(),
                        nullableBeforeBlock, nullableAfterBlock, copyArgumentsToFrameArray);

        final LLVMNodeSourceDescriptor sourceDescriptor = body.getOrCreateSourceDescriptor();
        sourceDescriptor.setTags(LLVMTags.Function.FUNCTION_TAGS);
        sourceDescriptor.setNodeObject(
                        LLVMNodeObject.newBuilder().option(LLVMTags.Function.EXTRA_DATA_LLVM_NAME, method.getName()).option(LLVMTags.Function.EXTRA_DATA_SOURCE_NAME, method.getSourceName()).build());
        sourceDescriptor.setSourceLocation(location);

        RootNode rootNode = runtime.getContext().getLanguage().getNodeFactory().createFunctionStartNode(body, frame, method.getName(), method.getSourceName(),
                        method.getParameters().size(), source, location);
        method.onAfterParse();

        return Truffle.getRuntime().createCallTarget(rootNode);
    }

    @Override
    public LLVMSourceFunctionType getSourceType() {
        convert();
        return method.getSourceFunction().getSourceType();
    }

    private static FrameSlot[][] getNullableFrameSlots(FrameSlot[] frameSlots, BitSet[] nullablePerBlock, List<FrameSlot> notNullable) {
        FrameSlot[][] result = new FrameSlot[nullablePerBlock.length][];

        for (int i = 0; i < nullablePerBlock.length; i++) {
            BitSet nullable = nullablePerBlock[i];
            int bitIndex = -1;

            ArrayList<FrameSlot> nullableSlots = new ArrayList<>();
            while ((bitIndex = nullable.nextSetBit(bitIndex + 1)) >= 0) {
                FrameSlot frameSlot = frameSlots[bitIndex];
                if (!notNullable.contains(frameSlot)) {
                    nullableSlots.add(frameSlot);
                }
            }
            if (nullableSlots.size() > 0) {
                result[i] = nullableSlots.toArray(LLVMBitcodeInstructionVisitor.NO_SLOTS);
            } else {
                assert result[i] == null;
            }
        }
        return result;
    }

    private List<LLVMStatementNode> copyArgumentsToFrame(FrameDescriptor frame) {
        final NodeFactory nodeFactory = runtime.getContext().getLanguage().getNodeFactory();

        List<FunctionParameter> parameters = method.getParameters();
        List<LLVMStatementNode> formalParamInits = new ArrayList<>();
        LLVMExpressionNode stackPointerNode = nodeFactory.createFunctionArgNode(0, PrimitiveType.I64);
        formalParamInits.add(nodeFactory.createFrameWrite(PointerType.VOID, stackPointerNode, frame.findFrameSlot(LLVMStack.FRAME_ID)));

        int argIndex = 1;
        if (method.getType().getReturnType() instanceof StructureType) {
            argIndex++;
        }

        for (int i = 0; i < parameters.size(); i++) {
            final FunctionParameter parameter = parameters.get(i);
            LLVMExpressionNode parameterNode = nodeFactory.createFunctionArgNode(argIndex++, parameter.getType());

            final LLVMNodeObject nodeObject = LLVMNodeObject.newBuilder().option(LLVMTags.ReadCallArg.EXTRA_DATA_SSA_SOURCE, LLVMIdentifier.toLocalIdentifier(parameter.getName())).option(
                            LLVMTags.ReadCallArg.EXTRA_DATA_ARG_INDEX, i).build();
            final LLVMNodeSourceDescriptor sourceDescriptor = parameterNode.getOrCreateSourceDescriptor();
            sourceDescriptor.setTags(LLVMTags.ReadCallArg.EXPRESSION_TAGS);
            sourceDescriptor.setNodeObject(nodeObject);

            FrameSlot slot = frame.findFrameSlot(parameter.getName());
            if (isStructByValue(parameter)) {
                final Type type = ((PointerType) parameter.getType()).getPointeeType();
                final LLVMExpressionNode readParameterValueNode = nodeFactory.createCopyStructByValue(type, GetStackSpaceFactory.createAllocaFactory(), parameterNode);
                final LLVMStatementNode writeParameterValueToFrameNode = nodeFactory.createFrameWrite(parameter.getType(), readParameterValueNode, slot);
                formalParamInits.add(writeParameterValueToFrameNode);
            } else {
                formalParamInits.add(nodeFactory.createFrameWrite(parameter.getType(), parameterNode, slot));
            }
        }

        return formalParamInits;
    }

    private static boolean isStructByValue(FunctionParameter parameter) {
        if (parameter.getType() instanceof PointerType && parameter.getParameterAttribute() != null) {
            for (Attribute a : parameter.getParameterAttribute().getAttributes()) {
                if (a instanceof KnownAttribute && ((KnownAttribute) a).getAttr() == Kind.BYVAL) {
                    return true;
                }
            }
        }
        return false;
    }
}
