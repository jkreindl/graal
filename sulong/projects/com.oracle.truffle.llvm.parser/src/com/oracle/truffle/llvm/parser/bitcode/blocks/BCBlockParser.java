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

/**
 * Base class for parsers for the individual blocks that may occur in an LLVM bitcode file.
 */
abstract class BCBlockParser {

    BCBlockParser() {
    }

    /**
     * Callback that is invoked once before the records and subblocks of this block
     * {@link BCBlockScanner#scanBlock} () block are parsed}.
     */
    void onEnter() {
    }

    /**
     * Callback that is invoked for each record of the parsed block.
     *
     * @param record the scanned record
     */
    abstract void parseRecord(LLVMBitcodeRecord record);

    /**
     * Returns a parser for a specific subblock of the parsed block. As each subblock is parsed
     * using a separate block parser, the returned block parser may contain contextual data such as
     * current scope information. Implementations of this method may return null for blocks they
     * wish to skip rather than parse.
     *
     * @param blockId id of the subblock to parse
     * @return a parser for the specific subblock
     */
    BCBlockParser getParserForSubblock(@SuppressWarnings("unused") BCBlockScanner.ScannerData scannerData, @SuppressWarnings("unused") int blockId) {
        return null;
    }

    /**
     * Callback that is invoked once after all records and subblocks of this block
     * {@link BCBlockScanner#scanBlock block have been parsed}.
     */
    void onExit() {
    }
}
