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
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ValueInstruction;
import com.oracle.truffle.llvm.parser.model.visitors.ConstantVisitor;
import com.oracle.truffle.llvm.parser.model.visitors.ValueInstructionVisitor;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.nodes.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.nodes.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

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
        tags = LLVMTags.SSARead.SINGLE_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(valueInstruction).build();
    }

    @Override
    public void visit(FunctionParameter param) {
        tags = LLVMTags.SSARead.SINGLE_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(param).build();
    }

    @Override
    public void visit(FunctionDeclaration toResolve) {
        // TODO (jkreindl) add info as nodeobject
        tags = LLVMTags.GlobalRead.SINGLE_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(toResolve).build();
    }

    @Override
    public void visit(FunctionDefinition toResolve) {
        // TODO (jkreindl) add info as nodeobject
        tags = LLVMTags.GlobalRead.CONSTANT_GLOBAL_READ_TAGS;
        nodeObject = createTypedNodeObject(toResolve).build();
    }

    @Override
    public void visit(GlobalAlias alias) {
        // TODO (jkreindl) add info as nodeobject
        tags = LLVMTags.GlobalRead.SINGLE_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(alias).build();
    }

    @Override
    public void visit(GlobalVariable global) {
        // TODO (jkreindl) add info as nodeobject
        if (global.isReadOnly()) {
            tags = LLVMTags.GlobalRead.CONSTANT_GLOBAL_READ_TAGS;
        } else {
            tags = LLVMTags.GlobalRead.SINGLE_EXPRESSION_TAG;
        }
        nodeObject = createTypedNodeObject(global).build();
    }

    @Override
    public void visitConstant(Symbol constant) {
        // TODO (jkreindl) add tag for operation
        tags = LLVMTags.Constant.SINGLE_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(BinaryOperationConstant constant) {
        switch (constant.getOperator()) {
            case FP_ADD:
            case INT_ADD:
                tags = LLVMTags.Add.CONSTANT_EXPRESSION_TAG;
                break;
            case FP_SUBTRACT:
            case INT_SUBTRACT:
                tags = LLVMTags.Sub.CONSTANT_EXPRESSION_TAG;
                break;
            case FP_MULTIPLY:
            case INT_MULTIPLY:
                tags = LLVMTags.Mul.CONSTANT_EXPRESSION_TAG;
                break;
            case FP_DIVIDE:
            case INT_UNSIGNED_DIVIDE:
            case INT_SIGNED_DIVIDE:
                tags = LLVMTags.Div.CONSTANT_EXPRESSION_TAG;
                break;
            case FP_REMAINDER:
            case INT_UNSIGNED_REMAINDER:
            case INT_SIGNED_REMAINDER:
                tags = LLVMTags.Rem.CONSTANT_EXPRESSION_TAG;
                break;
            case INT_SHIFT_LEFT:
                tags = LLVMTags.ShiftLeft.CONSTANT_EXPRESSION_TAG;
                break;
            case INT_LOGICAL_SHIFT_RIGHT:
            case INT_ARITHMETIC_SHIFT_RIGHT:
                tags = LLVMTags.ShiftRight.CONSTANT_EXPRESSION_TAG;
                break;
            case INT_AND:
                tags = LLVMTags.And.CONSTANT_EXPRESSION_TAG;
                break;
            case INT_OR:
                tags = LLVMTags.Or.CONSTANT_EXPRESSION_TAG;
                break;
            case INT_XOR:
                tags = LLVMTags.XOr.CONSTANT_EXPRESSION_TAG;
                break;
            default:
                throw new LLVMParserException("unknown constant operation: " + constant.getOperator());
        }
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(CastConstant constant) {
        tags = LLVMTags.Cast.CONSTANT_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(CompareConstant constant) {
        if (Type.isFloatingpointType(constant.getLHS().getType())) {
            tags = LLVMTags.FCMP.CONSTANT_EXPRESSION_TAG;
        } else {
            tags = LLVMTags.ICMP.CONSTANT_EXPRESSION_TAG;
        }
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(GetElementPointerConstant constant) {
        tags = LLVMTags.GetElementPtr.CONSTANT_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(constant).build();
    }

    @Override
    public void visit(SelectConstant constant) {
        tags = LLVMTags.Select.CONSTANT_EXPRESSION_TAG;
        nodeObject = createTypedNodeObject(constant).build();
    }
}
