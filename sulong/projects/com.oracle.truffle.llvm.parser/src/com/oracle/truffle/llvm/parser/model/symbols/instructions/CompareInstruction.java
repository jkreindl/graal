/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.model.symbols.instructions;

import com.oracle.truffle.llvm.parser.model.SymbolImpl;
import com.oracle.truffle.llvm.parser.model.SymbolTable;
import com.oracle.truffle.llvm.parser.model.visitors.SymbolVisitor;
import com.oracle.truffle.llvm.runtime.CompareOperator;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VectorType;

public final class CompareInstruction extends ValueInstruction {

    private final CompareOperator operator;
    private final int flags;

    private SymbolImpl lhs;
    private SymbolImpl rhs;

    private CompareInstruction(Type type, CompareOperator operator, int flags) {
        super(calculateResultType(type));
        this.operator = operator;
        this.flags = flags;
    }

    private static Type calculateResultType(Type type) {
        // The comparison performed always yields either an i1 or vector of i1 as result
        if (type instanceof VectorType) {
            return new VectorType(PrimitiveType.I1, ((VectorType) type).getNumberOfElements());
        }
        return PrimitiveType.I1;
    }

    @Override
    public void accept(SymbolVisitor visitor) {
        visitor.visit(this);
    }

    public SymbolImpl getLHS() {
        return lhs;
    }

    public CompareOperator getOperator() {
        return operator;
    }

    public SymbolImpl getRHS() {
        return rhs;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public void replace(SymbolImpl original, SymbolImpl replacement) {
        if (lhs == original) {
            lhs = replacement;
        }
        if (rhs == original) {
            rhs = replacement;
        }
    }

    public static CompareInstruction fromSymbols(SymbolTable symbols, Type type, int opcode, int lhs, int rhs, int flags) {
        final CompareInstruction cmpInst = new CompareInstruction(type, decodeCompareOperator(opcode), flags);
        cmpInst.lhs = symbols.getForwardReferenced(lhs, cmpInst);
        cmpInst.rhs = symbols.getForwardReferenced(rhs, cmpInst);
        return cmpInst;
    }

    public static CompareOperator decodeCompareOperator(int opcode) {
        switch (opcode) {
            case 0:
                return CompareOperator.FP_FALSE;
            case 1:
                return CompareOperator.FP_ORDERED_EQUAL;
            case 2:
                return CompareOperator.FP_ORDERED_GREATER_THAN;
            case 3:
                return CompareOperator.FP_ORDERED_GREATER_OR_EQUAL;
            case 4:
                return CompareOperator.FP_ORDERED_LESS_THAN;
            case 5:
                return CompareOperator.FP_ORDERED_LESS_OR_EQUAL;
            case 6:
                return CompareOperator.FP_ORDERED_NOT_EQUAL;
            case 7:
                return CompareOperator.FP_ORDERED;
            case 8:
                return CompareOperator.FP_UNORDERED;
            case 9:
                return CompareOperator.FP_UNORDERED_EQUAL;
            case 10:
                return CompareOperator.FP_UNORDERED_GREATER_THAN;
            case 11:
                return CompareOperator.FP_UNORDERED_GREATER_OR_EQUAL;
            case 12:
                return CompareOperator.FP_UNORDERED_LESS_THAN;
            case 13:
                return CompareOperator.FP_UNORDERED_LESS_OR_EQUAL;
            case 14:
                return CompareOperator.FP_UNORDERED_NOT_EQUAL;
            case 15:
                return CompareOperator.FP_TRUE;
            case 32:
                return CompareOperator.INT_EQUAL;
            case 33:
                return CompareOperator.INT_NOT_EQUAL;
            case 34:
                return CompareOperator.INT_UNSIGNED_GREATER_THAN;
            case 35:
                return CompareOperator.INT_UNSIGNED_GREATER_OR_EQUAL;
            case 36:
                return CompareOperator.INT_UNSIGNED_LESS_THAN;
            case 37:
                return CompareOperator.INT_UNSIGNED_LESS_OR_EQUAL;
            case 38:
                return CompareOperator.INT_SIGNED_GREATER_THAN;
            case 39:
                return CompareOperator.INT_SIGNED_GREATER_OR_EQUAL;
            case 40:
                return CompareOperator.INT_SIGNED_LESS_THAN;
            case 41:
                return CompareOperator.INT_SIGNED_LESS_OR_EQUAL;
            default:
                throw new LLVMParserException("Unknown comparison kind: " + opcode);
        }
    }
}
