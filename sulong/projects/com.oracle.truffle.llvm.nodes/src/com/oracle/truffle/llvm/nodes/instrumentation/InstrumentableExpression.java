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
package com.oracle.truffle.llvm.nodes.instrumentation;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

@NodeInfo(shortName = "IR expression", description = "wrapper node marking IR-Level expressions for instrumentation", language = "LLVM IR")
public class InstrumentableExpression extends LLVMExpressionNode implements InstrumentableNode {

    private static final SourceSection SOURCE_SECTION;

    static {
        final Source source = Source.newBuilder("llvm", "LLVM IR expression", "<llvm expression>").mimeType("text/plain").build();
        SOURCE_SECTION = source.createUnavailableSection();
    }

    @CompilationFinal(dimensions = 1) private final Class<? extends Tag>[] tags;
    private final Object nodeObject;

    @Child private LLVMExpressionNode expr;

    public InstrumentableExpression(LLVMExpressionNode expr, Class<? extends Tag>[] tags, Object nodeObject) {
        assert tags != null;
        assert Arrays.stream(tags).allMatch(Objects::nonNull);
        this.tags = tags;

        // null for instructions without additional info, e.g., unreachable
        this.nodeObject = nodeObject;

        assert expr != null;
        this.expr = expr;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return expr.executeGeneric(frame);
    }

    public LLVMExpressionNode getExpression() {
        return expr;
    }

    @Override
    public boolean isInstrumentable() {
        return true;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return LLVMTags.isTagProvided(this.tags, tag);
    }

    @Override
    public Object getNodeObject() {
        return nodeObject;
    }

    @Override
    public SourceSection getSourceSection() {
        return SOURCE_SECTION;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        return Arrays.stream(tags).map(Class::getSimpleName).collect(Collectors.joining(", ", "Instrumentable Expression {", "}"));
    }
}
