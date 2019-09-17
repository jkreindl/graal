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
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

public final class BinaryArithmeticParser {

    public static LLVMArithmeticOperator parse(int opcode, boolean isFloatingPoint) {
        if (isFloatingPoint) {
            switch (opcode) {
                case 0:
                    return LLVMArithmeticOperator.FADD;
                case 1:
                    return LLVMArithmeticOperator.FSUB;
                case 2:
                    return LLVMArithmeticOperator.FMUL;
                case 4:
                    return LLVMArithmeticOperator.FDIV;
                case 6:
                    return LLVMArithmeticOperator.FREM;
                default:
                    throw new LLVMParserException("Not a valid opcode for binary floating point arithmetic: " + opcode);
            }
        } else {
            switch (opcode) {
                case 0:
                    return LLVMArithmeticOperator.ADD;
                case 1:
                    return LLVMArithmeticOperator.SUB;
                case 2:
                    return LLVMArithmeticOperator.MUL;
                case 3:
                    return LLVMArithmeticOperator.UDIV;
                case 4:
                    return LLVMArithmeticOperator.SDIV;
                case 5:
                    return LLVMArithmeticOperator.UREM;
                case 6:
                    return LLVMArithmeticOperator.SREM;
                case 7:
                    return LLVMArithmeticOperator.SHL;
                case 8:
                    return LLVMArithmeticOperator.LSHR;
                case 9:
                    return LLVMArithmeticOperator.ASHR;
                case 10:
                    return LLVMArithmeticOperator.AND;
                case 11:
                    return LLVMArithmeticOperator.OR;
                case 12:
                    return LLVMArithmeticOperator.XOR;
                default:
                    throw new LLVMParserException("Not a valid opcode for binary integer arithmetic: " + opcode);
            }
        }
    }

    private BinaryArithmeticParser() {
    }
}
