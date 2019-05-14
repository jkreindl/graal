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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

import java.util.ArrayList;

public abstract class LLVMParserBlockNode extends Node implements RepeatingNode {

    static LoopNode createBlockParser(LLVMParserBlockNode blockParser) {
        return Truffle.getRuntime().createLoopNode(blockParser);
    }

    private static final int ID_END_BLOCK = 0;
    private static final int ID_ENTER_SUBBLOCK = 1;
    private static final int ID_DEFINE_ABBREV = 2;
    private static final int ID_UNABBREV_RECORD = 3;
    private static final int ID_CUSTOM_ABBREV_OFFSET = 4;

    private final FrameSlot idSizeSlot;
    private final FrameSlot bitOffsetSlot;
    @CompilationFinal(dimensions = 1) private final FrameSlot[] recordSlots;
    private final FrameSlot spilledRecordEntriesSlot;

    @Child private BCReader readFixedNode;
    @Child private BCReader readVBRNode;

    public LLVMParserBlockNode(LLVMParserRootNode rootNode) {
        this.idSizeSlot = rootNode.getIdSizeSlot();
        this.bitOffsetSlot = rootNode.getBitStreamOffsetSlot();
        this.recordSlots = rootNode.getRecordSlots();
        this.spilledRecordEntriesSlot = rootNode.getSpilledRecordSlot();

        this.readFixedNode = new BCReader.ReadFixedNode(rootNode.getBitStreamSlot(), rootNode.getBitStreamOffsetSlot());
        this.readVBRNode = new BCReader.ReadVBRNode(rootNode.getBitStreamSlot(), rootNode.getBitStreamOffsetSlot());
    }

    @Override
    public boolean executeRepeating(VirtualFrame frame) {
        final int currentIdSize = getIdSize(frame);
        final int id = readFixedInt(frame, currentIdSize);

        switch (id) {
            case ID_END_BLOCK: {
                // byte-align the offset to find the bitcode entity following the parsed block
                byteAlignOffset(frame);

                // TODO reset abbreviation definitions

                // allow the block to e.g. batch resolve forward references
                afterBlock();

                return false;
            }

            case ID_ENTER_SUBBLOCK:
                enterContainedBlock(frame);
                break;

            case ID_DEFINE_ABBREV:
                defineAbbreviation(frame);
                break;

            case ID_UNABBREV_RECORD:
                unabbreviatedRecord();
                break;

            case ID_CUSTOM_ABBREV_OFFSET:
                // custom defined abbreviation
                handleRecord(id);
                break;

            default:
                CompilerDirectives.transferToInterpreter();
                throw new LLVMParserException("Unknown LLVM bitcode id: " + id);
        }
        return false;
    }

    private static final int SUBBLOCK_ID_BITS = 8;
    private static final int SUBBLOCK_SIZE_BITS = 4;

    private void enterContainedBlock(VirtualFrame frame) {
        // safe the current parser state
        final int currentIDSize = getIdSize(frame);

        // setup parser for the contained block
        final int blockId = readVBRInt(frame, SUBBLOCK_ID_BITS);
        final int blockSize = readVBRInt(frame, SUBBLOCK_SIZE_BITS);
        byteAlignOffset(frame);

        // parse the contained block
        parseContainedBlock(blockId);

        // reset parser state after the contained block has been parsed
        setIDSize(frame, currentIDSize);
    }

    /**
     * Parse a block denoted by the given ID.
     *
     * @param blockId identifier for the kind of block to parse
     */
    abstract void parseContainedBlock(int blockId);

    /**
     * Invoked after the current block has been fully parsed.
     */
    abstract void afterBlock();

    /**
     * Parse a record with the given ID.
     *
     * @param recordId identifier for the kind of record to parse
     */
    abstract void handleRecord(int recordId);

    private static final int ABBREVIATED_RECORD_OPERANDS_COUNT_BITS = 5;
    private static final int ABBREVIATED_RECORD_OPERAND_LITERAL_BIT_BITS = 1;
    private static final int ABBREVIATED_RECORD_OPERAND_LITERAL_BITS = 8;
    private static final int ABBREVIATED_RECORD_OPERAND_TYPE_BITS = 3;

    /**
     * Define a custom record.
     */
    private void defineAbbreviation(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final int operandCount = readVBRInt(frame, ABBREVIATED_RECORD_OPERANDS_COUNT_BITS);
        final ArrayList<AbbreviatedRecordNode.EntryWriteNode> recordEntries = new ArrayList<>(operandCount);

        int currentOperand = 0;
        while (currentOperand < operandCount) {
            final boolean isLiteral = readFixed(frame, ABBREVIATED_RECORD_OPERAND_LITERAL_BIT_BITS) == 1L;
            if (isLiteral) {
                final long literal = readVBR(frame, ABBREVIATED_RECORD_OPERAND_LITERAL_BITS);
                AbbreviatedRecordNode.EntryReadNode readNode = new AbbreviatedRecordNode.LiteralEntryReadNode(bitOffsetSlot, literal);
                AbbreviatedRecordNode.EntryWriteNode writeNode = createRecordWriteNode(readNode, currentOperand);
                recordEntries.add(writeNode);

            } else {
                final int recordType = readFixedInt(frame, ABBREVIATED_RECORD_OPERAND_TYPE_BITS);
            }

            currentOperand++;
        }

        // TODO parse all entry kinds

        final AbbreviatedRecordNode.EntryWriteNode[] entries = recordEntries.toArray(AbbreviatedRecordNode.NO_ENTRIES);
        final AbbreviatedRecordNode recordNode = new AbbreviatedRecordNode(entries);

        // TODO store and run record parser
    }

    private AbbreviatedRecordNode.EntryWriteNode createRecordWriteNode(AbbreviatedRecordNode.EntryReadNode readNode, int index) {
        if (index < recordSlots.length) {
            return new AbbreviatedRecordNode.EntryWriteSlot(readNode, recordSlots[index]);
        } else {
            return AbbreviatedRecordNodeFactory.EntryWriteSpilledSlotNodeGen.create(readNode, spilledRecordEntriesSlot, index, new AbbreviatedRecordNode.ReadFrameNode(spilledRecordEntriesSlot));
        }
    }

    /**
     * Parse an unoptimized record.
     */
    private void unabbreviatedRecord() {
        // TODO
    }

    private int readFixedInt(VirtualFrame frame, int bits) {
        assert bits <= Integer.SIZE;
        final long datum = readFixedNode.executeWithTarget(frame, bits);
        return (int) datum;
    }

    private int readVBRInt(VirtualFrame frame, int bits) {
        assert bits <= Integer.SIZE;
        final long datum = readVBRNode.executeWithTarget(frame, bits);
        return (int) datum;
    }

    private long readFixed(VirtualFrame frame, int bits) {
        return readFixedNode.executeWithTarget(frame, bits);
    }

    private long readVBR(VirtualFrame frame, int bits) {
        return readVBRNode.executeWithTarget(frame, bits);
    }

    private long byteAlignOffset(VirtualFrame frame) {
        long offset = getBitOffset(frame);
        long mask = Integer.SIZE - 1;
        if ((offset & mask) != 0) {
            offset = (offset & ~mask) + Integer.SIZE;
            setBitOffset(frame, offset);
        }
        return offset;
    }

    private void setIDSize(VirtualFrame frame, int idSize) {
        frame.setInt(idSizeSlot, idSize);
    }

    private int getIdSize(VirtualFrame frame) {
        return FrameUtil.getIntSafe(frame, idSizeSlot);
    }

    private void setBitOffset(VirtualFrame frame, long newOffset) {
        frame.setLong(bitOffsetSlot, newOffset);
    }

    private long getBitOffset(VirtualFrame frame) {
        return FrameUtil.getLongSafe(frame, bitOffsetSlot);
    }
}
