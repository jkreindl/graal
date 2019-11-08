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
package com.oracle.truffle.llvm.parser.model.enums;

import com.oracle.truffle.llvm.runtime.arithmetic.LLVMArithmeticOperator;
import com.oracle.truffle.llvm.runtime.arithmetic.LLVMCastOperator;
import com.oracle.truffle.llvm.runtime.arithmetic.LLVMRMWOperator;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

public final class OperatorParser {

    private static final LLVMArithmeticOperator[] FLOATING_BINARY_OPERATORS = new LLVMArithmeticOperator[]{
                    LLVMArithmeticOperator.FADD,
                    LLVMArithmeticOperator.FSUB,
                    LLVMArithmeticOperator.FMUL,
                    null,
                    LLVMArithmeticOperator.FDIV,
                    null,
                    LLVMArithmeticOperator.FREM
    };

    private static final LLVMArithmeticOperator[] INTEGER_BINARY_OPERATORS = new LLVMArithmeticOperator[]{
                    LLVMArithmeticOperator.ADD,
                    LLVMArithmeticOperator.SUB,
                    LLVMArithmeticOperator.MUL,
                    LLVMArithmeticOperator.UDIV,
                    LLVMArithmeticOperator.SDIV,
                    LLVMArithmeticOperator.UREM,
                    LLVMArithmeticOperator.SREM,
                    LLVMArithmeticOperator.SHL,
                    LLVMArithmeticOperator.LSHR,
                    LLVMArithmeticOperator.ASHR,
                    LLVMArithmeticOperator.AND,
                    LLVMArithmeticOperator.OR,
                    LLVMArithmeticOperator.XOR
    };

    public static LLVMArithmeticOperator parseArithmeticOperator(int opcode, boolean isFloatingPoint) {
        LLVMArithmeticOperator parsedOperator = null;

        if (opcode >= 0) {
            LLVMArithmeticOperator[] validOperators = isFloatingPoint ? FLOATING_BINARY_OPERATORS : INTEGER_BINARY_OPERATORS;
            if (opcode < validOperators.length) {
                parsedOperator = validOperators[opcode];
            }
        }

        if (parsedOperator != null) {
            return parsedOperator;
        }

        throw new LLVMParserException("Not a valid opcode for " + (isFloatingPoint ? "floating point" : "integer") + " arithmetic: " + opcode);
    }

    private static final LLVMCastOperator[] CAST_OPERATORS = LLVMCastOperator.values();

    public static LLVMCastOperator parseCastOperator(int code) {
        if (code >= 0 && code < CAST_OPERATORS.length) {
            return CAST_OPERATORS[code];
        }
        return null;
    }

    private static final LLVMRMWOperator[] RMW_OPERATORS = LLVMRMWOperator.values();

    public static LLVMRMWOperator parseRMWOperator(int code) {
        if (code >= 0 && code < RMW_OPERATORS.length) {
            return RMW_OPERATORS[code];
        }
        return null;
    }
}
