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
package com.oracle.truffle.llvm.runtime.instrumentation;

import com.oracle.truffle.api.CompilerAsserts;
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
import org.graalvm.collections.MapCursor;

@ExportLibrary(InteropLibrary.class)
public final class LLVMNodeObject implements TruffleObject {

    public static final LLVMNodeObject EMPTY = new LLVMNodeObject(new String[0], new Object[0]);

    @CompilationFinal(dimensions = 1) private final String[] keys;
    @CompilationFinal(dimensions = 1) private final Object[] values;

    private LLVMNodeObject(String[] keys, Object[] values) {
        assert notNull(keys);
        assert notNull(values);

        this.keys = keys;
        this.values = values;
    }

    public String[] getKeys() {
        return keys;
    }

    public Object[] getValues() {
        return values;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return new LLVMKeysObject(keys);
    }

    @ExportMessage
    Object readMember(String member, @Cached BranchProfile exception) throws UnknownIdentifierException {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i].equals(member)) {
                return values[i];
            }
        }

        exception.enter();
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        for (String key : keys) {
            if (key.equals(member)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        final StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < keys.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(keys[i]).append(" = ").append(values[i]);
        }
        builder.append("}");
        return builder.toString();
    }

    public static LLVMNodeObject create(EconomicMap<String, Object> entries) {
        CompilerAsserts.neverPartOfCompilation("Node objects must not be allocated in compilation");
        if (entries == null || entries.isEmpty()) {
            return EMPTY;
        }

        final String[] keys = new String[entries.size()];
        final Object[] values = new Object[entries.size()];
        final MapCursor<String, Object> entriesIterator = entries.getEntries();
        for (int i = 0; i < keys.length && entriesIterator.advance(); i++) {
            keys[i] = entriesIterator.getKey();
            values[i] = entriesIterator.getValue();
        }

        return new LLVMNodeObject(keys, values);
    }

    public static LLVMNodeObject create(String key, Object value) {
        CompilerAsserts.neverPartOfCompilation("Node objects must not be allocated in compilation");
        assert key != null;
        assert value != null;
        return new LLVMNodeObject(new String[]{key}, new Object[]{value});
    }

    private static <T> boolean notNull(T[] arr) {
        if (arr == null) {
            return false;
        }

        for (T t : arr) {
            if (t == null) {
                return false;
            }
        }

        return true;
    }
}
