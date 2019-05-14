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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;

public class LLVMParserRootNode extends RootNode {

    public LLVMParserRootNode() {
        super(LLVMLanguage.getLanguage());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return null;
    }

    /**
     * Find the bitcode stream in the frame.
     *
     * @return the {@link FrameSlot} in which the bitstream is stored
     */
    FrameSlot getBitStreamSlot() {
        return null;
    }

    /**
     * Find the current offset into the bitcode stream in the frame.
     *
     * @return the {@link FrameSlot} in which the offset is stored
     */
    FrameSlot getBitStreamOffsetSlot() {
        return null;
    }

    /**
     * Find the current size of record identifiers in the frame.
     *
     * @return the {@link FrameSlot} in which the size is stored
     */
    FrameSlot getIdSizeSlot() {
        return null;
    }

    /**
     * Find the id of the record to parse next in the frame.
     *
     * @return the {@link FrameSlot} in which the id is stored
     */
    FrameSlot getRecordIdSlot() {
        return null;
    }

    /**
     * Find the stack-allocated parts of the record to parse on the stack.
     *
     * @return an array of {@link FrameSlot slots} in which the record is stored
     */
    FrameSlot[] getRecordSlots() {
        return null;
    }

    /**
     * Find the parts of the record to parse that did not fit into
     * {@link LLVMParserRootNode#getRecordSlots}.
     *
     * @return the {@link FrameSlot} which contains an array holding the spilled parts of the record
     */
    FrameSlot getSpilledRecordSlot() {
        return null;
    }

    /**
     * Find a possible blob from the last parsed record in the frame.
     *
     * @return the {@link FrameSlot} in which the blob is stored
     */
    FrameSlot getBlobSlot() {
        return null;
    }
}
