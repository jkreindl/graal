/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.parser.bitcode.blocks;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.metadata.debuginfo.DebugInfoModuleProcessor;
import com.oracle.truffle.llvm.parser.model.IRScope;
import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalValueSymbol;
import com.oracle.truffle.llvm.parser.text.LLSourceBuilder;
import com.oracle.truffle.llvm.parser.util.SymbolNameMangling;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;

import java.util.List;

final class BCImplicitRootBlock extends BCBlockParser {

    private final ModelModule module;
    private final StringTable stringTable;
    private final IRScope scope;
    private final LLSourceBuilder llSource;
    private final LLVMContext context;

    BCImplicitRootBlock(ModelModule module, Source bcSource, LLVMContext context) {
        this.module = module;
        this.context = context;
        this.stringTable = new StringTable();
        this.scope = new IRScope();
        this.llSource = LLSourceBuilder.create(bcSource);
    }

    @Override
    void parseRecord(LLVMBitcodeRecord record) {
        throw new IllegalStateException("Record outside of top-level block");
    }

    @Override
    void onExit() {
        int globalIndex = setMissingNames(module.getGlobalVariables(), 0);
        setMissingNames(module.getAliases(), globalIndex);
        SymbolNameMangling.demangleGlobals(module);
        DebugInfoModuleProcessor.processModule(module, scope.getMetadata(), context);
    }

    @Override
    BCBlockParser getParserForSubblock(BCBlockScanner.ScannerData scannerData, int blockId) {
        switch (blockId) {
            case BCModuleBlock.BLOCK_ID:
                return new BCModuleBlock(module, stringTable, scope, llSource);

            case BCStrTabBlock.BLOCK_ID:
                return new BCStrTabBlock(stringTable);

            default:
                return super.getParserForSubblock(scannerData, blockId);
        }
    }

    private static int setMissingNames(List<? extends GlobalValueSymbol> globals, int startIndex) {
        int globalIndex = startIndex;
        for (GlobalValueSymbol variable : globals) {
            if (LLVMIdentifier.isUnknown(variable.getName())) {
                variable.setName(String.valueOf(globalIndex++));
            }
        }
        return globalIndex;
    }
}
