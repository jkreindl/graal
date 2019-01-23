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
package com.oracle.truffle.llvm.runtime.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.collections.EconomicMap;

public final class LLVMNodeObject implements TruffleObject {

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof LLVMNodeObject;
    }

    private final EconomicMap<String, Object> entries;
    @CompilationFinal(dimensions = 1) private final String[] keys;

    @TruffleBoundary
    public LLVMNodeObject(String[] keys, Object[] values) {
        assert keys != null;
        assert values != null;
        assert keys.length == values.length;
        this.keys = keys;
        this.entries = EconomicMap.create(keys.length);
        for (int i = 0; i < keys.length; i++) {
            this.entries.put(keys[i], values[i]);
        }
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return LLVMNodeObjectMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = LLVMNodeObject.class)
    abstract static class LLVMNodeObjectMessageResolution {

        @TruffleBoundary
        private static boolean isInvalidKey(LLVMNodeObject receiver, String key) {
            return key == null || !receiver.entries.containsKey(key);
        }

        @Resolve(message = "HAS_KEYS")
        abstract static class HasKeys extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return true;
            }
        }

        @Resolve(message = "KEYS")
        abstract static class Keys extends Node {
            public TruffleObject access(LLVMNodeObject receiver) {
                return new LLVMNodeObjectKeys(receiver.keys);
            }
        }

        @Resolve(message = "KEY_INFO")
        abstract static class KeyInfo extends Node {

            public int access(LLVMNodeObject receiver, String key) {
                if (isInvalidKey(receiver, key)) {
                    CompilerDirectives.transferToInterpreter();
                    throw UnknownIdentifierException.raise(key);
                }

                return com.oracle.truffle.api.interop.KeyInfo.READABLE;
            }
        }

        @Resolve(message = "READ")
        abstract static class Read extends Node {

            @TruffleBoundary
            private static Object readKey(LLVMNodeObject receiver, String key) {
                return receiver.entries.get(key);
            }

            public Object access(LLVMNodeObject receiver, String key) {
                if (isInvalidKey(receiver, key)) {
                    CompilerDirectives.transferToInterpreter();
                    throw UnknownIdentifierException.raise(key);
                }

                return readKey(receiver, key);
            }
        }

        @Resolve(message = "IS_NULL")
        public abstract static class IsNull extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_POINTER")
        public abstract static class IsPointer extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return false;
            }
        }

        @Resolve(message = "HAS_SIZE")
        public abstract static class HasSize extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        public abstract static class IsExecutable extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_INSTANTIABLE")
        public abstract static class IsInstantiable extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_BOXED")
        public abstract static class IsBoxed extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObject receiver) {
                return false;
            }
        }
    }
}
