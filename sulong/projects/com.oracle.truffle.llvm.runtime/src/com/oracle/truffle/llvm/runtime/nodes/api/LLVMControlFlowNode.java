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
package com.oracle.truffle.llvm.runtime.nodes.api;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;

public abstract class LLVMControlFlowNode extends LLVMNode implements InstrumentableNode {

    @CompilationFinal private SourceSection explicitSourceSection = null;
    @CompilationFinal(dimensions = 1) private Class<? extends Tag>[] tags = null;
    @CompilationFinal private Object nodeObject;

    private final LLVMSourceLocation source;

    public LLVMControlFlowNode(LLVMSourceLocation source) {
        this.source = source;
    }

    public abstract int getSuccessorCount();

    public abstract LLVMStatementNode getPhiNode(int successorIndex);

    public boolean needsBranchProfiling() {
        return getSuccessorCount() > 1;
    }

    @Override
    public LLVMSourceLocation getSourceLocation() {
        return source;
    }

    public void setExplicitSourceSection(SourceSection explicitSourceSection) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.explicitSourceSection = explicitSourceSection;
    }

    public void setTags(Class<? extends Tag>[] tags) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.tags = tags;
    }

    public void setNodeObject(Object nodeObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.nodeObject = nodeObject;
    }

    @Override
    public boolean isInstrumentable() {
        return source != null || explicitSourceSection != null;
    }

    @Override
    public SourceSection getSourceSection() {
        return explicitSourceSection != null ? explicitSourceSection : super.getSourceSection();
    }

    @Override
    public Object getNodeObject() {
        return nodeObject;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tags == null) {
            return false;
        }
        for (Class<? extends Tag> attachedTag : tags) {
            if (tag == attachedTag) {
                return true;
            }
        }
        return false;
    }
}
