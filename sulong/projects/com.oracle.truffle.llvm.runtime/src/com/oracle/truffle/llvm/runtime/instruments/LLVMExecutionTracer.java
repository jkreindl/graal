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
package com.oracle.truffle.llvm.runtime.instruments;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.runtime.nodes.LLVMTags;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public final class LLVMExecutionTracer {

    private LLVMExecutionTracer() {
    }

    @SuppressWarnings("unchecked")
    public static void initialize(TruffleLanguage.Env env) {
        final Instrumenter instrumenter = env.lookup(Instrumenter.class);
        assert instrumenter != null;

        final HashSet<Class<? extends Tag>> llvmTags = new HashSet<>(Arrays.asList(LLVMTags.ALL_TAGS));
        final PrintWriter writer = new PrintWriter(env.err());
        instrumenter.attachExecutionEventFactory(SourceSectionFilter.newBuilder().tagIs(LLVMTags.ALL_TAGS).includeInternal(true).build(), context -> {
            final String tags = instrumenter.queryTags(context.getInstrumentedNode()).stream().map(tag -> (Class<? extends Tag>) tag).filter(llvmTags::contains).map(
                            Tag::getIdentifier).collect(Collectors.joining(", "));
            return new TraceNode(tags, writer);
        });
    }

    static class TraceNode extends ExecutionEventNode {

        private final String tags;
        private final PrintWriter targetWriter;

        TraceNode(String tags, PrintWriter targetWriter) {
            this.tags = tags;
            this.targetWriter = targetWriter;
        }

        @TruffleBoundary
        private void print(String str) {
            targetWriter.println(str);
        }

        @Override
        protected void onEnter(VirtualFrame frame) {
            print("<node tags=\"" + tags + "\">");
        }

        @Override
        protected void onReturnValue(VirtualFrame frame, Object result) {
            print("</node>");
        }

        @Override
        protected void onReturnExceptional(VirtualFrame frame, Throwable exception) {
            print("</node>");
        }
    }
}
