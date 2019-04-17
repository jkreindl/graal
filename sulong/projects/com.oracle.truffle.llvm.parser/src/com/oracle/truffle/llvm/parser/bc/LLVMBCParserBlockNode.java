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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

public abstract class LLVMBCParserBlockNode extends LLVMBCParserNode {

    protected abstract Object executeSubBlock(VirtualFrame frame, int blockId);
    protected abstract Object parseRecord(VirtualFrame frame, int recordId);

    static final int END_BLOCK = 0;
    static final int ENTER_SUBBLOCK = 1;
    static final int DEFINE_ABBREV = 2;
    static final int UNABBREV_RECORD = 3;
    static final int CUSTOM_ABBREV_OFFSET = 4;

    @Override
    public Object executeWithTarget(VirtualFrame frame, long bcOffset) {
        switch (id) {
            case END_BLOCK:
                return null;

            case ENTER_SUBBLOCK:
                enterSubBlock();
                break;

            case DEFINE_ABBREV:
                defineAbbreviation();
                break;

            case UNABBREV_RECORD:
                unabbreviatedRecord();
                break;

            case CUSTOM_ABBREV_OFFSET:
                abbreviatedRecord(id);
                break;

            default: {
                CompilerDirectives.transferToInterpreter();
                throw new LLVMParserException("Unknown entry id in block: " + id);
            }
        }
        return null;
    }
}
