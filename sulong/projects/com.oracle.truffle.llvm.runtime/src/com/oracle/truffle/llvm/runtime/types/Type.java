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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.datalayout.DataLayout;
import com.oracle.truffle.llvm.runtime.instrumentation.LLVMKeysObject;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType.PrimitiveKind;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

public abstract class Type {

    public static final Type[] EMPTY_ARRAY = {};

    public abstract int getBitSize();

    public abstract void accept(TypeVisitor visitor);

    public abstract int getAlignment(DataLayout targetDataLayout);

    public abstract int getSize(DataLayout targetDataLayout);

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    public static Type getIntegerType(int size) {
        switch (size) {
            case 1:
                return PrimitiveType.I1;
            case 8:
                return PrimitiveType.I8;
            case 16:
                return PrimitiveType.I16;
            case 32:
                return PrimitiveType.I32;
            case 64:
                return PrimitiveType.I64;
            default:
                return new VariableBitWidthType(size);
        }
    }

    public static Type createConstantForType(Type type, Object value) {
        if (type instanceof PrimitiveType) {
            return new PrimitiveType(((PrimitiveType) type).getPrimitiveKind(), value);
        } else {
            return new VariableBitWidthType(((VariableBitWidthType) type).getBitSize(), value);
        }
    }

    public static boolean isIntegerType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType primitive = (PrimitiveType) type;
            PrimitiveKind kind = primitive.getPrimitiveKind();
            return kind == PrimitiveKind.I1 || kind == PrimitiveKind.I8 || kind == PrimitiveKind.I16 || kind == PrimitiveKind.I32 || kind == PrimitiveKind.I64;
        }
        return type instanceof VariableBitWidthType;
    }

    public static boolean isFloatingpointType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType primitive = (PrimitiveType) type;
            PrimitiveKind kind = primitive.getPrimitiveKind();
            return kind == PrimitiveKind.F128 || kind == PrimitiveKind.FLOAT || kind == PrimitiveKind.HALF || kind == PrimitiveKind.PPC_FP128 ||
                            kind == PrimitiveKind.X86_FP80 || kind == PrimitiveKind.DOUBLE;
        }
        return false;
    }

    public static FrameSlotKind getFrameSlotKind(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType primitive = (PrimitiveType) type;
            PrimitiveKind kind = primitive.getPrimitiveKind();
            switch (kind) {
                case FLOAT:
                    return FrameSlotKind.Float;
                case DOUBLE:
                    return FrameSlotKind.Double;
                case I1:
                    return FrameSlotKind.Boolean;
                case I16:
                case I32:
                    return FrameSlotKind.Int;
                case I64:
                    return FrameSlotKind.Long;
                case I8:
                    return FrameSlotKind.Byte;
                default:
                    return FrameSlotKind.Object;

            }
        } else if (type instanceof VariableBitWidthType) {
            switch (type.getBitSize()) {
                case 1:
                    return FrameSlotKind.Boolean;
                case 8:
                    return FrameSlotKind.Byte;
                case 16:
                case 32:
                    return FrameSlotKind.Int;
                case 64:
                    return FrameSlotKind.Long;
                default:
                    return FrameSlotKind.Object;
            }
        }
        return FrameSlotKind.Object;
    }

    public static int getPadding(long offset, int alignment) {
        assert (alignment == 0 ? 0 : (alignment - (offset % alignment)) % alignment) == (int) (alignment == 0 ? 0 : (alignment - (offset % alignment)) % alignment);
        return (int) (alignment == 0 ? 0 : (alignment - (offset % alignment)) % alignment);
    }

    public static int getPadding(long offset, Type type, DataLayout targetDataLayout) {
        final int alignment = type.getAlignment(targetDataLayout);
        return getPadding(offset, alignment);
    }

    private static final String MEMBER_BYTESIZE = "byteSize";
    private static final String MEMBER_ALIGNMENT = "byteAlignment";
    static final String MEMBER_IS_INTEGER = "isIntegerType";
    static final String MEMBER_IS_FLOATING_POINT = "isFloatingPointType";
    static final String MEMBER_IS_STRUCTURE = "isStructureType";
    static final String MEMBER_IS_ARRAY = "isArrayType";
    static final String MEMBER_IS_VECTOR = "isVectorType";
    static final String MEMBER_IS_POINTER = "isPointerType";
    static final String MEMBER_IS_OPAQUE = "isOpaqueType";
    static final String MEMBER_IS_FUNCTION = "isFunctionType";
    static final String MEMBER_IS_VOID = "isVoidType";
    static final String MEMBER_IS_META = "isMetaType";

    private static final int DEFAULT_MEMBER_COUNT = 12;

    public static boolean isDefaultMember(String key) {
        assert key != null;
        switch (key) {
            case MEMBER_BYTESIZE:
            case MEMBER_ALIGNMENT:
            case MEMBER_IS_INTEGER:
            case MEMBER_IS_FLOATING_POINT:
            case MEMBER_IS_STRUCTURE:
            case MEMBER_IS_ARRAY:
            case MEMBER_IS_VECTOR:
            case MEMBER_IS_POINTER:
            case MEMBER_IS_OPAQUE:
            case MEMBER_IS_FUNCTION:
            case MEMBER_IS_VOID:
            case MEMBER_IS_META:
                return true;

            default:
                return false;
        }
    }

    static LLVMKeysObject getDefaultTypeKeys(String... additionalMembers) {
        final String[] members = new String[DEFAULT_MEMBER_COUNT + additionalMembers.length];
        members[0] = MEMBER_BYTESIZE;
        members[1] = MEMBER_ALIGNMENT;
        members[2] = MEMBER_IS_INTEGER;
        members[3] = MEMBER_IS_FLOATING_POINT;
        members[4] = MEMBER_IS_STRUCTURE;
        members[5] = MEMBER_IS_ARRAY;
        members[6] = MEMBER_IS_VECTOR;
        members[7] = MEMBER_IS_POINTER;
        members[8] = MEMBER_IS_OPAQUE;
        members[9] = MEMBER_IS_FUNCTION;
        members[10] = MEMBER_IS_VOID;
        members[11] = MEMBER_IS_META;
        for (int i = 0; i < additionalMembers.length; i++) {
            members[DEFAULT_MEMBER_COUNT + i] = additionalMembers[i];
        }
        return new LLVMKeysObject(members);
    }

    public Object readDefaultMember(String member, TruffleLanguage.ContextReference<LLVMContext> contextReference) throws UnknownIdentifierException {
        assert member != null;
        switch (member) {
            case MEMBER_BYTESIZE:
                return contextReference.get().getByteSize(this);

            case MEMBER_ALIGNMENT:
                return contextReference.get().getByteAlignment(this);

            case MEMBER_IS_INTEGER:
            case MEMBER_IS_FLOATING_POINT:
            case MEMBER_IS_STRUCTURE:
            case MEMBER_IS_ARRAY:
            case MEMBER_IS_VECTOR:
            case MEMBER_IS_POINTER:
            case MEMBER_IS_OPAQUE:
            case MEMBER_IS_FUNCTION:
            case MEMBER_IS_VOID:
                return false;

            default:
                CompilerDirectives.transferToInterpreter();
                throw UnknownIdentifierException.create(member);
        }
    }
}
