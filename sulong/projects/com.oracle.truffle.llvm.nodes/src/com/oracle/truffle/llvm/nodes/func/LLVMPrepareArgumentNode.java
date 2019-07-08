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
package com.oracle.truffle.llvm.nodes.func;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

@GenerateWrapper
abstract class LLVMPrepareArgumentNode extends LLVMNode implements InstrumentableNode {

    private static final SourceSection SOURCE_SECTION;

    static {
        final Source source = Source.newBuilder("llvm", "LLVM IR function argument preparation", "<llvm function argument>").mimeType("text/plain").build();
        SOURCE_SECTION = source.createUnavailableSection();
    }

    protected abstract Object executeWithTarget(VirtualFrame frame, Object value);

    private final int argIndex;

    protected LLVMPrepareArgumentNode(int argIndex) {
        this.argIndex = argIndex;
    }

    protected LLVMPrepareArgumentNode(LLVMPrepareArgumentNode delegate) {
        this.argIndex = delegate.argIndex;
    }

    @Specialization
    protected LLVMPointer doPointer(LLVMPointer address) {
        return address.copy();
    }

    @Fallback
    protected Object doOther(Object value) {
        return value;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return LLVMTags.isTagProvided(LLVMTags.StoreValueAsCallArgument.EXPRESSION_TAGS, tag);
    }

    @Override
    public Object getNodeObject() {
        return LLVMNodeObject.newBuilder().option(LLVMTags.StoreValueAsCallArgument.EXTRA_DATA_ARG_INDEX, argIndex).build();
    }

    @Override
    public SourceSection getSourceSection() {
        return SOURCE_SECTION;
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public WrapperNode createWrapper(ProbeNode probe) {
        return new LLVMPrepareArgumentNodeWrapper(this, this, probe);
    }
}
