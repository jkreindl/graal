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
package com.oracle.truffle.llvm.parser.bitcode;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import org.graalvm.polyglot.io.ByteSequence;

abstract class BCReader extends Node {

    abstract long executeWithTarget(VirtualFrame frame, int bits);

    static class ReadFixedNode extends BCReader {

        private static final int BYTE_BITS_SHIFT = 3;
        private static final int BYTE_BITS_MASK = 0x7;

        private static final long BYTE_MASK = 0xffL;

        private final FrameSlot bitStreamSlot;
        private final FrameSlot offsetSlot;

        ReadFixedNode(FrameSlot bitStreamSlot, FrameSlot offsetSlot) {
            this.bitStreamSlot = bitStreamSlot;
            this.offsetSlot = offsetSlot;
        }

        @Override
        long executeWithTarget(VirtualFrame frame, int bits) {
            final ByteSequence bitStream;
            bitStream = (ByteSequence) FrameUtil.getObjectSafe(frame, bitStreamSlot);

            long offset = FrameUtil.getLongSafe(frame, offsetSlot);
            frame.setLong(offsetSlot, offset + bits);

            int byteIndex = (int) (offset >> BYTE_BITS_SHIFT);
            int bitOffsetInByte = (int) (offset & BYTE_BITS_MASK);
            int availableBits = Byte.SIZE - bitOffsetInByte;

            long value = (bitStream.byteAt(byteIndex++) & BYTE_MASK) >> bitOffsetInByte;
            if (bits <= availableBits) {
                return value & (BYTE_MASK >> (8 - bits));
            }
            int remainingBits = bits - availableBits;
            int shift = availableBits;
            while (true) {
                byte byteValue = bitStream.byteAt(byteIndex++);

                if (remainingBits > Byte.SIZE) {
                    value = value | ((byteValue & BYTE_MASK) << shift);
                    remainingBits -= Byte.SIZE;
                    shift += Byte.SIZE;
                } else {
                    return value | ((byteValue & (BYTE_MASK >> (Byte.SIZE - remainingBits))) << shift);
                }
            }
        }
    }

    static class ReadVBRNode extends BCReader {

        @Child private ReadFixedNode readFixedNode;

        ReadVBRNode(FrameSlot bitStreamSlot, FrameSlot offsetSlot) {
            readFixedNode = new ReadFixedNode(bitStreamSlot, offsetSlot);
        }

        @Override
        long executeWithTarget(VirtualFrame frame, int width) {
            long value = 0;
            long shift = 0;
            long datum;
            long dmask = 1 << (width - 1);
            do {
                datum = readFixedNode.executeWithTarget(frame, width);
                value += (datum & (dmask - 1)) << shift;
                shift += width - 1;
            } while ((datum & dmask) != 0);
            return value;
        }
    }
}
