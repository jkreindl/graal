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
package com.oracle.truffle.llvm.runtime.nodes.api;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTypeInteropWrapper;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import org.graalvm.collections.EconomicMap;

public abstract class LLVMInstrumentableNode extends LLVMNode implements InstrumentableNode {

    @CompilationFinal private LLVMNodeSourceDescriptor sourceDescriptor = null;
    @CompilationFinal private Type irNodeType = null;

    /**
     * Get a {@link LLVMNodeSourceDescriptor descriptor} for the debug and instrumentation
     * properties of this node.
     *
     * @return a source descriptor attached to this node
     */
    public final LLVMNodeSourceDescriptor getSourceDescriptor() {
        return sourceDescriptor;
    }

    /**
     * Get a {@link LLVMNodeSourceDescriptor descriptor} for the debug and instrumentation
     * properties of this node. If no such descriptor is currently attached to this node, one will
     * be created.
     *
     * @return a source descriptor attached to this node
     */
    public final LLVMNodeSourceDescriptor getOrCreateSourceDescriptor() {
        if (sourceDescriptor == null) {
            setSourceDescriptor(new LLVMNodeSourceDescriptor());
        }
        return sourceDescriptor;
    }

    public final void setSourceDescriptor(LLVMNodeSourceDescriptor sourceDescriptor) {
        // the source descriptor should only be set in the parser, and should only be modified
        // before this node is first executed
        CompilerAsserts.neverPartOfCompilation();
        this.sourceDescriptor = sourceDescriptor;
    }

    public final void enableIRTags(Type irNodeType) {
        CompilerAsserts.neverPartOfCompilation();
        this.irNodeType = irNodeType;
    }

    @Override
    public SourceSection getSourceSection() {
        if (sourceDescriptor != null) {
            return sourceDescriptor.getSourceSection();
        } else if (irNodeType != null) {
            return LLVMNodeSourceDescriptor.DEFAULT_SOURCE_SECTION;
        } else {
            return null;
        }
    }

    @Override
    public boolean isInstrumentable() {
        return getSourceSection() != null;
    }

    /**
     * Describes whether this node has source-level debug information attached and should be
     * considered a source-level statement for instrumentation.
     *
     * @return whether this node may provide the
     *         {@link com.oracle.truffle.api.instrumentation.StandardTags.StatementTag}
     */
    private boolean hasStatementTag() {
        return sourceDescriptor != null && sourceDescriptor.hasStatementTag();
    }

    /**
     * Get a {@link LLVMSourceLocation descriptor} for the source-level code location and scope
     * information of this node.
     *
     * @return the {@link LLVMSourceLocation} attached to this node
     */
    public LLVMSourceLocation getSourceLocation() {
        return sourceDescriptor != null ? sourceDescriptor.getSourceLocation() : null;
    }

    @SuppressWarnings("unchecked") //
    private static final Class<? extends Tag>[] NO_TAGS = new Class[0];

    /**
     * If this node {@link LLVMInstrumentableNode#hasStatementTag() is a statement for source-level
     * instrumentatipon}, this function considers the node to be tagged with
     * {@link com.oracle.truffle.api.instrumentation.StandardTags.StatementTag}.
     *
     * @param tag class of a tag {@link com.oracle.truffle.api.instrumentation.ProvidedTags
     *            provided} by {@link com.oracle.truffle.llvm.runtime.LLVMLanguage}
     *
     * @return whether this node is associated with the given tag
     */
    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return hasTag(tag, NO_TAGS);
    }

    protected boolean hasTag(Class<? extends Tag> tag, Class<? extends Tag>[] irTags) {
        assert irTags != null;

        if (tag == StandardTags.StatementTag.class) {
            return hasStatementTag();
        }

        if (irNodeType != null) {
            for (Class<? extends Tag> providedTag : irTags) {
                if (tag == providedTag) {
                    return true;
                }
            }
        }

        return sourceDescriptor != null && sourceDescriptor.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return collectIRNodeData(sourceDescriptor != null ? sourceDescriptor.getNodeObject() : null);
    }

    @TruffleBoundary
    private LLVMNodeObject collectIRNodeData(LLVMNodeObject staticMembers) {
        final EconomicMap<String, Object> members = EconomicMap.create();

        // import IR-level node properties
        if (irNodeType != null) {
            if (irNodeType != VoidType.INSTANCE) {
                members.put(LLVMTags.EXTRA_DATA_VALUE_TYPE, LLVMTypeInteropWrapper.create(irNodeType, getDataLayout()));
            }
            collectIRNodeData(members);
        }

        // import dynamic properties not encoded in the nodes
        if (staticMembers != null) {
            final String[] keys = staticMembers.getKeys();
            final Object[] values = staticMembers.getValues();
            for (int i = 0; i < keys.length; i++) {
                members.put(keys[i], values[i]);
            }
        }

        return LLVMNodeObject.create(members);
    }

    protected void collectIRNodeData(@SuppressWarnings("unused") EconomicMap<String, Object> members) {
    }
}
