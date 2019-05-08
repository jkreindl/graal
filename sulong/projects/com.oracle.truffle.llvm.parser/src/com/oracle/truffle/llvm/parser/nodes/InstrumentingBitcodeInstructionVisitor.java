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
package com.oracle.truffle.llvm.parser.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.llvm.parser.LLVMLivenessAnalysis;
import com.oracle.truffle.llvm.parser.LLVMPhiManager;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.AllocateInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.BinaryOperationInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.BranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CallInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CastInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CompareExchangeInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.CompareInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ConditionalBranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ExtractElementInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ExtractValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.FenceInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.GetElementPointerInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.IndirectBranchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InsertElementInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InsertValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.InvokeInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LandingpadInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.LoadInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.PhiInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ReadModifyWriteInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ResumeInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ReturnInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SelectInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ShuffleVectorInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.StoreInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SwitchInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.SwitchOldInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.UnreachableInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.ValueInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.VoidCallInstruction;
import com.oracle.truffle.llvm.parser.model.symbols.instructions.VoidInvokeInstruction;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.nodes.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.types.Type;

import java.util.ArrayList;
import java.util.List;

final class InstrumentingBitcodeInstructionVisitor extends BitcodeInstructionVisitorImpl {

    private Class<? extends Tag>[] tags;

    InstrumentingBitcodeInstructionVisitor(FrameDescriptor frame, LLVMStack.UniquesRegion uniquesRegion, List<LLVMPhiManager.Phi> blockPhis, int argCount, LLVMSymbolReadResolver symbols,
                    LLVMContext context, LLVMContext.ExternalLibrary library, ArrayList<LLVMLivenessAnalysis.NullerInformation> nullerInfos, List<FrameSlot> notNullable,
                    LLVMRuntimeDebugInformation dbgInfoHandler) {
        super(frame, uniquesRegion, blockPhis, argCount, symbols, context, library, nullerInfos, notNullable, dbgInfoHandler);
    }

    @Override
    public void visit(AllocateInstruction allocate) {
        tags = LLVMTags.Alloca.SINGLE_EXPRESSION_TAG;
        super.visit(allocate);
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        switch (operation.getOperator()) {
            case FP_ADD:
            case INT_ADD:
                tags = LLVMTags.Add.SINGLE_EXPRESSION_TAG;
                break;
            case FP_SUBTRACT:
            case INT_SUBTRACT:
                tags = LLVMTags.Sub.SINGLE_EXPRESSION_TAG;
                break;
            case FP_MULTIPLY:
            case INT_MULTIPLY:
                tags = LLVMTags.Mul.SINGLE_EXPRESSION_TAG;
                break;
            case FP_DIVIDE:
            case INT_UNSIGNED_DIVIDE:
            case INT_SIGNED_DIVIDE:
                tags = LLVMTags.Div.SINGLE_EXPRESSION_TAG;
                break;
            case FP_REMAINDER:
            case INT_UNSIGNED_REMAINDER:
            case INT_SIGNED_REMAINDER:
                tags = LLVMTags.Rem.SINGLE_EXPRESSION_TAG;
                break;
            case INT_SHIFT_LEFT:
                tags = LLVMTags.ShiftLeft.SINGLE_EXPRESSION_TAG;
                break;
            case INT_LOGICAL_SHIFT_RIGHT:
            case INT_ARITHMETIC_SHIFT_RIGHT:
                tags = LLVMTags.ShiftRight.SINGLE_EXPRESSION_TAG;
                break;
            case INT_AND:
                tags = LLVMTags.And.SINGLE_EXPRESSION_TAG;
                break;
            case INT_OR:
                tags = LLVMTags.Or.SINGLE_EXPRESSION_TAG;
                break;
            case INT_XOR:
                tags = LLVMTags.XOr.SINGLE_EXPRESSION_TAG;
                break;
            default:
                tags = null;
                break;
        }
        super.visit(operation);
    }

    @Override
    public void visit(BranchInstruction branch) {
        tags = LLVMTags.Br.SINGLE_EXPRESSION_TAG;
        super.visit(branch);
    }

    @Override
    public void visit(CallInstruction call) {
        tags = LLVMTags.Call.SINGLE_EXPRESSION_TAG;
        super.visit(call);
    }

    @Override
    public void visit(LandingpadInstruction landingpadInstruction) {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    @Override
    public void visit(ResumeInstruction resumeInstruction) {
        tags = LLVMTags.Resume.SINGLE_EXPRESSION_TAG;
        super.visit(resumeInstruction);
    }

    @Override
    public void visit(CompareExchangeInstruction cmpxchg) {
        tags = LLVMTags.CmpXchg.SINGLE_EXPRESSION_TAG;
        super.visit(cmpxchg);
    }

    @Override
    public void visit(VoidCallInstruction call) {
        tags = LLVMTags.Call.SINGLE_EXPRESSION_TAG;
        super.visit(call);
    }

    @Override
    public void visit(InvokeInstruction call) {
        tags = LLVMTags.Invoke.SINGLE_EXPRESSION_TAG;
        super.visit(call);
    }

    @Override
    public void visit(VoidInvokeInstruction call) {
        tags = LLVMTags.Invoke.SINGLE_EXPRESSION_TAG;
        super.visit(call);
    }

    @Override
    public void visit(CastInstruction cast) {
        tags = LLVMTags.Cast.SINGLE_EXPRESSION_TAG;
        super.visit(cast);
    }

    @Override
    public void visit(CompareInstruction compare) {
        if (Type.isFloatingpointType(compare.getLHS().getType())) {
            tags = LLVMTags.FCMP.SINGLE_EXPRESSION_TAG;
        } else {
            tags = LLVMTags.ICMP.SINGLE_EXPRESSION_TAG;
        }
        super.visit(compare);
    }

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        tags = LLVMTags.Br.SINGLE_EXPRESSION_TAG;
        super.visit(branch);
    }

    @Override
    public void visit(ExtractElementInstruction extract) {
        tags = LLVMTags.ExtractElement.SINGLE_EXPRESSION_TAG;
        super.visit(extract);
    }

    @Override
    public void visit(ExtractValueInstruction extract) {
        tags = LLVMTags.ExtractValue.SINGLE_EXPRESSION_TAG;
        super.visit(extract);
    }

    @Override
    public void visit(GetElementPointerInstruction gep) {
        tags = LLVMTags.GetElementPtr.SINGLE_EXPRESSION_TAG;
        super.visit(gep);
    }

    @Override
    public void visit(IndirectBranchInstruction branch) {
        tags = LLVMTags.IndirectBr.SINGLE_EXPRESSION_TAG;
        super.visit(branch);
    }

    @Override
    public void visit(InsertElementInstruction insert) {
        tags = LLVMTags.InsertElement.SINGLE_EXPRESSION_TAG;
        super.visit(insert);
    }

    @Override
    public void visit(InsertValueInstruction insert) {
        tags = LLVMTags.InsertValue.SINGLE_EXPRESSION_TAG;
        super.visit(insert);
    }

    @Override
    public void visit(LoadInstruction load) {
        tags = LLVMTags.Load.SINGLE_EXPRESSION_TAG;
        super.visit(load);
    }

    @Override
    public void visit(PhiInstruction phi) {
        tags = LLVMTags.Phi.SINGLE_EXPRESSION_TAG;
        super.visit(phi);
    }

    @Override
    public void visit(ReturnInstruction ret) {
        tags = LLVMTags.Ret.SINGLE_EXPRESSION_TAG;
        super.visit(ret);
    }

    @Override
    public void visit(SelectInstruction select) {
        tags = LLVMTags.Select.SINGLE_EXPRESSION_TAG;
        super.visit(select);
    }

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        tags = LLVMTags.ShuffleVector.SINGLE_EXPRESSION_TAG;
        super.visit(shuffle);
    }

    @Override
    public void visit(StoreInstruction store) {
        tags = LLVMTags.Store.SINGLE_EXPRESSION_TAG;
        super.visit(store);
    }

    @Override
    public void visit(ReadModifyWriteInstruction rmw) {
        tags = LLVMTags.AtomicRMW.SINGLE_EXPRESSION_TAG;
        super.visit(rmw);
    }

    @Override
    public void visit(FenceInstruction fence) {
        tags = LLVMTags.Fence.SINGLE_EXPRESSION_TAG;
        super.visit(fence);
    }

    @Override
    public void visit(SwitchInstruction zwitch) {
        tags = LLVMTags.Switch.SINGLE_EXPRESSION_TAG;
        super.visit(zwitch);
    }

    @Override
    public void visit(SwitchOldInstruction zwitch) {
        tags = LLVMTags.Switch.SINGLE_EXPRESSION_TAG;
        super.visit(zwitch);
    }

    @Override
    public void visit(UnreachableInstruction ui) {
        tags = LLVMTags.Unreachable.SINGLE_EXPRESSION_TAG;
        super.visit(ui);
    }

    @Override
    void createFrameWrite(LLVMExpressionNode result, ValueInstruction source, LLVMSourceLocation sourceLocation) {
        ensureTagsValid();
        // instrument the source of the write
        final LLVMExpressionNode node = nodeFactory.createInstrumentableExpression(result, tags);

        // super.createFrameWrite will call addInstruction, prepare the tags for then
        tags = LLVMTags.SSAWrite.SINGLE_EXPRESSION_TAG;
        super.createFrameWrite(node, source, sourceLocation);
    }

    @Override
    void addInstruction(LLVMStatementNode node) {
        ensureTagsValid();
        final LLVMStatementNode instrumentedNode = nodeFactory.createInstrumentableStatement(node, tags);
        super.addInstruction(instrumentedNode);
        tags = null;
    }

    @Override
    public void addInstructionUnchecked(LLVMStatementNode instruction) {
        // this is only ever used for Sulong internal nodes
        super.addInstructionUnchecked(instruction);
        tags = null;
    }

    @Override
    void setControlFlowNode(LLVMControlFlowNode controlFlowNode) {
        super.setControlFlowNode(controlFlowNode);
        tags = null;
    }

    private void ensureTagsValid() {
        if (tags == null) {
            throw new LLVMParserException("Failed to instrument node");
        }
    }
}
