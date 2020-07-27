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

import com.oracle.truffle.llvm.parser.model.attributes.AttributesCodeEntry;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of Parameter Attributes.
 */
final class ParameterAttributes {

    // stores attributes defined in PARAMATTR_GRP_CODE_ENTRY
    private final List<AttributesGroup> attributes;

    // store code entries defined in PARAMATTR_CODE_ENTRY
    private final ArrayList<AttributesCodeEntry> parameterCodeEntry;

    ParameterAttributes() {
        this.parameterCodeEntry = new ArrayList<>();
        this.attributes = new ArrayList<>();
    }

    void addAttributes(AttributesGroup newAttributes) {
        attributes.add(newAttributes);
    }

    void addCodeEntry(AttributesCodeEntry newEntry) {
        assert newEntry != null;
        parameterCodeEntry.add(newEntry);
    }

    List<AttributesGroup> getAttributes() {
        return attributes;
    }

    /**
     * Get ParsedAttributeGroup by Bitcode index.
     *
     * @param idx index as it was defined in the LLVM-Bitcode, means starting with 1
     * @return found attributeGroup, or otherwise an empty List
     */
    AttributesCodeEntry getCodeEntry(long idx) {
        if (idx <= 0 || parameterCodeEntry.size() < idx) {
            return AttributesCodeEntry.EMPTY;
        }

        return parameterCodeEntry.get(Math.toIntExact(idx - 1));
    }
}
