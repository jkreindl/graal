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

import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.instrumentation.Tag.Identifier;

public final class LLVMTags {

    private LLVMTags() {
    }

    @Identifier(value = "SSA_READ")
    public static final class SSARead extends Tag {
        private SSARead() {
        }
    }

    @Identifier(value = "SSA_WRITE")
    public static final class SSAWrite extends Tag {
        private SSAWrite() {
        }
    }

    @Identifier(value = "CONSTANT")
    public static final class Constant extends Tag {
        private Constant() {
        }
    }

    @Identifier(value = "ADD")
    public static final class Add extends Tag {
        private Add() {
        }
    }

    @Identifier(value = "SUB")
    public static final class Sub extends Tag {
        private Sub() {
        }
    }

    @Identifier(value = "MUL")
    public static final class Mul extends Tag {
        private Mul() {
        }
    }

    @Identifier(value = "DIV")
    public static final class Div extends Tag {
        private Div() {
        }
    }

    @Identifier(value = "REM")
    public static final class Rem extends Tag {
        private Rem() {
        }
    }

    @Identifier(value = "SHL")
    public static final class ShiftLeft extends Tag {
        private ShiftLeft() {
        }
    }

    @Identifier(value = "SHR")
    public static final class ShiftRight extends Tag {
        private ShiftRight() {
        }
    }

    @Identifier(value = "AND")
    public static final class And extends Tag {
        private And() {
        }
    }

    @Identifier(value = "OR")
    public static final class Or extends Tag {
        private Or() {
        }
    }

    @Identifier(value = "XOR")
    public static final class XOr extends Tag {
        private XOr() {
        }
    }

    @Identifier(value = "CALL")
    public static final class Call extends Tag {
        private Call() {
        }
    }

    @Identifier(value = "INVOKE")
    public static final class Invoke extends Tag {
        private Invoke() {
        }
    }

    @Identifier(value = "PHI")
    public static final class Phi extends Tag {
        private Phi() {
        }
    }

    @Identifier(value = "SELECT")
    public static final class Select extends Tag {
        private Select() {
        }
    }

    @Identifier(value = "RET")
    public static final class Ret extends Tag {
        private Ret() {
        }
    }

    @Identifier(value = "BR")
    public static final class Br extends Tag {
        private Br() {
        }
    }

    @Identifier(value = "SWITCH")
    public static final class Switch extends Tag {
        private Switch() {
        }
    }

    @Identifier(value = "INDIRECTBR")
    public static final class IndirectBr extends Tag {
        private IndirectBr() {
        }
    }

    @Identifier(value = "RESUME")
    public static final class Resume extends Tag {
        private Resume() {
        }
    }

    @Identifier(value = "UNREACHABLE")
    public static final class Unreachable extends Tag {
        private Unreachable() {
        }
    }

    @Identifier(value = "ICMP")
    public static final class ICMP extends Tag {
        private ICMP() {
        }
    }

    @Identifier(value = "FCMP")
    public static final class FCMP extends Tag {
        private FCMP() {
        }
    }

    @Identifier(value = "CAST")
    public static final class Cast extends Tag {
        private Cast() {
        }
    }

    @Identifier(value = "ALLOCA")
    public static final class Alloca extends Tag {
        private Alloca() {
        }
    }

    @Identifier(value = "LOAD")
    public static final class Load extends Tag {
        private Load() {
        }
    }

    @Identifier(value = "STORE")
    public static final class Store extends Tag {
        private Store() {
        }
    }

    @Identifier(value = "FENCE")
    public static final class Fence extends Tag {
        private Fence() {
        }
    }

    @Identifier(value = "CMPXCHG")
    public static final class CmpXchg extends Tag {
        private CmpXchg() {
        }
    }

    @Identifier(value = "ATOMICRMW")
    public static final class AtomicRMW extends Tag {
        private AtomicRMW() {
        }
    }

    @Identifier(value = "GETELEMENTPTR")
    public static final class GetElementPtr extends Tag {
        private GetElementPtr() {
        }
    }

    @Identifier(value = "EXTRACTELEMENT")
    public static final class ExtractElement extends Tag {
        public ExtractElement() {
        }
    }

    @Identifier(value = "INSERTELEMENT")
    public static final class InsertElement extends Tag {
        public InsertElement() {
        }
    }

    @Identifier(value = "SHUFFLEVECTOR")
    public static final class ShuffleVector extends Tag {
        public ShuffleVector() {
        }
    }

    @Identifier(value = "EXTRACTVALUE")
    public static final class ExtractValue extends Tag {
        public ExtractValue() {
        }
    }

    @Identifier(value = "INSERTVALUE")
    public static final class InsertValue extends Tag {
        public InsertValue() {
        }
    }

    @Identifier(value = "BLOCK")
    public static final class Block extends Tag {
        public Block() {
        }
    }

    @Identifier(value = "INTERNAL")
    public static final class Internal extends Tag {
        public Internal() {
        }
    }

    @Identifier(value = "PREPARE_CALL_ARG")
    public static final class PrepareCallArg extends Tag {
        public PrepareCallArg() {
        }
    }

    @Identifier(value = "READ_CALL_ARG")
    public static final class ReadCallArg extends Tag {
        public ReadCallArg() {
        }
    }
}
