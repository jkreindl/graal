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
package com.oracle.truffle.llvm.runtime.nodes.vector;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
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

@NodeChild(type = LLVMExpressionNode.class, value = "vector")
@NodeChild(type = LLVMExpressionNode.class, value = "index")
public abstract class LLVMExtractElementNode extends LLVMExpressionNode {

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return super.hasTag(tag, LLVMTags.ExtractElement.EXPRESSION_TAGS);
    }

    public abstract static class LLVMI1ExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected boolean doI1(LLVMI1Vector vector, int index) {
            return vector.getValue(index);
        }
    }

    public abstract static class LLVMI8ExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected byte doI8(LLVMI8Vector vector, int index) {
            return vector.getValue(index);
        }
    }

    public abstract static class LLVMI16ExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected short doI16(LLVMI16Vector vector, int index) {
            return vector.getValue(index);
        }
    }

    public abstract static class LLVMI32ExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected int doI32(LLVMI32Vector vector, int index) {
            return vector.getValue(index);
        }
    }

    public abstract static class LLVMI64ExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected long doI64(LLVMI64Vector vector, int index) {
            return vector.getValue(index);
        }

        @Specialization
        protected LLVMPointer doPointer(LLVMPointerVector vector, int index) {
            return vector.getValue(index);
        }
    }

    public abstract static class LLVMFloatExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected float doFloat(LLVMFloatVector vector, int index) {
            return vector.getValue(index);
        }
    }

    public abstract static class LLVMDoubleExtractElementNode extends LLVMExtractElementNode {

        @Specialization
        protected double doDouble(LLVMDoubleVector vector, int index) {
            return vector.getValue(index);
        }
    }
}
