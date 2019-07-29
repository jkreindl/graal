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

import java.util.Arrays;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
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
public final class StructureType extends AggregateType implements TruffleObject {

    private final String name;
    private final boolean isPacked;
    @CompilationFinal(dimensions = 1) private final Type[] types;
    private int size = -1;

    public StructureType(String name, boolean isPacked, Type[] types) {
        assert name != null;
        this.name = name;
        this.isPacked = isPacked;
        this.types = types;
    }

    public StructureType(boolean isPacked, Type[] types) {
        this(LLVMIdentifier.UNKNOWN, isPacked, types);
    }

    public Type[] getElementTypes() {
        return types;
    }

    public boolean isPacked() {
        return isPacked;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getBitSize() {
        if (isPacked) {
            int sum = 0;
            for (Type member : types) {
                sum += member.getBitSize();
            }
            return sum;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("TargetDataLayout is necessary to compute Padding information!");
        }
    }

    @Override
    public void accept(TypeVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public int getNumberOfElements() {
        return types.length;
    }

    @Override
    public Type getElementType(long index) {
        assert index == (int) index;
        return types[(int) index];
    }

    @Override
    public int getAlignment(DataLayout targetDataLayout) {
        return isPacked ? 1 : getLargestAlignment(targetDataLayout);
    }

    @Override
    public int getSize(DataLayout targetDataLayout) {
        if (size != -1) {
            return size;
        }
        int sumByte = 0;
        for (final Type elementType : types) {
            if (!isPacked) {
                sumByte += Type.getPadding(sumByte, elementType, targetDataLayout);
            }
            sumByte += elementType.getSize(targetDataLayout);
        }

        int padding = 0;
        if (!isPacked && sumByte != 0) {
            padding = Type.getPadding(sumByte, getAlignment(targetDataLayout));
        }
        size = sumByte + padding;
        return size;
    }

    @Override
    public long getOffsetOf(long index, DataLayout targetDataLayout) {
        int offset = 0;
        for (int i = 0; i < index; i++) {
            final Type elementType = types[i];
            if (!isPacked) {
                offset += Type.getPadding(offset, elementType, targetDataLayout);
            }
            offset += elementType.getSize(targetDataLayout);
        }
        if (!isPacked && getSize(targetDataLayout) > offset) {
            assert index == (int) index;
            offset += Type.getPadding(offset, types[(int) index], targetDataLayout);
        }
        return offset;
    }

    private int getLargestAlignment(DataLayout targetDataLayout) {
        int largestAlignment = 0;
        for (final Type elementType : types) {
            largestAlignment = Math.max(largestAlignment, elementType.getAlignment(targetDataLayout));
        }
        return largestAlignment;
    }

    @Override
    @TruffleBoundary
    public String toString() {
        if (LLVMIdentifier.UNKNOWN.equals(name)) {
            return Arrays.stream(types).map(String::valueOf).collect(Collectors.joining(", ", "%{", "}"));
        } else {
            return name;
        }
    }

    @Override
    @TruffleBoundary
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isPacked ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + Arrays.hashCode(types);
        return result;
    }

    @Override
    @TruffleBoundary
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        StructureType other = (StructureType) obj;
        if (isPacked != other.isPacked) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (!Arrays.equals(types, other.types)) {
            return false;
        }
        return true;
    }

    private static final String MEMBER_IS_STRUCTURE_PACKED = "isPacked";
    private static final String MEMBER_HAS_NAME = "hasName";
    private static final String MEMBER_GET_NAME = "getName";

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean hasArrayElements() {
        return true;
    }

    @ExportMessage
    public long getArraySize() {
        return types.length;
    }

    @ExportMessage
    public boolean isArrayElementReadable(long index) {
        return index >= 0 && index < types.length;
    }

    @ExportMessage
    public Type readArrayElement(long index) throws InvalidArrayIndexException {
        if (index >= 0L && index < getArraySize()) {
            return types[(int) index];
        }

        throw InvalidArrayIndexException.create(index);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public LLVMNodeObjectKeys getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return getDefaultTypeKeys(MEMBER_IS_STRUCTURE_PACKED, MEMBER_HAS_NAME, MEMBER_GET_NAME);
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    public boolean isMemberReadable(String member) {
        assert member != null;
        switch (member) {
            case MEMBER_IS_STRUCTURE_PACKED:
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
            case MEMBER_IS_STRUCTURE:
                return true;
            case MEMBER_IS_STRUCTURE_PACKED:
                return isPacked;
            case MEMBER_HAS_NAME:
                return !LLVMIdentifier.UNKNOWN.equals(name);
            case MEMBER_GET_NAME:
                return name;
            default:
                return readDefaultMember(member, contextReference);
        }
    }
}
