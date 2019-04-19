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
package com.oracle.truffle.llvm.nodes.intrinsics.llvm;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMReadStringNode;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMReadStringNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.nodes.LLVMNodeObjects;
import com.oracle.truffle.llvm.runtime.nodes.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public abstract class LLVMIntrinsic extends LLVMExpressionNode {

    @CompilationFinal private String intrinsicName = null;

    public void setIntrinsicName(String name) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        intrinsicName = name;
    }

    public LLVMReadStringNode createReadString() {
        return LLVMReadStringNodeGen.create();
    }

    @Override
    public boolean isInstrumentable() {
        return intrinsicName != null;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return (tag == LLVMTags.Intrinsic.class && intrinsicName != null) || super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return new LLVMNodeObject(new String[]{LLVMNodeObjects.KEY_INTRINSIC_NAME}, new Object[]{intrinsicName});
    }

    public static class LLVMIntrinsicWrapper extends LLVMIntrinsic {

        @Child private LLVMExpressionNode body;

        public LLVMIntrinsicWrapper(LLVMExpressionNode body) {
            this.body = body;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return body.executeGeneric(frame);
        }
    }
}
