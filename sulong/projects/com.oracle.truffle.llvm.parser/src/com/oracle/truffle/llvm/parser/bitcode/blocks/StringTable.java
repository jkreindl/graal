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

import com.oracle.truffle.llvm.parser.model.ValueSymbol;

import java.util.ArrayList;

/**
 * Represents the String table, which contains the names of functions and globals as a single long
 * string. As the module block precedes the string table, these are commonly forward referenced.
 */
final class StringTable {

    private final ArrayList<NameRequest> requests;

    private String entries;

    StringTable() {
        this.entries = null;
        this.requests = new ArrayList<>();
    }

    void fillTable(String newEntries) {
        assert newEntries != null;
        this.entries = newEntries;

        for (NameRequest request : requests) {
            request.resolve(this);
        }

        requests.clear();
        requests.trimToSize();
    }

    private String get(int offset, int size) {
        if (offset + size < entries.length()) {
            return entries.substring(offset, offset + size);
        } else {
            return "";
        }
    }

    void requestName(int offset, int length, ValueSymbol target) {
        if (length <= 0 || offset < 0) {
            return;
        }
        // the STRTAB block's content may be forward referenced
        if (entries != null) {
            target.setName(get(offset, length));
        } else {
            requests.add(new NameRequest(offset, length, target));
        }
    }

    private static final class NameRequest {

        private final ValueSymbol target;
        private final int nameOffset;
        private final int nameLength;

        private NameRequest(int nameOffset, int nameLength, ValueSymbol target) {
            this.nameOffset = nameOffset;
            this.nameLength = nameLength;
            this.target = target;
        }

        void resolve(StringTable table) {
            target.setName(table.get(nameOffset, nameLength));
        }
    }
}
