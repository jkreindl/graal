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
package com.oracle.truffle.llvm.nodes.intrinsics.interop;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMReadStringNodeGen.ForeignReadStringNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMReadStringNodeGen.PointerReadStringNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetElementPtrNode.LLVMIncrementPointerNode;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetElementPtrNodeGen.LLVMIncrementPointerNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI8LoadNodeGen;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobal;
import com.oracle.truffle.llvm.runtime.interop.LLVMAsForeignNode;
import com.oracle.truffle.llvm.runtime.interop.LLVMTypedForeignObject;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNode;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

public abstract class LLVMReadStringNode extends LLVMNode {

    @Child PointerReadStringNode readOther;

    public abstract String executeWithTarget(VirtualFrame frame, Object address);

    @Specialization
    String readString(String address) {
        return address;
    }

    @Specialization(guards = "isForeign(foreign)")
    String readForeign(VirtualFrame frame, LLVMManagedPointer foreign,
                    @Cached("create()") ForeignReadStringNode read) {
        return read.execute(frame, foreign);
    }

    @Fallback
    String readOther(VirtualFrame frame, Object address) {
        if (readOther == null) {
            readOther = insert(PointerReadStringNode.create());
        }
        return readOther.execute(frame, address);
    }

    protected static boolean isForeign(LLVMManagedPointer pointer) {
        return pointer.getOffset() == 0 && pointer.getObject() instanceof LLVMTypedForeignObject;
    }

    abstract static class Dummy extends LLVMNode {

        protected abstract LLVMManagedPointer execute();
    }

    @NodeChild(value = "object", type = Dummy.class)
    @NodeChild(value = "foreign", type = LLVMAsForeignNode.class, executeWith = "object")
    abstract static class ForeignReadStringNode extends LLVMNode {

        protected abstract String execute(VirtualFrame frame, LLVMManagedPointer foreign);

        @Specialization(limit = "3")
        String doDefault(VirtualFrame frame, @SuppressWarnings("unused") LLVMManagedPointer object, TruffleObject foreign,
                        @CachedLibrary("foreign") InteropLibrary interop,
                        @Cached PointerReadStringNode read) {
            if (interop.isString(foreign)) {
                try {
                    return interop.asString(foreign);
                } catch (UnsupportedMessageException e) {
                }
            }
            return read.execute(frame, object);
        }

        public static ForeignReadStringNode create() {
            return ForeignReadStringNodeGen.create(null, LLVMAsForeignNode.createOptional());
        }
    }

    abstract static class PointerReadStringNode extends LLVMNode {

        @Child private LLVMIncrementPointerNode inc = LLVMIncrementPointerNodeGen.create();
        @Child private LLVMLoadNode read = LLVMI8LoadNodeGen.create(null);

        protected abstract String execute(VirtualFrame frame, Object address);

        boolean isReadOnlyMemory(LLVMPointer address) {
            CompilerAsserts.neverPartOfCompilation();
            LLVMGlobal global = lookupContextReference(LLVMLanguage.class).get().findGlobal(address);
            if (global != null) {
                return global.isReadOnly();
            } else {
                return false;
            }
        }

        @Specialization(guards = {"cachedAddress.equals(address)", "isReadOnlyMemory(cachedAddress)"})
        String doCachedPointer(@SuppressWarnings("unused") VirtualFrame frame, @SuppressWarnings("unused") LLVMPointer address,
                        @Cached("address") @SuppressWarnings("unused") LLVMPointer cachedAddress,
                        @Cached("doReadString(frame, cachedAddress)") String result) {
            return result;
        }

        @Specialization(replaces = "doCachedPointer")
        String doReadString(VirtualFrame frame, Object address) {
            Object ptr = address;
            int length = 0;
            while ((byte) read.executeWithTarget(frame, ptr) != 0) {
                length++;
                ptr = inc.executeWithTarget(ptr, Byte.BYTES);
            }

            char[] string = new char[length];

            ptr = address;
            for (int i = 0; i < length; i++) {
                string[i] = (char) Byte.toUnsignedInt((byte) read.executeWithTarget(frame, ptr));
                ptr = inc.executeWithTarget(ptr, Byte.BYTES);
            }

            return toString(string);
        }

        @TruffleBoundary
        private static String toString(char[] string) {
            return new String(string);
        }

        public static PointerReadStringNode create() {
            return PointerReadStringNodeGen.create();
        }
    }
}
