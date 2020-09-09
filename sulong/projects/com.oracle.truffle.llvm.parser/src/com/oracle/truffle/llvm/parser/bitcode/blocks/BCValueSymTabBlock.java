/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates.
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

import com.oracle.truffle.llvm.parser.model.IRScope;

final class BCValueSymTabBlock extends BCBlockParser {

    static final int BLOCK_ID = 14;

    private static final int VALUE_SYMTAB_ENTRY = 1;
    private static final int VALUE_SYMTAB_BASIC_BLOCK_ENTRY = 2;
    private static final int VALUE_SYMTAB_FUNCTION_ENTRY = 3;
    // private static final int VALUE_SYMTAB_COMBINED_FNENTRY = 4;

    private final IRScope container;

    BCValueSymTabBlock(IRScope container) {
        this.container = container;
        assert container != null;
    }

    @Override
    void parseRecord(LLVMBitcodeRecord record) {
        int index = record.readInt();
        switch (record.getId()) {
            case VALUE_SYMTAB_ENTRY:
                container.nameSymbol(index, record.readString());
                break;

            case VALUE_SYMTAB_BASIC_BLOCK_ENTRY:
                container.nameBlock(index, record.readString());
                break;

            case VALUE_SYMTAB_FUNCTION_ENTRY:
                record.skip(); // ignored
                container.nameSymbol(index, record.readString());
                break;

            default:
                break;
        }
    }
}