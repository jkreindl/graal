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
package com.oracle.truffle.llvm.runtime;

public enum LLVMCompareOperator {

    FP_FALSE("false"),
    FP_ORDERED_EQUAL("oeq"),
    FP_ORDERED_GREATER_THAN("ogt"),
    FP_ORDERED_GREATER_OR_EQUAL("oge"),
    FP_ORDERED_LESS_THAN("olt"),
    FP_ORDERED_LESS_OR_EQUAL("ole"),
    FP_ORDERED_NOT_EQUAL("one"),
    FP_ORDERED("ord"),
    FP_UNORDERED("uno"),
    FP_UNORDERED_EQUAL("ueq"),
    FP_UNORDERED_GREATER_THAN("ugt"),
    FP_UNORDERED_GREATER_OR_EQUAL("uge"),
    FP_UNORDERED_LESS_THAN("ult"),
    FP_UNORDERED_LESS_OR_EQUAL("ule"),
    FP_UNORDERED_NOT_EQUAL("une"),
    FP_TRUE("true"),

    INT_EQUAL("eq"),
    INT_NOT_EQUAL("ne"),
    INT_UNSIGNED_GREATER_THAN("ugt"),
    INT_UNSIGNED_GREATER_OR_EQUAL("uge"),
    INT_UNSIGNED_LESS_THAN("ult"),
    INT_UNSIGNED_LESS_OR_EQUAL("ule"),
    INT_SIGNED_GREATER_THAN("sgt"),
    INT_SIGNED_GREATER_OR_EQUAL("sge"),
    INT_SIGNED_LESS_THAN("slt"),
    INT_SIGNED_LESS_OR_EQUAL("sle");

    private final String irString;

    LLVMCompareOperator(String irString) {
        this.irString = irString;
    }

    public String getIrString() {
        return irString;
    }
}
