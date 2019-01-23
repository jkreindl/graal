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
package com.oracle.truffle.llvm.nodes.cast;

import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.runtime.CastOperator;
import com.oracle.truffle.llvm.runtime.nodes.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.nodes.LLVMNodeObjects;
import com.oracle.truffle.llvm.runtime.nodes.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public abstract class LLVMCastNode extends LLVMExpressionNode {

    private final CastOperator conversionKind;

    protected LLVMCastNode(CastOperator conversionKind) {
        assert conversionKind != null : "Cast must have a valid conversionKind";
        this.conversionKind = conversionKind;
    }

    public CastOperator getConversionKind() {
        return conversionKind;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == LLVMTags.Cast.class || super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return new LLVMNodeObject(new String[]{LLVMNodeObjects.KEY_CONVERSION_KIND}, new Object[]{conversionKind.getIrString()});
    }

    @Override
    public String toString() {
        return conversionKind.getIrString();
    }
}
