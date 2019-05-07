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
package com.oracle.truffle.llvm.parser.nodes;

import java.util.List;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.llvm.parser.LLVMParserRuntime;
import com.oracle.truffle.llvm.parser.model.SymbolImpl;
import com.oracle.truffle.llvm.parser.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.runtime.GetStackSpaceFactory;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.NodeFactory;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;

public abstract class LLVMSymbolReadResolver {

    final LLVMParserRuntime runtime;
    final LLVMContext context;
    final NodeFactory nodeFactory;

    protected LLVMSymbolReadResolver(LLVMParserRuntime runtime) {
        this.runtime = runtime;
        this.context = runtime.getContext();
        this.nodeFactory = context.getNodeFactory();
    }

    public static LLVMSymbolReadResolver create(LLVMParserRuntime runtime, FrameDescriptor frame, GetStackSpaceFactory getStackSpaceFactory) {
        LLVMSymbolReadResolver impl = new ReadResolverImpl(runtime, frame, getStackSpaceFactory);

        if (runtime.getContext().getEnv().getOptions().get(SulongEngineOption.INSTRUMENT_IR)) {
            impl = new ReadResolverInstrumentationWrapper(runtime, impl);
        }

        return impl;
    }

    public abstract LLVMExpressionNode resolve(SymbolImpl symbol);

    public static Integer evaluateIntegerConstant(SymbolImpl constant) {
        if (constant instanceof IntegerConstant) {
            assert ((IntegerConstant) constant).getValue() == (int) ((IntegerConstant) constant).getValue();
            return (int) ((IntegerConstant) constant).getValue();
        } else if (constant instanceof BigIntegerConstant) {
            return ((BigIntegerConstant) constant).getValue().intValueExact();
        } else if (constant instanceof NullConstant) {
            return 0;
        } else {
            return null;
        }
    }

    public static Long evaluateLongIntegerConstant(SymbolImpl constant) {
        if (constant instanceof IntegerConstant) {
            return ((IntegerConstant) constant).getValue();
        } else if (constant instanceof BigIntegerConstant) {
            return ((BigIntegerConstant) constant).getValue().longValueExact();
        } else if (constant instanceof NullConstant) {
            return 0L;
        } else {
            return null;
        }
    }

    public LLVMExpressionNode resolveElementPointer(SymbolImpl base, List<SymbolImpl> indices) {
        LLVMExpressionNode currentAddress = resolve(base);
        Type currentType = base.getType();

        for (int i = 0, indicesSize = indices.size(); i < indicesSize; i++) {
            final SymbolImpl indexSymbol = indices.get(i);
            final Type indexType = indexSymbol.getType();

            final Long indexInteger = evaluateLongIntegerConstant(indexSymbol);
            if (indexInteger == null) {
                // the index is determined at runtime
                if (currentType instanceof StructureType) {
                    // according to http://llvm.org/docs/LangRef.html#getelementptr-instruction
                    throw new LLVMParserException("Indices on structs must be constant integers!");
                }
                AggregateType aggregate = (AggregateType) currentType;
                final long indexedTypeLength = context.getIndexOffset(1, aggregate);
                currentType = aggregate.getElementType(1);
                final LLVMExpressionNode indexNode = resolve(indexSymbol);
                currentAddress = nodeFactory.createTypedElementPointer(currentAddress, indexNode, indexedTypeLength, currentType);
            } else {
                // the index is a constant integer
                AggregateType aggregate = (AggregateType) currentType;
                final long addressOffset = context.getIndexOffset(indexInteger, aggregate);
                currentType = aggregate.getElementType(indexInteger);

                // creating a pointer inserts type information, this needs to happen for the address
                // computed by getelementptr even if it is the same as the basepointer
                if (addressOffset != 0 || i == indicesSize - 1) {
                    final LLVMExpressionNode indexNode;
                    if (indexType == PrimitiveType.I32) {
                        indexNode = nodeFactory.createLiteral(1, PrimitiveType.I32);
                    } else if (indexType == PrimitiveType.I64) {
                        indexNode = nodeFactory.createLiteral(1L, PrimitiveType.I64);
                    } else {
                        throw new AssertionError(indexType);
                    }
                    currentAddress = nodeFactory.createTypedElementPointer(currentAddress, indexNode, addressOffset, currentType);
                }
            }
        }

        return currentAddress;
    }

}
