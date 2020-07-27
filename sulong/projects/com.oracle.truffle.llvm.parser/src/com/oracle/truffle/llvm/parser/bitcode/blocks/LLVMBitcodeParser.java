/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
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
import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.Magic;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import org.graalvm.polyglot.io.ByteSequence;

public final class LLVMBitcodeParser {

    /* Each bitcode file starts with a magic word. The actual bitcode data comes only after that. */
    private static final int BC_DATA_OFFSET = Integer.SIZE;

    public static void parseBitcode(ByteSequence bitcode, ModelModule model, Source bcSource, LLVMContext context) {
        final BitStream bitstream = BitStream.create(bitcode);

        final long actualMagicWord = bitstream.read(0L, BC_DATA_OFFSET);
        if (actualMagicWord != Magic.BC_MAGIC_WORD.magic) {
            throw new LLVMParserException("Not a valid Bitcode File!");
        }

        final LLVMBitcodeReader bitcodeReader = new LLVMBitcodeReader(bitstream);
        final BCImplicitRootBlock rootBlock = new BCImplicitRootBlock(model, bcSource, context);
        final BCBlockScanner.ScannerData rootScannerData = BCBlockScanner.createScannerData(bitcodeReader, BC_DATA_OFFSET);

        BCBlockScanner.scanBlock(rootScannerData, -1L, rootBlock);
    }
}
