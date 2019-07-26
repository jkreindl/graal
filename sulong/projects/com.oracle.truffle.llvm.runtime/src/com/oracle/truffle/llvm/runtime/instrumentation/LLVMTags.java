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
package com.oracle.truffle.llvm.runtime.instrumentation;

import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.instrumentation.Tag.Identifier;

/**
 * This class describes the LLVM IR-level {@link Tag instrumentation tags} provided by Sulong. Most
 * tags directly correspond to one or a group of similar LLVM IR instructions. The remaining tags
 * provide meta-information about executing instructions, e.g., whether a node returns
 * {@link LLVMExpression a value} when a {@link Function function} or {@link Block basic block} is
 * entered. Many of these tags provide additional information, which is described for each tag in
 * more detail.
 *
 * The semantics of tags representing LLVM IR opcodes mostly match what is described in the LLVM
 * language reference manual. E.g., each instruction will receive input events corresponding to the
 * dynamic input values expected for the corresponding instructions. In some cases, however, the
 * semantics differ due to Sulong's execution model. Where such differences occur, they are
 * documented with the corresponding tag.
 */
public final class LLVMTags {

    @SuppressWarnings("unchecked") //
    public static final Class<? extends Tag>[] ALL_TAGS = new Class[]{//
                    LLVMTags.SSARead.class, //
                    LLVMTags.SSAWrite.class, //
                    LLVMTags.GlobalRead.class, //
                    LLVMTags.Call.class, //
                    LLVMTags.Invoke.class, //
                    LLVMTags.Add.class, //
                    LLVMTags.Sub.class, //
                    LLVMTags.Mul.class, //
                    LLVMTags.UDiv.class, //
                    LLVMTags.SDiv.class, //
                    LLVMTags.FDiv.class, //
                    LLVMTags.URem.class, //
                    LLVMTags.SRem.class, //
                    LLVMTags.FRem.class, //
                    LLVMTags.ShL.class, //
                    LLVMTags.LShR.class, //
                    LLVMTags.AShR.class, //
                    LLVMTags.And.class, //
                    LLVMTags.Or.class, //
                    LLVMTags.XOr.class, //
                    LLVMTags.Phi.class, //
                    LLVMTags.Ret.class, //
                    LLVMTags.Br.class, //
                    LLVMTags.Switch.class, //
                    LLVMTags.IndirectBr.class, //
                    LLVMTags.Resume.class, //
                    LLVMTags.Unreachable.class, //
                    LLVMTags.ICMP.class, //
                    LLVMTags.FCMP.class, //
                    LLVMTags.Cast.class, //
                    LLVMTags.Alloca.class, //
                    LLVMTags.Load.class, //
                    LLVMTags.Store.class, //
                    LLVMTags.Fence.class, //
                    LLVMTags.CmpXchg.class, //
                    LLVMTags.AtomicRMW.class, //
                    LLVMTags.GetElementPtr.class, //
                    LLVMTags.ExtractElement.class, //
                    LLVMTags.InsertElement.class, //
                    LLVMTags.ShuffleVector.class, //
                    LLVMTags.ExtractValue.class, //
                    LLVMTags.InsertValue.class, //
                    LLVMTags.Block.class, //
                    LLVMTags.Function.class, //
                    LLVMTags.Select.class, //
                    LLVMTags.ReadCallArg.class, //
                    LLVMTags.Intrinsic.class, //
                    LLVMTags.LandingPad.class, //
                    LLVMTags.Function.class, //
                    LLVMTags.LLVMExpression.class, //
                    LLVMTags.LLVMStatement.class, //
                    LLVMTags.LLVMControlFlow.class, //
                    LLVMTags.Literal.class, //
                    LLVMTags.SSALifetimeEnd.class //
    };

    private LLVMTags() {
    }

    /**
     * The type of the values returned by a value-creating semantic entity such as an expression, a
     * constant, or the access of an ssa-value.
     */
    public static final String EXTRA_DATA_VALUE_TYPE = "VALUE_TYPE";

    /**
     * Represents an instruction in LLVM IR that produces a value.
     */
    @Identifier(value = "LLVM_EXPRESSION")
    public static final class LLVMExpression extends Tag {
        private LLVMExpression() {
        }
    }

    /**
     * Represents an instruction in LLVM IR that does not produce a value.
     */
    @Identifier(value = "LLVM_STATEMENT")
    public static final class LLVMStatement extends Tag {
        private LLVMStatement() {
        }
    }

    /**
     * Represents an instruction in LLVM IR that directs control flow.
     */
    @Identifier(value = "LLVM_CONTROL_FLOW")
    public static final class LLVMControlFlow extends Tag {
        private LLVMControlFlow() {
        }
    }

    /**
     * Represents a read access to an ssa-value.
     */
    @Identifier(value = "SSA_READ")
    public static final class SSARead extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, SSARead.class};

        /**
         * The name of the SSA-value that is being read from.
         */
        public static final String EXTRA_DATA_SSA_SOURCE = "SOURCE_SSA_NAME";

        private SSARead() {
        }
    }

    /**
     * Represents a write access to an ssa-value.
     */
    @Identifier(value = "SSA_WRITE")
    public static final class SSAWrite extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMStatement.class, SSAWrite.class};

        /**
         * The name of the SSA-value that is being written to.
         */
        public static final String EXTRA_DATA_SSA_TARGET = "TARGET_SSA_NAME";

        private SSAWrite() {
        }
    }

    /**
     * Represents a read access to global constant, to a global variable or to a function. Such an
     * access always produces a literal, namely the descriptor of the function or global.
     */
    @Identifier(value = "GLOBAL_READ")
    public static final class GlobalRead extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Literal.class, GlobalRead.class};

        /**
         * LLVM-level name of the global symbol being accessed.
         */
        public static final String EXTRA_DATA_GLOBAL_NAME_LLVM = "LLVM_NAME";

        private GlobalRead() {
        }
    }

    /**
     * Represents an LLVM IR literal value.
     */
    @Identifier(value = "LITERAL")
    public static final class Literal extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Literal.class};

        private Literal() {
        }
    }

    /**
     * Represents the LLVM {@code add} and {@code fadd} instructions. Distinction between both
     * opcodes is apparent from the types of the respective input values.
     */
    @Identifier(value = "ADD")
    public static final class Add extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Add.class};

        private Add() {
        }
    }

    /**
     * Represents the LLVM {@code sub} and {@code fsub} instructions. Distinction between both
     * opcodes is apparent from the types of the respective input values.
     */
    @Identifier(value = "SUB")
    public static final class Sub extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Sub.class};

        private Sub() {
        }
    }

    /**
     * Represents the LLVM {@code mul} and {@code fmul} instructions. Distinction between both
     * opcodes is apparent from the types of the respective input values.
     */
    @Identifier(value = "MUL")
    public static final class Mul extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Mul.class};

        private Mul() {
        }
    }

    /**
     * Represents the LLVM {@code udiv} instruction.
     */
    @Identifier(value = "UDIV")
    public static final class UDiv extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, UDiv.class};

        private UDiv() {
        }
    }

    /**
     * Represents the LLVM {@code sdiv} instruction.
     */
    @Identifier(value = "SDIV")
    public static final class SDiv extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, SDiv.class};

        private SDiv() {
        }
    }

    /**
     * Represents the LLVM {@code fdiv} instruction.
     */
    @Identifier(value = "FDIV")
    public static final class FDiv extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, FDiv.class};

        private FDiv() {
        }
    }

    /**
     * Represents the LLVM {@code urem}, {@code srem} and {@code frem} instructions.
     */
    @Identifier(value = "UREM")
    public static final class URem extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, URem.class};

        private URem() {
        }
    }

    /**
     * Represents the LLVM {@code urem}, {@code srem} and {@code frem} instructions.
     */
    @Identifier(value = "SREM")
    public static final class SRem extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, SRem.class};

        private SRem() {
        }
    }

    /**
     * Represents the LLVM {@code urem}, {@code srem} and {@code frem} instructions.
     */
    @Identifier(value = "FREM")
    public static final class FRem extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, FRem.class};

        private FRem() {
        }
    }

    /**
     * Represents the LLVM {@code shl} instruction.
     */
    @Identifier(value = "SHL")
    public static final class ShL extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, ShL.class};

        private ShL() {
        }
    }

    /**
     * Represents the LLVM {@code lshr} instruction.
     */
    @Identifier(value = "LSHR")
    public static final class LShR extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, LShR.class};

        private LShR() {
        }
    }

    /**
     * Represents the LLVM {@code ashr} instruction.
     */
    @Identifier(value = "ASHR")
    public static final class AShR extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, AShR.class};

        private AShR() {
        }
    }

    /**
     * Represents the LLVM {@code and} instruction.
     */
    @Identifier(value = "AND")
    public static final class And extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, And.class};

        private And() {
        }
    }

    /**
     * Represents the LLVM {@code or} instruction.
     */
    @Identifier(value = "OR")
    public static final class Or extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Or.class};

        private Or() {
        }
    }

    /**
     * Represents the LLVM {@code xor} instruction.
     */
    @Identifier(value = "XOR")
    public static final class XOr extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, XOr.class};

        private XOr() {
        }
    }

    /**
     * Represents the LLVM {@code call} instruction.
     */
    @Identifier(value = "CALL")
    public static final class Call extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] VOID_CALL_TAGS = new Class[]{LLVMStatement.class, Call.class};

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] VALUE_CALL_TAGS = new Class[]{LLVMExpression.class, Call.class};

        /**
         * The number of argument values to this call.
         */
        public static final String EXTRA_DATA_ARGS_COUNT = "ARGS_COUNT";

        private Call() {
        }
    }

    /**
     * Represents the LLVM {@code invoke} instruction. Invocations that produce a value will not
     * cause a separate {@link SSAWrite} event. Instead, the write event is implicit and its target
     * is described by {@link LLVMTags.Invoke#EXTRA_DATA_SSA_TARGET EXTRA_DATA_SSA_TARGET}.
     */
    @Identifier(value = "INVOKE")
    public static final class Invoke extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] VOID_INVOKE_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, Invoke.class};

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] VALUE_INVOKE_TAGS = new Class[]{LLVMExpression.class, LLVMControlFlow.class, Invoke.class};

        /**
         * The number of argument values to this invoke.
         */
        public static final String EXTRA_DATA_ARGS_COUNT = "ARGS_COUNT";

        /**
         * The name of the SSA-value that is written by this invoke, or
         * {@link LLVMTags.Invoke#NO_TARGET NO_TARGET} if the invoked function does not produce a
         * value.
         */
        public static final String EXTRA_DATA_SSA_TARGET = "TARGET_SSA_NAME";

        /**
         * The value reported by {@link LLVMTags.Invoke#EXTRA_DATA_SSA_TARGET EXTRA_DATA_SSA_TARGET}
         * if this invocation does not produce a value.
         */
        public static final String NO_TARGET = "<none>";

        private Invoke() {
        }
    }

    /**
     * Represents a collection of LLVM {@code phi}-instructions. This receives input events in order
     * of the ssa-names stored in {@link LLVMTags.Phi#EXTRA_DATA_TARGETS EXTRA_DATA_TARGETS}, with
     * each input event denoting the assignment of the value to the corresponding ssa-value.
     */
    @Identifier(value = "PHI")
    public static final class Phi extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMStatement.class, Phi.class};

        /**
         * An array of ssa-value names that are written by the phis aggregated by this node. The
         * names are in the same sequence as their values are read.
         */
        public static final String EXTRA_DATA_TARGETS = "SSA_TARGETS";

        private Phi() {
        }
    }

    /**
     * Represents an LLVM {@code select} instruction or constant.
     */
    @Identifier(value = "SELECT")
    public static final class Select extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Select.class};

        private Select() {
        }
    }

    /**
     * Represents the LLVM {@code ret} instruction.
     */
    @Identifier(value = "RET")
    public static final class Ret extends Tag {

        /**
         * A flag to indicate whether this returns a value.
         */
        public static final String EXTRA_DATA_RETURN_WITH_VALUE = "RETURN_WITH_VALUE";

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, Ret.class};

        private Ret() {
        }
    }

    /**
     * Represents the LLVM {@code br} instruction. If the jump is conditional, the value of the
     * condition is received as input event.
     */
    @Identifier(value = "BR")
    public static final class Br extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, Br.class};

        /**
         * A boolean value indicating whether this jump is conditional or not.
         */
        public static final String EXTRA_DATA_IS_CONDITIONAL = "IS_CONDITIONAL";

        private Br() {
        }
    }

    /**
     * Represents the LLVM {@code switch} instruction.
     */
    @Identifier(value = "SWITCH")
    public static final class Switch extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, Switch.class};

        private Switch() {
        }
    }

    /**
     * Represents the LLVM {@code indirectbr} instruction.
     */
    @Identifier(value = "INDIRECTBR")
    public static final class IndirectBr extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, IndirectBr.class};

        private IndirectBr() {
        }
    }

    /**
     * Represents the LLVM {@code resume} instruction.
     */
    @Identifier(value = "RESUME")
    public static final class Resume extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, Resume.class};

        private Resume() {
        }
    }

    /**
     * Represents the LLVM {@code landingpad} instruction.
     */
    @Identifier(value = "LANDINGPAD")
    public static final class LandingPad extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LandingPad.class};

        private LandingPad() {
        }
    }

    /**
     * Represents the LLVM {@code unreachable} instruction.
     */
    @Identifier(value = "UNREACHABLE")
    public static final class Unreachable extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, LLVMControlFlow.class, Unreachable.class};

        private Unreachable() {
        }
    }

    /**
     * Represents the LLVM {@code icmp} instruction.
     */
    @Identifier(value = "ICMP")
    public static final class ICMP extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, ICMP.class};

        /**
         * The kind of comparison to perform.
         */
        public static final String EXTRA_DATA_KIND = "ICMP_KIND";

        private ICMP() {
        }
    }

    /**
     * Represents the LLVM {@code fcmp} instruction.
     */
    @Identifier(value = "FCMP")
    public static final class FCMP extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, FCMP.class};

        /**
         * The kind of comparison to perform.
         */
        public static final String EXTRA_DATA_KIND = "FCMP_KIND";

        private FCMP() {
        }
    }

    /**
     * Represents an LLVM {@code cast} instruction or constant.
     */
    @Identifier(value = "CAST")
    public static final class Cast extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Cast.class};

        /**
         * Whether the value produced by the cast is signed.
         */
        public static final String EXTRA_DATA_IS_SIGNED_CAST = "IS_SIGNED";

        private Cast() {
        }
    }

    /**
     * Represents the LLVM {@code alloca} instruction.
     */
    @Identifier(value = "ALLOCA")
    public static final class Alloca extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Alloca.class};

        /**
         * The type of values to be allocated.
         */
        public static final String EXTRA_DATA_ALLOCATION_TYPE = "ALLOCA_TYPE";

        /**
         * The alignment of values to be allocated.
         */
        public static final String EXTRA_DATA_ALLOCATION_ALIGNMENT = "ALLOCA_ALIGN";

        private Alloca() {
        }
    }

    /**
     * Represents the LLVM {@code load} instruction.
     */
    @Identifier(value = "LOAD")
    public static final class Load extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, Load.class};

        private Load() {
        }
    }

    /**
     * Represents the LLVM {@code store} instruction.
     */
    @Identifier(value = "STORE")
    public static final class Store extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, Store.class};

        private Store() {
        }
    }

    /**
     * Represents the LLVM {@code fence} instruction.
     */
    @Identifier(value = "FENCE")
    public static final class Fence extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] STATEMENT_TAGS = new Class[]{LLVMStatement.class, Fence.class};

        private Fence() {
        }
    }

    /**
     * Represents the LLVM {@code cmpxchg} instruction.
     */
    @Identifier(value = "CMPXCHG")
    public static final class CmpXchg extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, CmpXchg.class};

        private CmpXchg() {
        }
    }

    /**
     * Represents the LLVM {@code atomicrmw} instruction.
     */
    @Identifier(value = "ATOMICRMW")
    public static final class AtomicRMW extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, AtomicRMW.class};

        /**
         * The operation to perform atomically.
         */
        public static final String EXTRA_DATA_OPERATION = "OP";

        private AtomicRMW() {
        }
    }

    /**
     * Represents an LLVM {@code getelementptr} instruction or constant.
     */
    @Identifier(value = "GETELEMENTPTR")
    public static final class GetElementPtr extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, GetElementPtr.class};

        /**
         * Indicates whether the instruction has the "inbound" attribute.
         */
        public static final String EXTRA_DATA_IS_INBOUND = "IS_INBOUND";

        /**
         * Provides an array containing the types of the indices.
         */
        public static final String EXTRA_DATA_INDEX_TYPES = "INDEX_TYPES";

        /**
         * Provides an array providing the values of the indices. If the indices are constant
         * values, these values are contained in the array directly. If the indices are dynamic
         * values, the provided array contains {@link GetElementPtr#INDEX_DYNAMIC_VALUE} and the
         * actual index value will be provided as expression input.
         */
        public static final String EXTRA_DATA_INDEX_VALUES = "INDEX_VALUES";

        public static final String INDEX_DYNAMIC_VALUE = "DYNAMIC";

        private GetElementPtr() {
        }
    }

    /**
     * Represents the LLVM {@code extractelement} instruction.
     */
    @Identifier(value = "EXTRACTELEMENT")
    public static final class ExtractElement extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, ExtractElement.class};

        private ExtractElement() {
        }
    }

    /**
     * Represents the LLVM {@code insertelement} instruction.
     */
    @Identifier(value = "INSERTELEMENT")
    public static final class InsertElement extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, InsertElement.class};

        private InsertElement() {
        }
    }

    /**
     * Represents the LLVM {@code shufflevector} instruction.
     */
    @Identifier(value = "SHUFFLEVECTOR")
    public static final class ShuffleVector extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, ShuffleVector.class};

        private ShuffleVector() {
        }
    }

    /**
     * Represents the LLVM {@code extractvalue} instruction. Sulong supports this instruction only
     * with a single index.
     */
    @Identifier(value = "EXTRACTVALUE")
    public static final class ExtractValue extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, ExtractValue.class};

        private ExtractValue() {
        }
    }

    /**
     * Represents the LLVM {@code insertvalue} instruction. Sulong supports this instruction only
     * with a single index.
     */
    @Identifier(value = "INSERTVALUE")
    public static final class InsertValue extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{LLVMExpression.class, InsertValue.class};

        private InsertValue() {
        }
    }

    /**
     * Represents the root of an LLVM function.
     */
    @Identifier(value = "FUNCTION")
    public static final class Function extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] FUNCTION_TAGS = new Class[]{Function.class, LLVMStatement.class};

        /**
         * The name of the function in the original source code.
         */
        public static final String EXTRA_DATA_SOURCE_NAME = "SOURCE_NAME";

        /**
         * The name of the function in LLVM IR.
         */
        public static final String EXTRA_DATA_LLVM_NAME = "LLVM_NAME";

        private Function() {
        }
    }

    /**
     * Represents an LLVM basic block inside a function. In Sulong, control flow nodes and phis are
     * siblings rather than children of basic blocks in the AST.
     */
    @Identifier(value = "BLOCK")
    public static final class Block extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] BLOCK_TAGS = new Class[]{Block.class, LLVMStatement.class};

        /**
         * The index of the instrumented block in the array of blocks contained in a function.
         */
        public static final String EXTRA_DATA_BLOCK_ID = "BLOCK_ID";

        /**
         * The name of the block, if explicitly assigned. If the block was not explicitly assigned a
         * name, the default value is {@code null}.
         */
        public static final String EXTRA_DATA_BLOCK_NAME = "BLOCK_NAME";

        private Block() {
        }
    }

    /**
     * Represents a function parameter being copied to the stack before the function executes.
     */
    @Identifier(value = "READ_CALL_ARG")
    public static final class ReadCallArg extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] EXPRESSION_TAGS = new Class[]{ReadCallArg.class, LLVMExpression.class};

        /**
         * The index of the argument in the function parameter list.
         */
        public static final String EXTRA_DATA_ARG_INDEX = "ARGUMENT_INDEX";

        private ReadCallArg() {
        }
    }

    /**
     * Represents an intrinsic or builtin function.
     */
    @Identifier(value = "INTRINSIC")
    public static final class Intrinsic extends Tag {

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] VOID_INTRINSIC_TAGS = new Class[]{LLVMStatement.class, Intrinsic.class};

        @SuppressWarnings("unchecked") //
        public static final Class<? extends Tag>[] VALUE_INTRINSIC_TAGS = new Class[]{LLVMExpression.class, Intrinsic.class};

        /**
         * The name of the intrinsic or builtin function.
         */
        public static final String EXTRA_DATA_FUNCTION_NAME = "FUNCTION_NAME";

        /**
         * The type of the intrinsic or builtin function.
         */
        public static final String EXTRA_DATA_FUNCTION_TYPE = "FUNCTION_TYPE";

        private Intrinsic() {
        }
    }

    /**
     * Marks a location where one or more ssa-values are not used anymore and can be removed.
     */
    @Identifier("SSA_LIFETIME_END")
    public static final class SSALifetimeEnd extends Tag {

        /**
         * An array of names of ssa-values whose lifetime ends at this point.
         */
        public static final String EXTRA_DATA_SLOTS = "SLOTS";

        private SSALifetimeEnd() {
        }
    }
}
