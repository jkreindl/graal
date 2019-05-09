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
import com.oracle.truffle.llvm.parser.model.SymbolImpl;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
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
import com.oracle.truffle.llvm.parser.util.LLVMBitcodeTypeHelper;
import com.oracle.truffle.llvm.runtime.ArithmeticFlag;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObjectKeys;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObject;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;

import java.util.ArrayList;
import java.util.List;

import static com.oracle.truffle.llvm.parser.nodes.InstrumentationUtil.createTypedNodeObject;

final class InstrumentingBitcodeInstructionVisitor extends BitcodeInstructionVisitorImpl {

    private Class<? extends Tag>[] tags;
    private LLVMNodeObject nodeObject;

    InstrumentingBitcodeInstructionVisitor(FrameDescriptor frame, LLVMStack.UniquesRegion uniquesRegion, List<LLVMPhiManager.Phi> blockPhis, int argCount, LLVMSymbolReadResolver symbols,
                    LLVMContext context, LLVMContext.ExternalLibrary library, ArrayList<LLVMLivenessAnalysis.NullerInformation> nullerInfos, List<FrameSlot> notNullable,
                    LLVMRuntimeDebugInformation dbgInfoHandler) {
        super(frame, uniquesRegion, blockPhis, argCount, symbols, context, library, nullerInfos, notNullable, dbgInfoHandler);
        tags = null;
        nodeObject = null;
    }

    @Override
    public void visit(AllocateInstruction allocate) {
        tags = LLVMTags.Alloca.EXPRESSION_TAGS;

        int alignment = allocate.getAlign();
        if (alignment > 0) {
            alignment = 1 << (allocate.getAlign() - 1);
        }
        nodeObject = createTypedNodeObject(allocate).option(LLVMTags.Alloca.EXTRA_DATA_ALLOCATION_TYPE, allocate.getPointeeType()).option(LLVMTags.Alloca.EXTRA_DATA_ALLOCATION_ALIGNMENT,
                        alignment).build();

        // create the alloca without inlining the count even if it is constant. a constant count
        // should be inlined anyways, but this way the count is properly reported to instrumentation
        createAlloca(allocate, false);
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        tags = InstrumentationUtil.getBinaryOperationTags(operation.getOperator(), false);

        final ArithmeticFlag[] allFlags = ArithmeticFlag.ALL_VALUES;
        final String[] keys = new String[allFlags.length];
        final Object[] values = new Object[allFlags.length];
        for (int i = 0; i < allFlags.length; i++) {
            keys[i] = allFlags[i].toString();
            values[i] = LLVMBitcodeTypeHelper.testArithmeticFlag(allFlags[i], operation.getFlags(), operation.getOperator());
        }
        nodeObject = new LLVMNodeObject(keys, values);

        super.visit(operation);
    }

    @Override
    public void visit(BranchInstruction branch) {
        tags = LLVMTags.Br.STATEMENT_TAGS;
        super.visit(branch);
    }

    @Override
    LLVMExpressionNode tryGenerateBuiltinNode(SymbolImpl target, LLVMExpressionNode[] argNodes, LLVMSourceLocation source) {
        final LLVMExpressionNode builtin = super.tryGenerateBuiltinNode(target, argNodes, source);
        if (builtin == null) {
            // not a builtin
            return builtin;
        }

        // only function declarations are resolved against the list of available builtins
        final FunctionDeclaration declare = (FunctionDeclaration) target;
        final String builtinName = LLVMIdentifier.toGlobalIdentifier(declare.getName());
        final FunctionType builtinType = declare.getType();

        // clear the call or invoke tags already set by resolving the IR-parent of this node
        tags = VoidType.INSTANCE.equals(builtinType.getReturnType()) ? LLVMTags.Intrinsic.VOID_INTRINSIC_TAGS : LLVMTags.Intrinsic.VALUE_INTRINSIC_TAGS;
        nodeObject = LLVMNodeObject.newBuilder().option(LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_NAME, builtinName).option(LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_TYPE, builtinType).build();

        return builtin;
    }

    @Override
    public void visit(CallInstruction call) {
        tags = LLVMTags.Call.VALUE_CALL_TAGS;
        nodeObject = createTypedNodeObject(call).build();
        super.visit(call);
    }

    @Override
    public void visit(LandingpadInstruction landingpadInstruction) {
        tags = LLVMTags.LandingPad.STATEMENT_TAGS;
        nodeObject = createTypedNodeObject(landingpadInstruction).build();
        super.visit(landingpadInstruction);
    }

    @Override
    public void visit(ResumeInstruction resumeInstruction) {
        tags = LLVMTags.Resume.STATEMENT_TAGS;
        super.visit(resumeInstruction);
    }

    @Override
    public void visit(CompareExchangeInstruction cmpxchg) {
        tags = LLVMTags.CmpXchg.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(cmpxchg).build();
        super.visit(cmpxchg);
    }

    @Override
    public void visit(VoidCallInstruction call) {
        tags = LLVMTags.Call.VOID_CALL_TAGS;
        super.visit(call);
    }

    @Override
    public void visit(InvokeInstruction call) {
        tags = LLVMTags.Invoke.VALUE_INVOKE_TAGS;
        nodeObject = createTypedNodeObject(call).build();
        super.visit(call);
    }

    @Override
    public void visit(VoidInvokeInstruction call) {
        tags = LLVMTags.Invoke.VOID_INVOKE_TAGS;
        super.visit(call);
    }

    @Override
    public void visit(CastInstruction cast) {
        tags = LLVMTags.Cast.EXPRESSION_TAGS;
        final Type srcType = cast.getValue().getType();
        final String castKind = cast.getOperator().getIrString();
        nodeObject = createTypedNodeObject(cast).option(LLVMTags.Cast.EXTRA_DATA_SOURCE_TYPE, srcType).option(LLVMTags.Cast.EXTRA_DATA_KIND, castKind).build();
        super.visit(cast);
    }

    @Override
    public void visit(CompareInstruction compare) {
        final LLVMNodeObject.Builder noBuilder = createTypedNodeObject(compare);
        final String cmpKind = compare.getOperator().name();

        if (Type.isFloatingpointType(compare.getLHS().getType())) {
            // TODO fcmp should have fast-math flags
            tags = LLVMTags.FCMP.EXPRESSION_TAGS;
            noBuilder.option(LLVMTags.FCMP.EXTRA_DATA_KIND, cmpKind);
        } else {
            tags = LLVMTags.ICMP.EXPRESSION_TAGS;
            noBuilder.option(LLVMTags.ICMP.EXTRA_DATA_KIND, cmpKind);
        }

        nodeObject = noBuilder.build();
        super.visit(compare);
    }

    @Override
    public void visit(ConditionalBranchInstruction branch) {
        tags = LLVMTags.Br.STATEMENT_TAGS;
        super.visit(branch);
    }

    @Override
    public void visit(ExtractElementInstruction extract) {
        tags = LLVMTags.ExtractElement.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(extract).build();
        super.visit(extract);
    }

    @Override
    public void visit(ExtractValueInstruction extract) {
        tags = LLVMTags.ExtractValue.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(extract).build();
        super.visit(extract);
    }

    @Override
    public void visit(GetElementPointerInstruction gep) {
        tags = LLVMTags.GetElementPtr.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(gep).option(LLVMTags.GetElementPtr.EXTRA_DATA_SOURCE_TYPE, gep.getBasePointer().getType()).option(LLVMTags.GetElementPtr.EXTRA_DATA_IS_INBOUND,
                        gep.isInbounds()).build();
        super.visit(gep);
    }

    @Override
    public void visit(IndirectBranchInstruction branch) {
        tags = LLVMTags.IndirectBr.STATEMENT_TAGS;
        super.visit(branch);
    }

    @Override
    public void visit(InsertElementInstruction insert) {
        tags = LLVMTags.InsertElement.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(insert).build();
        super.visit(insert);
    }

    @Override
    public void visit(InsertValueInstruction insert) {
        tags = LLVMTags.InsertValue.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(insert).build();
        super.visit(insert);
    }

    @Override
    public void visit(LoadInstruction load) {
        tags = LLVMTags.Load.EXPRESSION_TAGS;
        // TODO alignment
        final int loadByteSize = context.getByteSize(load.getSource().getType());
        nodeObject = createTypedNodeObject(load).option(LLVMTags.Load.EXTRA_DATA_BYTE_SIZE, loadByteSize).build();
        super.visit(load);
    }

    @Override
    LLVMStatementNode createAggregatePhi(LLVMExpressionNode[] from, FrameSlot[] to, Type[] types, ArrayList<LLVMPhiManager.Phi> phis) {
        final LLVMStatementNode phiWrites = super.createAggregatePhi(from, to, types, phis);
        if (phiWrites == null) {
            return null;
        }

        tags = LLVMTags.Phi.EXPRESSION_TAGS;

        final String[] targets = new String[phis.size()];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = LLVMIdentifier.toLocalIdentifier(phis.get(i).getPhiValue().getName());
        }
        nodeObject = LLVMNodeObject.newBuilder().option(LLVMTags.Phi.EXTRA_DATA_TARGETS, new LLVMNodeObjectKeys(targets)).build();

        final LLVMStatementNode instrumentablePhiWrites = nodeFactory.createInstrumentableStatement(phiWrites, tags, nodeObject);

        tags = null;
        nodeObject = null;

        return instrumentablePhiWrites;
    }

    @Override
    public void visit(PhiInstruction phi) {
        tags = LLVMTags.Phi.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(phi).build();
        super.visit(phi);
    }

    @Override
    public void visit(ReturnInstruction ret) {
        if (VoidType.INSTANCE.equals(ret.getType())) {
            tags = LLVMTags.Ret.STATEMENT_TAGS;
        } else {
            tags = LLVMTags.Ret.EXPRESSION_TAGS;
        }
        nodeObject = createTypedNodeObject(ret).build();
        super.visit(ret);
    }

    @Override
    public void visit(SelectInstruction select) {
        tags = LLVMTags.Select.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(select).build();
        super.visit(select);
    }

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        tags = LLVMTags.ShuffleVector.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(shuffle).build();
        super.visit(shuffle);
    }

    @Override
    public void visit(StoreInstruction store) {
        tags = LLVMTags.Store.STATEMENT_TAGS;
        // TODO alignment
        final int storeByteSize = context.getByteSize(store.getDestination().getType());
        nodeObject = LLVMNodeObject.newBuilder().option(LLVMTags.Store.EXTRA_DATA_BYTE_SIZE, storeByteSize).build();
        super.visit(store);
    }

    @Override
    public void visit(ReadModifyWriteInstruction rmw) {
        tags = LLVMTags.AtomicRMW.EXPRESSION_TAGS;
        nodeObject = createTypedNodeObject(rmw).build();
        super.visit(rmw);
    }

    @Override
    public void visit(FenceInstruction fence) {
        tags = LLVMTags.Fence.STATEMENT_TAGS;
        super.visit(fence);
    }

    @Override
    public void visit(SwitchInstruction zwitch) {
        tags = LLVMTags.Switch.STATEMENT_TAGS;
        super.visit(zwitch);
    }

    @Override
    public void visit(SwitchOldInstruction zwitch) {
        tags = LLVMTags.Switch.STATEMENT_TAGS;
        super.visit(zwitch);
    }

    @Override
    public void visit(UnreachableInstruction ui) {
        tags = LLVMTags.Unreachable.STATEMENT_TAGS;
        super.visit(ui);
    }

    @Override
    void createFrameWrite(LLVMExpressionNode result, ValueInstruction source, LLVMSourceLocation sourceLocation) {
        ensureTagsValid();
        // instrument the source of the write
        final LLVMExpressionNode node = nodeFactory.createInstrumentableExpression(result, tags, nodeObject);

        // super.createFrameWrite will call addInstruction, prepare the tags for then
        tags = LLVMTags.SSAWrite.EXPRESSION_TAGS;
        nodeObject = InstrumentationUtil.createSSAAccessDescriptor(source, LLVMTags.SSAWrite.EXTRA_DATA_SSA_TARGET);
        super.createFrameWrite(node, source, sourceLocation);
    }

    @Override
    void addInstruction(LLVMStatementNode node) {
        ensureTagsValid();
        final LLVMStatementNode instrumentedNode = nodeFactory.createInstrumentableStatement(node, tags, nodeObject);
        super.addInstruction(instrumentedNode);
        tags = null;
        nodeObject = null;
    }

    @Override
    public void addInstructionUnchecked(LLVMStatementNode instruction) {
        // this is only ever used for Sulong internal nodes
        super.addInstructionUnchecked(instruction);
        tags = null;
        nodeObject = null;
    }

    @Override
    void setControlFlowNode(LLVMControlFlowNode controlFlowNode) {
        nodeFactory.instrumentControlFlow(controlFlowNode, tags, nodeObject);
        super.setControlFlowNode(controlFlowNode);
        tags = null;
        nodeObject = null;
    }

    private void ensureTagsValid() {
        if (tags == null) {
            throw new LLVMParserException("Failed to instrument node");
        }
    }
}
