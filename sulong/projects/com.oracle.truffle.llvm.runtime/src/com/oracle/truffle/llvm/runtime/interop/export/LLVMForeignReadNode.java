/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.runtime.interop.export;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.interop.LLVMDataEscapeNode;
import com.oracle.truffle.llvm.runtime.interop.LLVMDataEscapeNodeGen;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.interop.convert.ForeignToLLVM;
import com.oracle.truffle.llvm.runtime.library.LLVMLibraryUtils;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

@GenerateUncached
public abstract class LLVMForeignReadNode extends LLVMNode {

    static final int VALUE_KIND_COUNT = LLVMInteropType.ValueKind.values().length;

    public abstract Object execute(LLVMPointer ptr, LLVMInteropType type);

    @Specialization
    static Object doStructured(LLVMPointer ptr, @SuppressWarnings(value = "unused") LLVMInteropType.Structured type) {
        // inline structured value, nothing to read
        return ptr;
    }

    @Specialization(guards = "type.getKind() == cachedKind", limit = "VALUE_KIND_COUNT")
    static Object doValue(LLVMPointer ptr, LLVMInteropType.Value type,
                    @Cached(value = "type.getKind()", allowUncached = true) @SuppressWarnings(value = "unused") LLVMInteropType.ValueKind cachedKind,
                    @Cached(value = "createCachedRead()", allowUncached = true) DirectCallNode load) {
        return load.call(ptr, type);
    }

    @Specialization(replaces = "doValue")
    @TruffleBoundary
    Object doValueUncached(LLVMPointer ptr, LLVMInteropType.Value type) {
        LLVMInteropType.ValueKind kind = type.getKind();
        return doValue(ptr, type, kind, createCachedRead());
    }

    LLVMLoadNode createLoadNode(LLVMInteropType.ValueKind kind) {
        CompilerAsserts.neverPartOfCompilation();
        TruffleLanguage.ContextReference<LLVMContext> ctxRef = lookupContextReference(LLVMLanguage.class);
        return ctxRef.get().getNodeFactory().createLoadNode(kind);
    }

    protected DirectCallNode createCachedRead() {
        return LLVMLibraryUtils.createDirectCall(new ForeignLLVMLoad());
    }

    static class ForeignLLVMLoad extends RootNode {

        @Child LLVMExpressionNode load;

        ForeignLLVMLoad() {
            super(LLVMLanguage.getLanguage());
            final LLVMExpressionNode ptr = new LLVMLibraryUtils.LLVMArgReadNode(0);
            final LLVMExpressionNode type = new LLVMLibraryUtils.LLVMArgReadNode(1);
            this.load = LLVMForeignReadNodeGen.ForeignLoadNodeGen.create(ptr, type);
        }

        @Override
        protected boolean isInstrumentable() {
            return false;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return load.executeGeneric(frame);
        }
    }

    @NodeChild(value = "ptr", type = LLVMExpressionNode.class)
    @NodeChild(value = "type", type = LLVMExpressionNode.class)
    static abstract class ForeignLoadNode extends LLVMExpressionNode {

        static final int VALUE_KIND_COUNT = LLVMInteropType.ValueKind.values().length;

        protected ForeignToLLVM createForeignToLLVM(LLVMInteropType.Value type) {
            return getNodeFactory().createForeignToLLVM(type);
        }

        @Specialization(guards = "type.getKind() == cachedKind", limit = "VALUE_KIND_COUNT")
        static Object doValue(VirtualFrame frame, LLVMPointer ptr, LLVMInteropType.Value type,
                              @Cached(value = "type.getKind()", allowUncached = true) @SuppressWarnings(value = "unused") LLVMInteropType.ValueKind cachedKind,
                              @Cached(value = "createLoadNode(cachedKind)", allowUncached = true) LLVMLoadNode load, @Cached LLVMDataEscapeNode dataEscape) {
            Object ret = load.executeWithTarget(frame, ptr);
            return dataEscape.executeWithType(ret, type.getBaseType());
        }

        @Specialization(replaces = "doValue")
        @TruffleBoundary
        Object doValueUncached(LLVMPointer ptr, LLVMInteropType.Value type) {
            LLVMInteropType.ValueKind kind = type.getKind();
            return doValue(ptr, type, kind, createLoadNode(kind), LLVMDataEscapeNodeGen.getUncached());
        }

        LLVMLoadNode createLoadNode(LLVMInteropType.ValueKind kind) {
            CompilerAsserts.neverPartOfCompilation();
            TruffleLanguage.ContextReference<LLVMContext> ctxRef = lookupContextReference(LLVMLanguage.class);
            return ctxRef.get().getNodeFactory().createLoadNode(kind);
        }
    }
}
