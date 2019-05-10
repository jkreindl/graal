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

    @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] ALL_TAGS = new Class[]{LLVMTags.SSARead.class, LLVMTags.SSAWrite.class,
                    LLVMTags.Constant.class, LLVMTags.Call.class, LLVMTags.Invoke.class, LLVMTags.Add.class, LLVMTags.Sub.class, LLVMTags.Mul.class, LLVMTags.Div.class, LLVMTags.Rem.class,
                    LLVMTags.ShiftLeft.class, LLVMTags.ShiftRight.class, LLVMTags.And.class, LLVMTags.Or.class, LLVMTags.XOr.class, LLVMTags.Phi.class, LLVMTags.Ret.class, LLVMTags.Br.class,
                    LLVMTags.Switch.class, LLVMTags.IndirectBr.class, LLVMTags.Resume.class, LLVMTags.Unreachable.class, LLVMTags.ICMP.class, LLVMTags.FCMP.class, LLVMTags.Cast.class,
                    LLVMTags.Alloca.class, LLVMTags.Load.class, LLVMTags.Store.class, LLVMTags.Fence.class, LLVMTags.CmpXchg.class, LLVMTags.AtomicRMW.class, LLVMTags.GetElementPtr.class,
                    LLVMTags.ExtractElement.class, LLVMTags.InsertElement.class, LLVMTags.ShuffleVector.class, LLVMTags.ExtractValue.class, LLVMTags.InsertValue.class, LLVMTags.Block.class,
                    LLVMTags.Select.class, LLVMTags.Internal.class, LLVMTags.PrepareCallArg.class, LLVMTags.ReadCallArg.class, LLVMTags.Intrinsic.class, LLVMTags.LandingPad.class};

    private LLVMTags() {
    }

    @Identifier(value = "SSA_READ")
    public static final class SSARead extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{SSAWrite.class};

        private SSARead() {
        }
    }

    @Identifier(value = "SSA_WRITE")
    public static final class SSAWrite extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{SSARead.class};

        private SSAWrite() {
        }
    }

    @Identifier(value = "GLOBAL_READ")
    public static final class GlobalRead extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{GlobalRead.class};
        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] CONSTANT_GLOBAL_READ_TAGS = new Class[]{GlobalRead.class, Constant.class};

        private GlobalRead() {
        }
    }

    @Identifier(value = "CONSTANT")
    public static final class Constant extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Constant.class};

        private Constant() {
        }
    }

    @Identifier(value = "ADD")
    public static final class Add extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Add.class};

        private Add() {
        }
    }

    @Identifier(value = "SUB")
    public static final class Sub extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Sub.class};

        private Sub() {
        }
    }

    @Identifier(value = "MUL")
    public static final class Mul extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Mul.class};

        private Mul() {
        }
    }

    @Identifier(value = "DIV")
    public static final class Div extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Div.class};

        private Div() {
        }
    }

    @Identifier(value = "REM")
    public static final class Rem extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Rem.class};

        private Rem() {
        }
    }

    @Identifier(value = "SHL")
    public static final class ShiftLeft extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ShiftLeft.class};

        private ShiftLeft() {
        }
    }

    @Identifier(value = "SHR")
    public static final class ShiftRight extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ShiftRight.class};

        private ShiftRight() {
        }
    }

    @Identifier(value = "AND")
    public static final class And extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{And.class};

        private And() {
        }
    }

    @Identifier(value = "OR")
    public static final class Or extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Or.class};

        private Or() {
        }
    }

    @Identifier(value = "XOR")
    public static final class XOr extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{XOr.class};

        private XOr() {
        }
    }

    @Identifier(value = "CALL")
    public static final class Call extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Call.class};

        private Call() {
        }
    }

    @Identifier(value = "INVOKE")
    public static final class Invoke extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Invoke.class};

        private Invoke() {
        }
    }

    @Identifier(value = "PHI")
    public static final class Phi extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Phi.class};

        private Phi() {
        }
    }

    @Identifier(value = "SELECT")
    public static final class Select extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Select.class};

        private Select() {
        }
    }

    @Identifier(value = "RET")
    public static final class Ret extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Ret.class};

        private Ret() {
        }
    }

    @Identifier(value = "BR")
    public static final class Br extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Br.class};

        private Br() {
        }
    }

    @Identifier(value = "SWITCH")
    public static final class Switch extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Switch.class};

        private Switch() {
        }
    }

    @Identifier(value = "INDIRECTBR")
    public static final class IndirectBr extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{IndirectBr.class};

        private IndirectBr() {
        }
    }

    @Identifier(value = "RESUME")
    public static final class Resume extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Resume.class};

        private Resume() {
        }
    }

    @Identifier(value = "LANDINGPAD")
    public static final class LandingPad extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{LandingPad.class};

        private LandingPad() {
        }
    }

    @Identifier(value = "UNREACHABLE")
    public static final class Unreachable extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Unreachable.class};

        private Unreachable() {
        }
    }

    @Identifier(value = "ICMP")
    public static final class ICMP extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ICMP.class};

        private ICMP() {
        }
    }

    @Identifier(value = "FCMP")
    public static final class FCMP extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{FCMP.class};

        private FCMP() {
        }
    }

    @Identifier(value = "CAST")
    public static final class Cast extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Cast.class};

        private Cast() {
        }
    }

    @Identifier(value = "ALLOCA")
    public static final class Alloca extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Alloca.class};

        private Alloca() {
        }
    }

    @Identifier(value = "LOAD")
    public static final class Load extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Load.class};

        private Load() {
        }
    }

    @Identifier(value = "STORE")
    public static final class Store extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Store.class};

        private Store() {
        }
    }

    @Identifier(value = "FENCE")
    public static final class Fence extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Fence.class};

        private Fence() {
        }
    }

    @Identifier(value = "CMPXCHG")
    public static final class CmpXchg extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{CmpXchg.class};

        private CmpXchg() {
        }
    }

    @Identifier(value = "ATOMICRMW")
    public static final class AtomicRMW extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{AtomicRMW.class};

        private AtomicRMW() {
        }
    }

    @Identifier(value = "GETELEMENTPTR")
    public static final class GetElementPtr extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{GetElementPtr.class};

        private GetElementPtr() {
        }
    }

    @Identifier(value = "EXTRACTELEMENT")
    public static final class ExtractElement extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ExtractElement.class};

        private ExtractElement() {
        }
    }

    @Identifier(value = "INSERTELEMENT")
    public static final class InsertElement extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{InsertElement.class};

        private InsertElement() {
        }
    }

    @Identifier(value = "SHUFFLEVECTOR")
    public static final class ShuffleVector extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ShuffleVector.class};

        private ShuffleVector() {
        }
    }

    @Identifier(value = "EXTRACTVALUE")
    public static final class ExtractValue extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ExtractValue.class};

        private ExtractValue() {
        }
    }

    @Identifier(value = "INSERTVALUE")
    public static final class InsertValue extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{InsertValue.class};

        private InsertValue() {
        }
    }

    @Identifier(value = "BLOCK")
    public static final class Block extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Block.class};

        private Block() {
        }
    }

    @Identifier(value = "INTERNAL")
    public static final class Internal extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Internal.class};

        private Internal() {
        }
    }

    @Identifier(value = "PREPARE_CALL_ARG")
    public static final class PrepareCallArg extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{PrepareCallArg.class};

        private PrepareCallArg() {
        }
    }

    @Identifier(value = "READ_CALL_ARG")
    public static final class ReadCallArg extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{ReadCallArg.class};

        private ReadCallArg() {
        }
    }

    @Identifier(value = "INTRINSIC")
    public static final class Intrinsic extends Tag {

        @SuppressWarnings("unchecked") public static final Class<? extends Tag>[] SINGLE_EXPRESSION_TAG = new Class[]{Intrinsic.class};

        private Intrinsic() {
        }
    }
}
