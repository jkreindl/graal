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
package com.oracle.truffle.llvm.runtime.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.graalvm.collections.EconomicMap;

@ExportLibrary(InteropLibrary.class)
public final class LLVMNodeObject implements TruffleObject {

    public static boolean isInstance(TruffleObject obj) {
        return obj instanceof LLVMNodeObject;
    }

    private final EconomicMap<String, Object> entries;
    @CompilationFinal(dimensions = 1) private final String[] keys;

    @TruffleBoundary
    public LLVMNodeObject(String[] keys, Object[] values) {
        assert keys != null;
        assert values != null;
        assert keys.length == values.length;
        this.keys = keys;
        this.entries = EconomicMap.create(keys.length);
        for (int i = 0; i < keys.length; i++) {
            this.entries.put(keys[i], values[i]);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new LLVMNodeObjectKeys(keys);
    }

    @TruffleBoundary
    private Object getValue(String key) {
        return entries.get(key);
    }

    @ExportMessage
    Object readMember(String key,
                    @Cached BranchProfile exception) throws UnknownIdentifierException {
        final Object element = getValue(key);
        if (element != null) {
            return element;
        } else {
            exception.enter();
            throw UnknownIdentifierException.create(key);
        }
    }

    @ExportMessage
    boolean isMemberReadable(String key) {
        final Object element = getValue(key);
        return element != null;
    }
}
