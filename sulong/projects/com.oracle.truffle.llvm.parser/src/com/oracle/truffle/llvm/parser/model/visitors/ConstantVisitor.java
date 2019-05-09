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
package com.oracle.truffle.llvm.parser.model.visitors;

import com.oracle.truffle.llvm.parser.model.symbols.constants.BinaryOperationConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.BlockAddressConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CastConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.CompareConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.GetElementPointerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.InlineAsmConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.NullConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.SelectConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.StringConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.UndefinedConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.ArrayConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.StructureConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.aggregate.VectorConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.DoubleConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.FloatConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.floatingpoint.X86FP80Constant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.BigIntegerConstant;
import com.oracle.truffle.llvm.parser.model.symbols.constants.integer.IntegerConstant;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public interface ConstantVisitor extends SymbolVisitor {

    void visitConstant(Symbol constant);

    @Override
    default void visit(ArrayConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(StructureConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(VectorConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(BigIntegerConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(BinaryOperationConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(BlockAddressConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(CastConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(CompareConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(DoubleConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(FloatConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(X86FP80Constant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(GetElementPointerConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(InlineAsmConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(IntegerConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(NullConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(StringConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(UndefinedConstant constant) {
        visitConstant(constant);
    }

    @Override
    default void visit(SelectConstant constant) {
        visitConstant(constant);
    }
}
