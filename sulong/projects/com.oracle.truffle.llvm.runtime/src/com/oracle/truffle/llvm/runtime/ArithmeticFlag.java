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
package com.oracle.truffle.llvm.runtime;

public enum ArithmeticFlag {

    // flags for integer arithmetic and logical shifts
    INT_NO_UNSIGNED_WRAP("nuw", 1),
    INT_NO_SIGNED_WRAP("nsw", 2),

    // flags for floating point arithmetic
    FP_NO_NANS("nnan", 2),
    FP_NO_INFINITIES("ninf", 4),
    FP_NO_SIGNED_ZEROES("nsz", 8),
    FP_ALLOW_RECIPROCAL("arcp", 16),
    FP_FAST("fast", 31),

    // additional flag for integer div
    INT_EXACT("exact", 1);

    public static final int NO_FLAGS = 0;
    public static final ArithmeticFlag[] ALL_VALUES = values();

    private final String stringValue;

    private final int bitMask;

    ArithmeticFlag(String stringValue, int bitMask) {
        this.stringValue = stringValue;
        this.bitMask = bitMask;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    public int set(int otherFlags) {
        return otherFlags | bitMask;
    }

    public boolean test(int flags) {
        return (flags & bitMask) != 0;
    }
}
