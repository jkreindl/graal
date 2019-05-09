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
import com.oracle.truffle.llvm.parser.model.ValueSymbol;
import com.oracle.truffle.llvm.parser.model.enums.BinaryOperator;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

final class InstrumentationUtil {

    static LLVMNodeObject.Builder createTypedNodeObject(Symbol symbol) {
        return LLVMNodeObject.newBuilder().option(LLVMTags.EXTRA_DATA_VALUE_TYPE, symbol.getType());
    }

    static Class<? extends Tag>[] getBinaryOperationTags(BinaryOperator operator, boolean isConstant) {
        switch (operator) {
            case FP_ADD:
            case INT_ADD:
                return isConstant ? LLVMTags.Add.CONSTANT_EXPRESSION_TAGS : LLVMTags.Add.EXPRESSION_TAGS;
            case FP_SUBTRACT:
            case INT_SUBTRACT:
                return isConstant ? LLVMTags.Sub.CONSTANT_EXPRESSION_TAGS : LLVMTags.Sub.EXPRESSION_TAGS;
            case FP_MULTIPLY:
            case INT_MULTIPLY:
                return isConstant ? LLVMTags.Mul.CONSTANT_EXPRESSION_TAGS : LLVMTags.Mul.EXPRESSION_TAGS;
            case FP_DIVIDE:
            case INT_UNSIGNED_DIVIDE:
            case INT_SIGNED_DIVIDE:
                return isConstant ? LLVMTags.Div.CONSTANT_EXPRESSION_TAGS : LLVMTags.Div.EXPRESSION_TAGS;
            case FP_REMAINDER:
                return isConstant ? LLVMTags.FRem.CONSTANT_EXPRESSION_TAGS : LLVMTags.FRem.EXPRESSION_TAGS;
            case INT_UNSIGNED_REMAINDER:
                return isConstant ? LLVMTags.URem.CONSTANT_EXPRESSION_TAGS : LLVMTags.URem.EXPRESSION_TAGS;
            case INT_SIGNED_REMAINDER:
                return isConstant ? LLVMTags.SRem.CONSTANT_EXPRESSION_TAGS : LLVMTags.SRem.EXPRESSION_TAGS;
            case INT_SHIFT_LEFT:
                return isConstant ? LLVMTags.ShiftLeft.CONSTANT_EXPRESSION_TAGS : LLVMTags.ShiftLeft.EXPRESSION_TAGS;
            case INT_LOGICAL_SHIFT_RIGHT:
            case INT_ARITHMETIC_SHIFT_RIGHT:
                return isConstant ? LLVMTags.ShiftRight.CONSTANT_EXPRESSION_TAGS : LLVMTags.ShiftRight.EXPRESSION_TAGS;
            case INT_AND:
                return isConstant ? LLVMTags.And.CONSTANT_EXPRESSION_TAGS : LLVMTags.And.EXPRESSION_TAGS;
            case INT_OR:
                return isConstant ? LLVMTags.Or.CONSTANT_EXPRESSION_TAGS : LLVMTags.Or.EXPRESSION_TAGS;
            case INT_XOR:
                return isConstant ? LLVMTags.XOr.CONSTANT_EXPRESSION_TAGS : LLVMTags.XOr.EXPRESSION_TAGS;
            default:
                throw new LLVMParserException("unknown binary operation: " + operator);
        }
    }

    static LLVMNodeObject createSSAAccessDescriptor(ValueSymbol ssaValue, String ssaNameKey) {
        final String ssaName = LLVMIdentifier.toLocalIdentifier(ssaValue.getName());
        return createTypedNodeObject(ssaValue).option(ssaNameKey, ssaName).build();
    }

    static LLVMNodeObject createGlobalAccessDescriptor(ValueSymbol global) {
        final String llvmName = LLVMIdentifier.toGlobalIdentifier(global.getName());
        return createTypedNodeObject(global).option(LLVMTags.GlobalRead.EXTRA_DATA_GLOBAL_NAME_LLVM, llvmName).build();
    }

    private InstrumentationUtil() {
    }
}
