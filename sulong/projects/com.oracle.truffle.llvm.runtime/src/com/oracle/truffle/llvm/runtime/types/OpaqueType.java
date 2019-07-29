/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.runtime.types;

import java.util.Objects;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.datalayout.DataLayout;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMNodeObjectKeys;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

@ExportLibrary(InteropLibrary.class)
public final class OpaqueType extends Type implements TruffleObject {

    private final String name;

    public OpaqueType() {
        this(LLVMIdentifier.UNKNOWN);
    }

    public OpaqueType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getBitSize() {
        return 0;
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getAlignment(DataLayout targetDataLayout) {
        return Long.BYTES;
    }

    @Override
    public int getSize(DataLayout targetDataLayout) {
        return 0;
    }

    @Override
    public String toString() {
        if (LLVMIdentifier.isUnknown(name)) {
            return "opaque";
        } else {
            return name;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OpaqueType) {
            OpaqueType other = (OpaqueType) obj;
            return Objects.equals(name, other.name);
        }
        return false;
    }

    private static final String MEMBER_HAS_NAME = "hasName";
    private static final String MEMBER_GET_NAME = "getName";

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public LLVMNodeObjectKeys getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return getDefaultTypeKeys(MEMBER_HAS_NAME, MEMBER_GET_NAME);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean isMemberReadable(String member) {
        assert member != null;
        switch (member) {
            case MEMBER_HAS_NAME:
            case MEMBER_GET_NAME:
                return true;
            default:
                return isDefaultMember(member);
        }
    }

    @ExportMessage
    public Object readMember(String member, @CachedContext(LLVMLanguage.class) TruffleLanguage.ContextReference<LLVMContext> contextReference) throws UnknownIdentifierException {
        assert member != null;
        switch (member) {
            case MEMBER_IS_OPAQUE:
                return true;
            case MEMBER_HAS_NAME:
                return !LLVMIdentifier.UNKNOWN.equals(name);
            case MEMBER_GET_NAME:
                return name;
            default:
                return readDefaultMember(member, contextReference);
        }
    }
}
