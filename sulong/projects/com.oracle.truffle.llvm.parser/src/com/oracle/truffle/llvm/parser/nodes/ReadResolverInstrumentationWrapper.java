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
package com.oracle.truffle.llvm.parser.nodes;

import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.parser.LLVMParserRuntime;
import com.oracle.truffle.llvm.parser.model.SymbolImpl;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.functions.FunctionParameter;
import com.oracle.truffle.llvm.parser.model.symbols.constants.BinaryOperationConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CastConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CompareConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.GetElementPointerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.SelectConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ValueInstruction;
import com.oracle.truffle.llvm.parser.model.visitors.ConstantVisitor;
import com.oracle.truffle.llvm.parser.model.visitors.ValueInstructionVisitor;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

import static com.oracle.truffle.llvm.parser.nodes.InstrumentationUtil.createGlobalAccessDescriptor;
import static com.oracle.truffle.llvm.parser.nodes.InstrumentationUtil.createSSAAccessDescriptor;
import static com.oracle.truffle.llvm.parser.nodes.InstrumentationUtil.createTypedNodeObject;

public class ReadResolverInstrumentationWrapper extends LLVMSymbolReadResolver implements ValueInstructionVisitor, ConstantVisitor {

    private final LLVMSymbolReadResolver impl;

    private Class<? extends Tag>[] tags;
    private LLVMNodeObject nodeObject;

    ReadResolverInstrumentationWrapper(LLVMParserRuntime runtime, LLVMSymbolReadResolver impl) {
        super(runtime);
        assert impl != null;
        this.impl = impl;

        this.tags = null;
        this.nodeObject = null;
    }

    @Override
    public LLVMExpressionNode resolve(SymbolImpl symbol) {
        final LLVMExpressionNode resolvedNode = impl.resolve(symbol);

        // determine instrumentation tags for the symbol
        tags = null;
        nodeObject = null;
        symbol.accept(this);
        if (tags == null) {
            throw new LLVMParserException("Failed to instrument expression for symbol: " + symbol);
        }

        final LLVMExpressionNode instrumentableNode = nodeFactory.createInstrumentableExpression(resolvedNode, tags, nodeObject);
        tags = null;
        nodeObject = null;
        return instrumentableNode;
    }

    @Override
    public void visitValueInstruction(ValueInstruction valueInstruction) {
        // a value instruction writes its result to an SSA slot
        tags = LLVMTags.SSARead.EXPRESSION_TAGS;
        nodeObject = createSSAAccessDescriptor(valueInstruction, LLVMTags.SSARead.EXTRA_DATA_SSA_SOURCE);
    }

    @Override
    public void visit(FunctionParameter param) {
        tags = LLVMTags.SSARead.EXPRESSION_TAGS;
        nodeObject = createSSAAccessDescriptor(param, LLVMTags.SSARead.EXTRA_DATA_SSA_SOURCE);
    }

    @Override
    public void visit(FunctionDeclaration toResolve) {
        tags = LLVMTags.GlobalRead.EXPRESSION_TAGS;
        nodeObject = createGlobalAccessDescriptor(toResolve);
    }

    @Override
    public void visit(FunctionDefinition toResolve) {
        tags = LLVMTags.GlobalRead.CONSTANT_EXPRESSION_TAGS;
        nodeObject = createGlobalAccessDescriptor(toResolve);
    }

    @Override
    public void visit(GlobalAlias alias) {
        tags = LLVMTags.GlobalRead.EXPRESSION_TAGS;
        nodeObject = createGlobalAccessDescriptor(alias);
    }

    @Override
    public void visit(GlobalVariable global) {
        if (global.isReadOnly()) {
            tags = LLVMTags.GlobalRead.CONSTANT_EXPRESSION_TAGS;
        } else {
            tags = LLVMTags.GlobalRead.EXPRESSION_TAGS;
        }
        nodeObject = createGlobalAccessDescriptor(global);
    }

    @Override
    public void visitConstant(Symbol constant) {
        tags = LLVMTags.Literal.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(BinaryOperationConstant constant) {
        tags = InstrumentationUtil.getBinaryOperationTags(constant.getOperator());
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(CastConstant constant) {
        tags = LLVMTags.Cast.CONSTANT_EXPRESSION_TAGS;
        final Type srcType = constant.getValue().getType();
        final String castKind = constant.getOperator().getIrString();
        nodeObject = createTypedNodeObject(constant).option(LLVMTags.Cast.EXTRA_DATA_SOURCE_TYPE, srcType).option(LLVMTags.Cast.EXTRA_DATA_KIND, castKind).build();
    }

    @Override
    public void visit(CompareConstant constant) {
        final LLVMNodeObject.Builder noBuilder = createTypedNodeObject(constant);
        final String cmpKind = constant.getOperator().name();

        if (Type.isFloatingpointType(constant.getLHS().getType())) {
            // TODO fcmp should have fast-math flags
            tags = LLVMTags.FCMP.CONSTANT_EXPRESSION_TAGS;
            noBuilder.option(LLVMTags.FCMP.EXTRA_DATA_KIND, cmpKind);
        } else {
            tags = LLVMTags.ICMP.CONSTANT_EXPRESSION_TAGS;
            noBuilder.option(LLVMTags.ICMP.EXTRA_DATA_KIND, cmpKind);
        }

        nodeObject = noBuilder.build();
    }

    @Override
    public void visit(GetElementPointerConstant constant) {
        tags = LLVMTags.GetElementPtr.CONSTANT_EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(constant).option(LLVMTags.GetElementPtr.EXTRA_DATA_SOURCE_TYPE, constant.getBasePointer().getType()).option(LLVMTags.GetElementPtr.EXTRA_DATA_IS_INBOUND,
                        constant.isInbounds()).build();
    }

    @Override
    public void visit(SelectConstant constant) {
        tags = LLVMTags.Select.CONSTANT_EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    LLVMExpressionNode createConstantElementPointerIndex(long actualIndex, Type indexType, LLVMExpressionNode currentAddress, long indexedAddressOffset, Type newType, boolean isLastIndex) {
        // for regular execution, Sulong usually does not create a node to increment a pointer when
        // the index is 0. when the index as given in the bitcode is a non-zero integer constant,
        // then Sulong computes the total offset, including the index, statically and uses a
        // constant "1" as multiplier for the run-time node. However, for instrumentation, the right
        // index needs to be reported.
        final LLVMExpressionNode indexForInstrumentation = resolve(new IntegerConstant(indexType, actualIndex));
        return nodeFactory.createInstrumentableConstantPointerIncrement(currentAddress, indexForInstrumentation, indexedAddressOffset);
    }
}
