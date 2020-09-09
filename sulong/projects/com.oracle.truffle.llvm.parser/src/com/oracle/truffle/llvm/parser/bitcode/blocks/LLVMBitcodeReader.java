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
 * Token reader based on an LLVM bitstream.
 */
final class LLVMBitcodeReader {

    private static final String CHAR6 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._";

    /**
     * The bitstream containing the bitcode to parse.
     */
    private final BitStream bitstream;

    /**
     * Current offset in the bitstream.
     */
    private long offset;

    LLVMBitcodeReader(BitStream bitstream) {
        this.bitstream = bitstream;
        this.offset = 0L;
    }

    /**
     * Set the offset from which to parse tokens.
     *
     * @param newOffset the new offset into the bitstream
     */
    void setOffset(long newOffset) {
        this.offset = newOffset;
    }

    /**
     * Get the offset from which the next token would be scanned.
     *
     * @return the current offset
     */
    long getOffset() {
        return offset;
    }

    /**
     * Read a fixed-width value from the {@link LLVMBitcodeReader#bitstream bitstream} at the
     * current {@link LLVMBitcodeReader#getOffset() offset}. The current
     * {@link LLVMBitcodeReader#getOffset() offset} is set to the first bit after that value.
     *
     * @param width number of bits to read
     * @return the next value in the stream zero-extended to a long
     */
    long readFixed(int width) {
        final long value = bitstream.read(offset, width);
        offset += width;
        return value;
    }

    private static final int CHAR_FIXED_WIDTH = 6;

    /**
     * Read a 6-bit character value from the {@link LLVMBitcodeReader#bitstream bitstream} at the
     * current {@link LLVMBitcodeReader#getOffset() offset}. The current
     * {@link LLVMBitcodeReader#getOffset() offset} is set to the first bit after that value.
     *
     * @return the next character in the stream
     */
    char readChar() {
        final long value = readFixed(CHAR_FIXED_WIDTH);
        return CHAR6.charAt((int) value);
    }

    /**
     * Read a variable-width value from the {@link LLVMBitcodeReader#bitstream bitstream} at the
     * current {@link LLVMBitcodeReader#getOffset() offset}. The current
     * {@link LLVMBitcodeReader#getOffset() offset} is set to the first bit after that value.
     *
     * @param width number of bits of each variable-length component of the value
     * @return the next value in the stream zero-extended to a long
     */
    long readVBR(int width) {
        long value = 0;
        long shift = 0;
        long datum;
        long dmask = 1 << (width - 1);
        do {
            datum = readFixed(width);
            value += (datum & (dmask - 1)) << shift;
            shift += width - 1;
        } while ((datum & dmask) != 0);
        return value;
    }

    /**
     * Word-align the {@link LLVMBitcodeReader#getOffset() offset}.
     */
    void alignInt() {
        long mask = Integer.SIZE - 1;
        if ((offset & mask) != 0) {
            offset = (offset & ~mask) + Integer.SIZE;
        }
    }

    /**
     * Total number of bits in the {@link LLVMBitcodeReader#bitstream bitstream}.
     *
     * @return the bit-size
     */
    long size() {
        return bitstream.size();
    }
}
