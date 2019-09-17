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
package com.oracle.truffle.llvm.runtime.arithmetic;

import static com.oracle.truffle.llvm.runtime.arithmetic.LLVMArithmeticFlag.EXACT;
import static com.oracle.truffle.llvm.runtime.arithmetic.LLVMArithmeticFlag.FAST_MATH_FLAGS;
import static com.oracle.truffle.llvm.runtime.arithmetic.LLVMArithmeticFlag.INTEGER_ARITHMETIC_FLAGS;
import static com.oracle.truffle.llvm.runtime.arithmetic.LLVMArithmeticFlag.NO_FLAGS;

public enum LLVMArithmeticOperator {

    ADD(INTEGER_ARITHMETIC_FLAGS),
    FADD(FAST_MATH_FLAGS),
    SUB(INTEGER_ARITHMETIC_FLAGS),
    FSUB(FAST_MATH_FLAGS),
    MUL(INTEGER_ARITHMETIC_FLAGS),
    FMUL(FAST_MATH_FLAGS),
    SDIV(EXACT),
    UDIV(EXACT),
    FDIV(FAST_MATH_FLAGS),
    SREM(),
    UREM(),
    FREM(FAST_MATH_FLAGS),
    AND(),
    OR(),
    XOR(),
    SHL(INTEGER_ARITHMETIC_FLAGS),
    LSHR(EXACT),
    ASHR(EXACT);

    private final LLVMArithmeticFlag[] possibleFlags;

    LLVMArithmeticOperator(LLVMArithmeticFlag... possibleFlags) {
        this.possibleFlags = possibleFlags != null ? possibleFlags : NO_FLAGS;
    }

    public LLVMArithmeticFlag[] getPossibleFlags() {
        return possibleFlags;
    }
}
