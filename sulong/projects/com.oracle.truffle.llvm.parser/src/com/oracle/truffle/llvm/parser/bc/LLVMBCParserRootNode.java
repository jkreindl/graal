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
package com.oracle.truffle.llvm.parser.bc;

import java.util.Arrays;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;

public class LLVMBCParserRootNode extends RootNode {

    private static final int DEFAULT_RECORD_SIZE = 10;
    private static final int DEFAULT_ID_SIZE = 2;

    private static FrameDescriptor FRAME_DESCRIPTOR = null;
    private static FrameSlot ID_SIZE_SLOT;
    private static FrameSlot[] RECORD_SLOTS = null;

    @Child private LLVMBCParserNode blockParser;

    @CompilationFinal(dimensions = 1) protected FrameSlot[] recordSlots;

    @TruffleBoundary
    protected LLVMBCParserRootNode(LLVMBCParserNode blockParser) {
        super(LLVMLanguage.getLanguage());
        this.blockParser = blockParser;
        recordSlots = initRecordSlots();
    }

    @TruffleBoundary
    private FrameSlot[] initRecordSlots() {
        if (FRAME_DESCRIPTOR == null) {
            FRAME_DESCRIPTOR = new FrameDescriptor();
            ID_SIZE_SLOT = FRAME_DESCRIPTOR.addFrameSlot("idSize", FrameSlotKind.Int);

            RECORD_SLOTS = new FrameSlot[DEFAULT_RECORD_SIZE];
            for (int i = 0; i < RECORD_SLOTS.length; i++) {
                RECORD_SLOTS[i] = FRAME_DESCRIPTOR.addFrameSlot("op" + i, FrameSlotKind.Long);
            }
        }
        return RECORD_SLOTS;
    }

    public static FrameSlot getIdSizeSlot() {
        return ID_SIZE_SLOT;
    }

    FrameSlot[] getRecordSlots() {
        return RECORD_SLOTS;
    }

    @TruffleBoundary
    FrameSlot[] extendRecordSlots(int newSize) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        RECORD_SLOTS = Arrays.copyOf(RECORD_SLOTS, newSize);
        for (int i = 0; i < RECORD_SLOTS.length; i++) {
            if (RECORD_SLOTS[i] != null) {
                RECORD_SLOTS[i] = FRAME_DESCRIPTOR.addFrameSlot("op" + i, FrameSlotKind.Long);
            }
        }
        return RECORD_SLOTS;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        frame.setInt(ID_SIZE_SLOT, DEFAULT_ID_SIZE);
        return blockParser.executeWithTarget(frame, 0L);
    }

    @ExplodeLoop
    static void clearRecord(VirtualFrame frame, FrameSlot[] recordSlots) {
        for (int i = 0; i < recordSlots.length; i++) {
            frame.setLong(recordSlots[i], 0L);
        }
    }
}
