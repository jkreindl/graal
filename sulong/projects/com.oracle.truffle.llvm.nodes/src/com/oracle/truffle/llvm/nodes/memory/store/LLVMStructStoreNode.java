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
package com.oracle.truffle.llvm.nodes.memory.store;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemMoveNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMToNativeNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

@NodeField(type = long.class, name = "structSize")
public abstract class LLVMStructStoreNode extends LLVMStoreNodeCommon {

    public abstract long getStructSize();

    @Child private LLVMMemMoveNode memMove;

    protected LLVMStructStoreNode(LLVMMemMoveNode memMove) {
        this(null, memMove);
    }

    protected LLVMStructStoreNode(LLVMSourceLocation sourceLocation, LLVMMemMoveNode memMove) {
        super(sourceLocation);
        this.memMove = memMove;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "getStructSize() == 0")
    protected void noCopy(Object address, Object value) {
        // nothing to do
    }

    @Specialization(guards = {"getStructSize() > 0", "!isAutoDerefHandle(address)", "!isAutoDerefHandle(value)"})
    protected void doOp(VirtualFrame frame, LLVMNativePointer address, LLVMNativePointer value) {
        memMove.executeWithTarget(frame, address, value, getStructSize());
    }

    @Specialization(guards = {"getStructSize() > 0", "isAutoDerefHandle(addr)", "isAutoDerefHandle(value)"})
    protected void doOpDerefHandle(VirtualFrame frame, LLVMNativePointer addr, LLVMNativePointer value) {
        doManaged(frame, getDerefHandleGetReceiverNode().execute(addr), getDerefHandleGetReceiverNode().execute(value));
    }

    @Specialization(guards = "getStructSize() > 0")
    protected void doManaged(VirtualFrame frame, LLVMManagedPointer address, LLVMManagedPointer value) {
        memMove.executeWithTarget(frame, address, value, getStructSize());
    }

    @Specialization(guards = {"getStructSize() > 0", "!isAutoDerefHandle(address)"}, replaces = "doOp")
    protected void doConvert(VirtualFrame frame, LLVMNativePointer address, LLVMPointer value,
                    @Cached("createToNativeWithTarget()") LLVMToNativeNode toNative) {
        memMove.executeWithTarget(frame, address, toNative.executeWithTarget(value), getStructSize());
    }

    @Override
    public long getStoreSize() {
        return getStructSize();
    }
}
