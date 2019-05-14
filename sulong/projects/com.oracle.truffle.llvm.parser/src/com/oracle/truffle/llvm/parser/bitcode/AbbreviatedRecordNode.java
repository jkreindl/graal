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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

final class AbbreviatedRecordNode extends Node {

    static final EntryWriteNode[] NO_ENTRIES = new EntryWriteNode[0];

    @Children private EntryWriteNode[] entries;

    AbbreviatedRecordNode(EntryWriteNode[] entries) {
        this.entries = entries;
    }

    @ExplodeLoop
    void executeGeneric(VirtualFrame frame) {
        for (EntryWriteNode parseEntryNode : entries) {
            parseEntryNode.executeGeneric(frame);
        }
    }

    static final class FixedEntryReadNode extends SimpleEntryReadNode {
        FixedEntryReadNode(FrameSlot bitStreamSlot, FrameSlot offsetSlot, int width) {
            super(offsetSlot, width, new BCReader.ReadFixedNode(bitStreamSlot, offsetSlot));
        }
    }

    static final class VBREntryReadNode extends SimpleEntryReadNode {
        VBREntryReadNode(FrameSlot bitStreamSlot, FrameSlot offsetSlot, int width) {
            super(offsetSlot, width, new BCReader.ReadVBRNode(bitStreamSlot, offsetSlot));
        }
    }

    private abstract static class SimpleEntryReadNode extends EntryReadNode {

        private final int width;

        @Child private BCReader readNode;

        SimpleEntryReadNode(FrameSlot offsetSlot, int width, BCReader readNode) {
            super(offsetSlot);
            this.width = width;
            this.readNode = readNode;
        }

        @Override
        long executeGeneric(VirtualFrame frame) {
            return readNode.executeWithTarget(frame, width);
        }
    }

    static final class LiteralEntryReadNode extends EntryReadNode {

        private final long literal;

        LiteralEntryReadNode(FrameSlot offsetSlot, long literal) {
            super(offsetSlot);
            this.literal = literal;
        }

        @Override
        long executeGeneric(VirtualFrame frame) {
            return literal;
        }
    }

    abstract static class EntryReadNode extends Node {

        final FrameSlot offsetSlot;

        EntryReadNode(FrameSlot offsetSlot) {
            this.offsetSlot = offsetSlot;
        }

        abstract long executeGeneric(VirtualFrame frame);
    }

    abstract static class EntryWriteNode extends Node {

        @Child EntryReadNode entryReadNode;

        protected EntryWriteNode(EntryReadNode entryReadNode) {
            this.entryReadNode = entryReadNode;
        }

        abstract void executeGeneric(VirtualFrame frame);
    }

    static final class EntryWriteSlot extends EntryWriteNode {

        final FrameSlot targetSlot;

        EntryWriteSlot(EntryReadNode entryReadNode, FrameSlot targetSlot) {
            super(entryReadNode);
            this.targetSlot = targetSlot;
        }

        @Override
        void executeGeneric(VirtualFrame frame) {
            final long entryValue = entryReadNode.executeGeneric(frame);
            frame.setLong(targetSlot, entryValue);
        }
    }

    @NodeChild(value = "spilledEntriesContainer", type = ReadFrameNode.class)
    abstract static class EntryWriteSpilledSlot extends EntryWriteNode {

        private final FrameSlot spilledEntriesContainerSlot;
        private final int index;

        EntryWriteSpilledSlot(EntryReadNode entryReadNode, FrameSlot spilledEntriesContainerSlot, int index) {
            super(entryReadNode);
            this.spilledEntriesContainerSlot = spilledEntriesContainerSlot;
            this.index = index;
        }

        @Specialization
        void writeTarget(VirtualFrame frame, long[] spilledEntriesContainer) {
            final long entryValue = entryReadNode.executeGeneric(frame);
            if (index < spilledEntriesContainer.length) {
                spilledEntriesContainer[index] = entryValue;
            } else {
                final long[] newContainer = new long[index + 1];
                for (int i = 0; i < spilledEntriesContainer.length; i++) {
                    newContainer[i] = spilledEntriesContainer[i];
                }
                newContainer[index] = entryValue;
                frame.setObject(spilledEntriesContainerSlot, newContainer);
            }
        }
    }

    static final class ReadFrameNode extends Node {

        final FrameSlot slot;

        protected ReadFrameNode(FrameSlot slot) {
            this.slot = slot;
        }

        Object executeGeneric(VirtualFrame frame) {
            return FrameUtil.getObjectSafe(frame, slot);
        }
    }
}
