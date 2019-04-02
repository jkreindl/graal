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
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType.ValueKind;
import com.oracle.truffle.llvm.runtime.interop.convert.ForeignToLLVM;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

@GenerateUncached
public abstract class LLVMForeignWriteNode extends LLVMNode {

    static final int VALUE_KIND_COUNT = ValueKind.values().length;

    public abstract void execute(LLVMPointer ptr, LLVMInteropType type, Object value) throws UnsupportedMessageException;

    @Specialization(guards = "type.getKind() == cachedKind", limit = "VALUE_KIND_COUNT")
    static void doValue(LLVMPointer ptr, LLVMInteropType.Value type, Object value,
                    @Cached(value = "type.getKind()", allowUncached = true) @SuppressWarnings("unused") LLVMInteropType.ValueKind cachedKind,
                    @Cached(value = "createCachedStore()") DirectCallNode store) {
        store.call(ptr, type, value);
    }

    @Specialization(replaces = "doValue")
    @TruffleBoundary
    void doValueUncached(LLVMPointer ptr, LLVMInteropType.Value type, Object value) {
        LLVMInteropType.ValueKind kind = type.getKind();
        doValue(ptr, type, value, kind, createCachedStore());
    }

    @Specialization
    @SuppressWarnings("unused")
    static void doStructured(LLVMPointer ptr, LLVMInteropType.Structured type, Object value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    protected DirectCallNode createCachedStore() {
        return LLVMForeignUtils.createDirectCall(new ForeignLLVMStore());
    }

    static class ForeignLLVMStore extends RootNode {

        @Child LLVMStatementNode store;

        ForeignLLVMStore() {
            super(LLVMLanguage.getLanguage());
            final LLVMExpressionNode ptr = new LLVMForeignUtils.ArgReadNode(0);
            final LLVMExpressionNode type = new LLVMForeignUtils.ArgReadNode(1);
            final LLVMExpressionNode value = new LLVMForeignUtils.ArgReadNode(2);
            this.store = LLVMForeignWriteNodeGen.ForeignStoreNodeGen.create(ptr, type, value);
        }

        @Override
        protected boolean isInstrumentable() {
            return false;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            store.execute(frame);
            return null;
        }
    }

    @NodeChild(value = "ptr", type = LLVMExpressionNode.class)
    @NodeChild(value = "type", type = LLVMExpressionNode.class)
    @NodeChild(value = "value", type = LLVMExpressionNode.class)
    static abstract class ForeignStoreNode extends LLVMStatementNode {

        static final int VALUE_KIND_COUNT = ValueKind.values().length;

        protected ForeignToLLVM createForeignToLLVM(LLVMInteropType.Value type) {
            return getNodeFactory().createForeignToLLVM(type);
        }

        @Specialization(guards = "type.getKind() == cachedKind", limit = "VALUE_KIND_COUNT")
        protected void doStore(VirtualFrame frame,
                        LLVMPointer ptr,
                        @SuppressWarnings("unused") LLVMInteropType.Value type,
                        Object value,
                        @Cached(value = "type.getKind()", allowUncached = true) @SuppressWarnings("unused") LLVMInteropType.ValueKind cachedKind,
                        @Cached("createForeignToLLVM(type)") ForeignToLLVM toLLVM,
                        @Cached(value = "createStoreNode(cachedKind)") LLVMStoreNode store) {
            Object llvmValue = toLLVM.executeWithForeignToLLVMType(value, type.getBaseType(), cachedKind.foreignToLLVMType);
            store.executeWithTarget(frame, ptr, llvmValue);
        }

        @Specialization(replaces = "doValue")
        @TruffleBoundary
        void doValueUncached(LLVMPointer ptr, LLVMInteropType.Value type, Object value) {
            LLVMInteropType.ValueKind kind = type.getKind();
            doValue(ptr, type, value, kind, createStoreNode(kind), ForeignToLLVM.getUncached());
        }

        protected LLVMStoreNode createStoreNode(LLVMInteropType.ValueKind kind) {
            CompilerAsserts.neverPartOfCompilation();
            ContextReference<LLVMContext> ctxRef = lookupContextReference(LLVMLanguage.class);
            return ctxRef.get().getNodeFactory().createStoreNode(kind);
        }
    }
}
