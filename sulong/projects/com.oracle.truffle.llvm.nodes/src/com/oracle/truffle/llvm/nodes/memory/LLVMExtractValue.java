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
package com.oracle.truffle.llvm.nodes.memory;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.nodes.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;
import com.oracle.truffle.llvm.runtime.vector.LLVMDoubleVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMFloatVector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI16Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI1Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI32Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI64Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMI8Vector;
import com.oracle.truffle.llvm.runtime.vector.LLVMPointerVector;

@NodeChild(value = "loadedValue", type = LLVMExpressionNode.class)
public abstract class LLVMExtractValue extends LLVMExpressionNode {

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == LLVMTags.ExtractValue.class || super.hasTag(tag);
    }

    @Specialization
    protected boolean doI1(boolean loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected byte doI8(byte loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected short doI16(short loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected int doI32(int loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected long doI64(long loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMPointer doLLVMPointer(LLVMPointer loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected float doFloat(float loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected double doDouble(double loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVM80BitFloat doLLVM80BitFloat(LLVM80BitFloat loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMI1Vector doLLVMI1Vector(LLVMI1Vector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMI8Vector doLLVMI8Vector(LLVMI8Vector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMI16Vector doLLVMI16Vector(LLVMI16Vector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMI32Vector doLLVMI32Vector(LLVMI32Vector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMI64Vector doLLVMI64Vector(LLVMI64Vector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMFloatVector doLLVMFloatVector(LLVMFloatVector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMDoubleVector doLLVMDoubleVector(LLVMDoubleVector loadedValue) {
        return loadedValue;
    }

    @Specialization
    protected LLVMPointerVector doLLVMPointerVector(LLVMPointerVector loadedValue) {
        return loadedValue;
    }
}
