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
import com.oracle.truffle.llvm.parser.model.symbols.instructions.DebugTrapInstruction;
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
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMTags;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMInstrumentableNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNodeSourceDescriptor;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import org.graalvm.collections.EconomicMap;

import java.util.ArrayList;
import java.util.List;

import static com.oracle.truffle.llvm.parser.nodes.InstrumentationUtil.createTypedNodeObject;

final class InstrumentingBitcodeInstructionVisitor extends BitcodeInstructionVisitorImpl {

    private Class<? extends Tag>[] tags;
    private EconomicMap<String, Object> nodeObjectEntries;

    InstrumentingBitcodeInstructionVisitor(FrameDescriptor frame, LLVMStack.UniquesRegion uniquesRegion, List<LLVMPhiManager.Phi> blockPhis, int argCount, LLVMSymbolReadResolver symbols,
                    LLVMContext context, LLVMContext.ExternalLibrary library, ArrayList<LLVMLivenessAnalysis.NullerInformation> nullerInfos, List<FrameSlot> notNullable,
                    LLVMRuntimeDebugInformation dbgInfoHandler) {
        super(frame, uniquesRegion, blockPhis, argCount, symbols, context, library, nullerInfos, notNullable, dbgInfoHandler);
        tags = null;
        nodeObjectEntries = null;
    }

    @Override
    public void visit(AllocateInstruction allocate) {
        tags = LLVMTags.Alloca.EXPRESSION_TAGS;

        int alignment = allocate.getAlign();
        if (alignment > 0) {
            alignment = 1 << (allocate.getAlign() - 1);
        }
        nodeObjectEntries = createTypedNodeObject(allocate);
        nodeObjectEntries.put(LLVMTags.Alloca.EXTRA_DATA_ALLOCATION_TYPE, allocate.getPointeeType());
        nodeObjectEntries.put(LLVMTags.Alloca.EXTRA_DATA_ALLOCATION_ALIGNMENT, alignment);

        // create the alloca without inlining the count even if it is constant. a constant count
        // should be inlined anyways, but this way the count is properly reported to instrumentation
        createAlloca(allocate, false);
    }

    @Override
    public void visit(BinaryOperationInstruction operation) {
        tags = InstrumentationUtil.getBinaryOperationTags(operation.getOperator(), false);

        nodeObjectEntries = EconomicMap.create(ArithmeticFlag.ALL_VALUES.length);
        final ArithmeticFlag[] allFlags = ArithmeticFlag.ALL_VALUES;
        for (ArithmeticFlag flag : allFlags) {
            final String key = flag.toString();
            final Object value = LLVMBitcodeTypeHelper.testArithmeticFlag(flag, operation.getFlags(), operation.getOperator());
            nodeObjectEntries.put(key, value);
        }

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
        final String builtinName = declare.getName();
        final FunctionType builtinType = declare.getType();

        // clear the call or invoke tags already set by resolving the IR-parent of this node
        tags = VoidType.INSTANCE.equals(builtinType.getReturnType()) ? LLVMTags.Intrinsic.VOID_INTRINSIC_TAGS : LLVMTags.Intrinsic.VALUE_INTRINSIC_TAGS;
        nodeObjectEntries = EconomicMap.create(2);
        nodeObjectEntries.put(LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_NAME, builtinName);
        nodeObjectEntries.put(LLVMTags.Intrinsic.EXTRA_DATA_FUNCTION_TYPE, builtinType);

        return builtin;
    }

    @Override
    public void visit(CallInstruction call) {
        tags = LLVMTags.Call.VALUE_CALL_TAGS;
        nodeObjectEntries = createTypedNodeObject(call);
        super.visit(call);
    }

    @Override
    public void visit(LandingpadInstruction landingpadInstruction) {
        tags = LLVMTags.LandingPad.STATEMENT_TAGS;
        nodeObjectEntries = createTypedNodeObject(landingpadInstruction);
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
        nodeObjectEntries = createTypedNodeObject(cmpxchg);
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
        nodeObjectEntries = createTypedNodeObject(call);
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
        nodeObjectEntries = createTypedNodeObject(cast);
        nodeObjectEntries.put(LLVMTags.Cast.EXTRA_DATA_SOURCE_TYPE, srcType);
        nodeObjectEntries.put(LLVMTags.Cast.EXTRA_DATA_KIND, castKind);
        super.visit(cast);
    }

    @Override
    public void visit(CompareInstruction compare) {
        nodeObjectEntries = createTypedNodeObject(compare);
        final String cmpKind = compare.getOperator().name();

        if (Type.isFloatingpointType(compare.getLHS().getType())) {
            // TODO fcmp should have fast-math flags
            tags = LLVMTags.FCMP.EXPRESSION_TAGS;
            nodeObjectEntries.put(LLVMTags.FCMP.EXTRA_DATA_KIND, cmpKind);
        } else {
            tags = LLVMTags.ICMP.EXPRESSION_TAGS;
            nodeObjectEntries.put(LLVMTags.ICMP.EXTRA_DATA_KIND, cmpKind);
        }

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
        nodeObjectEntries = createTypedNodeObject(extract);
        super.visit(extract);
    }

    @Override
    public void visit(ExtractValueInstruction extract) {
        tags = LLVMTags.ExtractValue.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(extract);
        super.visit(extract);
    }

    @Override
    public void visit(GetElementPointerInstruction gep) {
        tags = LLVMTags.GetElementPtr.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(gep);
        nodeObjectEntries.put(LLVMTags.GetElementPtr.EXTRA_DATA_SOURCE_TYPE, gep.getBasePointer().getType());
        nodeObjectEntries.put(LLVMTags.GetElementPtr.EXTRA_DATA_IS_INBOUND, gep.isInbounds());
        InstrumentationUtil.addElementPointerIndices(gep.getIndices(), nodeObjectEntries);
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
        nodeObjectEntries = createTypedNodeObject(insert);
        super.visit(insert);
    }

    @Override
    public void visit(InsertValueInstruction insert) {
        tags = LLVMTags.InsertValue.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(insert);
        super.visit(insert);
    }

    @Override
    public void visit(LoadInstruction load) {
        tags = LLVMTags.Load.EXPRESSION_TAGS;
        // TODO alignment
        final int loadByteSize = context.getByteSize(load.getSource().getType());
        nodeObjectEntries = createTypedNodeObject(load);
        nodeObjectEntries.put(LLVMTags.Load.EXTRA_DATA_BYTE_SIZE, loadByteSize);
        super.visit(load);
    }

    @Override
    LLVMStatementNode createAggregatePhi(LLVMExpressionNode[] from, FrameSlot[] to, Type[] types, ArrayList<LLVMPhiManager.Phi> phis) {
        // phis are resolved as part of resolving a control flow node, we must reset the tag and
        // nodeobject after the phis have been resolved
        final Class<? extends Tag>[] oldTags = tags;
        final EconomicMap<String, Object> oldNodeObjectEntries = nodeObjectEntries;

        final LLVMStatementNode phiWrites = super.createAggregatePhi(from, to, types, phis);
        if (phiWrites == null) {
            return null;
        }

        tags = LLVMTags.Phi.EXPRESSION_TAGS;

        final String[] targets = new String[phis.size()];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = LLVMIdentifier.toLocalIdentifier(phis.get(i).getPhiValue().getName());
        }
        nodeObjectEntries = EconomicMap.create(1);
        nodeObjectEntries.put(LLVMTags.Phi.EXTRA_DATA_TARGETS, new LLVMNodeObjectKeys(targets));

        instrument(phiWrites);
        tags = oldTags;
        nodeObjectEntries = oldNodeObjectEntries;

        return phiWrites;
    }

    @Override
    public void visit(PhiInstruction phi) {
        tags = LLVMTags.Phi.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(phi);
        super.visit(phi);
    }

    @Override
    public void visit(ReturnInstruction ret) {
        if (VoidType.INSTANCE.equals(ret.getType())) {
            tags = LLVMTags.Ret.STATEMENT_TAGS;
        } else {
            tags = LLVMTags.Ret.EXPRESSION_TAGS;
        }
        nodeObjectEntries = createTypedNodeObject(ret);
        super.visit(ret);
    }

    @Override
    public void visit(SelectInstruction select) {
        tags = LLVMTags.Select.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(select);
        super.visit(select);
    }

    @Override
    public void visit(ShuffleVectorInstruction shuffle) {
        tags = LLVMTags.ShuffleVector.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(shuffle);
        super.visit(shuffle);
    }

    @Override
    public void visit(StoreInstruction store) {
        tags = LLVMTags.Store.STATEMENT_TAGS;
        // TODO alignment
        final int storeByteSize = context.getByteSize(store.getDestination().getType());
        nodeObjectEntries = EconomicMap.create(1);
        nodeObjectEntries.put(LLVMTags.Store.EXTRA_DATA_BYTE_SIZE, storeByteSize);
        super.visit(store);
    }

    @Override
    public void visit(ReadModifyWriteInstruction rmw) {
        tags = LLVMTags.AtomicRMW.EXPRESSION_TAGS;
        nodeObjectEntries = createTypedNodeObject(rmw);
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

    @SuppressWarnings("unchecked") //
    private static final Class<? extends Tag>[] EMPTY_TAGS = new Class[0];

    @Override
    public void visit(DebugTrapInstruction inst) {
        tags = EMPTY_TAGS;
        super.visit(inst);
    }

    @Override
    void createFrameWrite(LLVMExpressionNode result, ValueInstruction source, LLVMSourceLocation sourceLocation) {
        // instrument the source of the write
        instrument(result);

        // super.createFrameWrite will call addInstruction, prepare the tags for then
        tags = LLVMTags.SSAWrite.EXPRESSION_TAGS;
        nodeObjectEntries = InstrumentationUtil.createSSAAccessDescriptor(source, LLVMTags.SSAWrite.EXTRA_DATA_SSA_TARGET);
        super.createFrameWrite(result, source, sourceLocation);
    }

    @Override
    void addInstruction(LLVMStatementNode node) {
        instrument(node);
        super.addInstruction(node);
        tags = null;
        nodeObjectEntries = null;
    }

    @Override
    public void addInstructionUnchecked(LLVMStatementNode instruction) {
        // this is only ever used for Sulong internal nodes
        super.addInstructionUnchecked(instruction);
        tags = null;
        nodeObjectEntries = null;
    }

    @Override
    void setControlFlowNode(LLVMControlFlowNode controlFlowNode) {
        InstrumentationUtil.addTags(controlFlowNode, tags, nodeObjectEntries);
        super.setControlFlowNode(controlFlowNode);
        tags = null;
        nodeObjectEntries = null;
    }

    private void instrument(LLVMInstrumentableNode node) {
        if (tags == null) {
            throw new LLVMParserException("Failed to instrument node");
        }

        final LLVMNodeSourceDescriptor sourceDescriptor = node.getOrCreateSourceDescriptor();
        sourceDescriptor.setNodeObjectEntries(nodeObjectEntries);

        assert sourceDescriptor.getTags() == null : "Unexpected tags";
        sourceDescriptor.setTags(tags);
    }
}
