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
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.nodes.Node;

public class LLVMNodeObjectKeys implements TruffleObject {

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof LLVMNodeObjectKeys;
    }

    @CompilationFinal(dimensions = 1) private final String[] properties;

    public LLVMNodeObjectKeys(String[] properties) {
        this.properties = properties;
    }

    @Override
    public ForeignAccess getForeignAccess() {
        return NodeObjectKeysMessageResolutionForeign.ACCESS;
    }

    @MessageResolution(receiverType = LLVMNodeObjectKeys.class)
    abstract static class NodeObjectKeysMessageResolution {

        @Resolve(message = "HAS_KEYS")
        abstract static class HasKeys extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return false;
            }
        }

        @Resolve(message = "HAS_SIZE")
        abstract static class HasSize extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return true;
            }
        }

        @Resolve(message = "GET_SIZE")
        abstract static class GetSize extends Node {
            public int access(LLVMNodeObjectKeys receiver) {
                return receiver.properties.length;
            }
        }

        @Resolve(message = "READ")
        abstract static class ReadNode extends Node {
            public String access(LLVMNodeObjectKeys receiver, int index) {
                if (index < 0 || index >= receiver.properties.length) {
                    CompilerDirectives.transferToInterpreter();
                    throw UnknownIdentifierException.raise(String.valueOf(index));
                }

                return receiver.properties[index];
            }

            public String access(LLVMNodeObjectKeys receiver, long index) {
                return access(receiver, (int) index);
            }
        }

        @Resolve(message = "IS_NULL")
        public abstract static class IsNull extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_POINTER")
        public abstract static class IsPointer extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_EXECUTABLE")
        public abstract static class IsExecutable extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_INSTANTIABLE")
        public abstract static class IsInstantiable extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return false;
            }
        }

        @Resolve(message = "IS_BOXED")
        public abstract static class IsBoxed extends Node {
            public boolean access(@SuppressWarnings("unused") LLVMNodeObjectKeys receiver) {
                return false;
            }
        }
    }
}
