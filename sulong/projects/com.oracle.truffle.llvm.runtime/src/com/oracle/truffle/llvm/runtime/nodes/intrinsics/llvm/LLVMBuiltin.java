/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.runtime.nodes.intrinsics.llvm;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.nodes.memory.LLVMGetElementPtrNode.LLVMIncrementPointerNode;
import com.oracle.truffle.llvm.runtime.nodes.memory.LLVMGetElementPtrNodeGen.LLVMIncrementPointerNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.memory.store.LLVMI16StoreNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.memory.store.LLVMI1StoreNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.memory.store.LLVMI32StoreNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.memory.store.LLVMI64StoreNodeGen;
import com.oracle.truffle.llvm.runtime.nodes.memory.store.LLVMI8StoreNodeGen;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import org.graalvm.collections.EconomicMap;

public abstract class LLVMBuiltin extends LLVMExpressionNode {

    protected static LLVMStoreNode createStoreI1() {
        return LLVMI1StoreNodeGen.create(null, null);
    }

    protected static LLVMStoreNode createStoreI8() {
        return LLVMI8StoreNodeGen.create(null, null);
    }

    protected static LLVMStoreNode createStoreI16() {
        return LLVMI16StoreNodeGen.create(null, null);
    }

    protected static LLVMStoreNode createStoreI32() {
        return LLVMI32StoreNodeGen.create(null, null);
    }

    protected static LLVMStoreNode createStoreI64() {
        return LLVMI64StoreNodeGen.create(null, null);
    }

    protected LLVMIncrementPointerNode getIncrementPointerNode() {
        return LLVMIncrementPointerNodeGen.create();
    }

    // Each builtin node implementation may provide the functionality of several differently typed
    // or named builtins/intrinsics, so storing these statically would require lots of code. In the
    // parser, the concrete name and type are known, so we assign them there.
    @CompilationFinal private String builtinName;
    @CompilationFinal private FunctionType builtinType;

    public LLVMBuiltin() {
        this.builtinName = null;
        this.builtinType = null;
    }

    public void setBuiltinName(String builtinName) {
        CompilerAsserts.neverPartOfCompilation("Name of builtin must be constant after parsing");
        this.builtinName = builtinName;
    }

    public void setBuiltinType(FunctionType builtinType) {
        CompilerAsserts.neverPartOfCompilation("Type of builtin must be constant after parsing");
        this.builtinType = builtinType;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (builtinType == null) {
            return super.hasTag(tag);
        } else if (builtinType.getReturnType() instanceof VoidType) {
            return super.hasTag(tag, LLVMTags.Intrinsic.VOID_INTRINSIC_TAGS);
        } else {
            return super.hasTag(tag, LLVMTags.Intrinsic.VALUE_INTRINSIC_TAGS);
        }
    }

    @Override
    @TruffleBoundary
    protected void collectIRNodeData(EconomicMap<String, Object> members) {
        members.put(LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_NAME, builtinName);
        members.put(LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_TYPE, builtinType);
    }

    public static class ExpressionWrapper extends LLVMBuiltin {

        @Child private LLVMExpressionNode wrappedNode;

        public ExpressionWrapper(LLVMExpressionNode wrappedNode) {
            this.wrappedNode = wrappedNode;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            return wrappedNode.executeGeneric(frame);
        }
    }
}
