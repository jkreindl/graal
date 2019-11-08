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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.llvm.runtime.datalayout.DataLayout;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.OpaqueType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.LLVMIdentifier;

public abstract class LLVMTypeInteropWrapper<T extends Type> implements TruffleObject {

    public static LLVMTypeInteropWrapper<? extends Type> create(Type type, DataLayout dataLayout) {
        if (type instanceof PrimitiveType) {
            return new Primitive((PrimitiveType) type, dataLayout);
        } else if (type instanceof PointerType) {
            return new Pointer((PointerType) type, dataLayout);
        } else if (type instanceof VariableBitWidthType) {
            return new VarBitInteger((VariableBitWidthType) type, dataLayout);
        } else if (type instanceof FunctionType) {
            return new Function((FunctionType) type, dataLayout);
        } else if (type instanceof ArrayType) {
            return new Array((ArrayType) type, dataLayout);
        } else if (type instanceof StructureType) {
            return new Structure((StructureType) type, dataLayout);
        } else if (type instanceof VectorType) {
            return new VectorWrapper((VectorType) type, dataLayout);
        } else if (type instanceof VoidType) {
            return new Void((VoidType) type, dataLayout);
        } else if (type instanceof MetaType) {
            return new Meta((MetaType) type, dataLayout);
        } else if (type instanceof OpaqueType) {
            return new Opaque((OpaqueType) type, dataLayout);
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException("Unsupported type for interop: " + type);
        }
    }

    private static final int NO_SIZE_OR_ALIGNMENT = -1;

    final T type;
    final DataLayout dataLayout;

    @CompilationFinal private int size = NO_SIZE_OR_ALIGNMENT;
    @CompilationFinal private int alignment = NO_SIZE_OR_ALIGNMENT;

    LLVMTypeInteropWrapper(T type, DataLayout dataLayout) {
        assert type != null;
        assert dataLayout != null;

        this.type = type;
        this.dataLayout = dataLayout;
    }

    private static final String MEMBER_BYTESIZE = "byteSize";
    private static final String MEMBER_ALIGNMENT = "byteAlignment";
    private static final String MEMBER_IS_INTEGER = "isIntegerType";
    private static final String MEMBER_IS_FLOATING_POINT = "isFloatingPointType";
    private static final String MEMBER_IS_STRUCTURE = "isStructureType";
    private static final String MEMBER_IS_ARRAY = "isArrayType";
    private static final String MEMBER_IS_VECTOR = "isVectorType";
    private static final String MEMBER_IS_POINTER = "isPointerType";
    private static final String MEMBER_IS_OPAQUE = "isOpaqueType";
    private static final String MEMBER_IS_FUNCTION = "isFunctionType";
    private static final String MEMBER_IS_VOID = "isVoidType";
    private static final String MEMBER_IS_META = "isMetaType";

    private int getSize() {
        if (size == NO_SIZE_OR_ALIGNMENT) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            size = type.getSize(dataLayout);
        }

        return size;
    }

    private int getAlignment() {
        if (alignment == NO_SIZE_OR_ALIGNMENT) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            alignment = type.getAlignment(dataLayout);
        }

        return alignment;
    }

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

    Object readDefaultMember(String member) throws UnknownIdentifierException {
        assert member != null;
        switch (member) {
            case MEMBER_BYTESIZE:
                return getSize();

            case MEMBER_ALIGNMENT:
                return getAlignment();

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

    @ExportLibrary(InteropLibrary.class)
    public static final class Array extends LLVMTypeInteropWrapper<ArrayType> {

        private static final String MEMBER_ARRAY_LENGTH = "getArrayLength";
        private static final String MEMBER_ELEMENT_TYPE = "getElementType";

        Array(ArrayType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            assert member != null;
            switch (member) {
                case MEMBER_ARRAY_LENGTH:
                case MEMBER_ELEMENT_TYPE:
                    return true;
                default:
                    return isDefaultMember(member);
            }
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_ARRAY_LENGTH, MEMBER_ELEMENT_TYPE);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_ARRAY:
                    return true;
                case MEMBER_ARRAY_LENGTH:
                    return type.getNumberOfElements();
                case MEMBER_ELEMENT_TYPE:
                    return LLVMTypeInteropWrapper.create(type.getElementType(), dataLayout);
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class VectorWrapper extends LLVMTypeInteropWrapper<VectorType> {

        // checkstyle erroneously errors if we call this 'Vector' because that is also the name of a
        // banned synchronized collection class
        VectorWrapper(VectorType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        private static final String MEMBER_VECTOR_LENGTH = "getVectorLength";
        private static final String MEMBER_ELEMENT_TYPE = "getElementType";

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            assert member != null;
            switch (member) {
                case MEMBER_VECTOR_LENGTH:
                case MEMBER_ELEMENT_TYPE:
                    return true;
                default:
                    return isDefaultMember(member);
            }
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_VECTOR_LENGTH, MEMBER_ELEMENT_TYPE);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_VECTOR:
                    return true;
                case MEMBER_VECTOR_LENGTH:
                    return type.getNumberOfElements();
                case MEMBER_ELEMENT_TYPE:
                    return LLVMTypeInteropWrapper.create(type.getElementType(), dataLayout);
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Pointer extends LLVMTypeInteropWrapper<PointerType> {

        Pointer(PointerType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        private static final String MEMBER_BASE_TYPE = "getBaseType";

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_BASE_TYPE);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            return MEMBER_BASE_TYPE.equals(member) || isDefaultMember(member);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_POINTER:
                    return true;
                case MEMBER_BASE_TYPE:
                    return LLVMTypeInteropWrapper.create(type.getPointeeType(), dataLayout);
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Structure extends LLVMTypeInteropWrapper<StructureType> {

        Structure(StructureType type, DataLayout dataLayout) {
            super(type, dataLayout);
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
            return type.getElementTypes().length;
        }

        @ExportMessage
        public boolean isArrayElementReadable(long index) {
            return index >= 0 && index < getArraySize();
        }

        @ExportMessage
        public Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (isArrayElementReadable(index)) {
                return LLVMTypeInteropWrapper.create(type.getElementTypes()[(int) index], dataLayout);
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
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
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
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_STRUCTURE:
                    return true;
                case MEMBER_IS_STRUCTURE_PACKED:
                    return type.isPacked();
                case MEMBER_HAS_NAME:
                    return !LLVMIdentifier.UNKNOWN.equals(type.getName());
                case MEMBER_GET_NAME:
                    return type.getName();
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Function extends LLVMTypeInteropWrapper<FunctionType> {

        Function(FunctionType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return type.getArgumentTypes().length;
        }

        @ExportMessage
        public boolean isArrayElementReadable(long index) {
            return index >= 0 && index < getArraySize();
        }

        @ExportMessage
        public Object readArrayElement(long index) throws InvalidArrayIndexException {
            final Type[] argTypes = type.getArgumentTypes();
            if (isArrayElementReadable(index)) {
                return LLVMTypeInteropWrapper.create(argTypes[(int) index], dataLayout);
            }

            throw InvalidArrayIndexException.create(index);
        }

        private static final String MEMBER_IS_FUNCTION_VARARGS = "isVarArgsFunctionType";

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_IS_FUNCTION_VARARGS);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            return MEMBER_IS_FUNCTION_VARARGS.equals(member) || isDefaultMember(member);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_FUNCTION:
                    return true;
                case MEMBER_IS_FUNCTION_VARARGS:
                    return type.isVarargs();
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Meta extends LLVMTypeInteropWrapper<MetaType> {

        Meta(MetaType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        private static final String MEMBER_GET_KIND = "getKind";

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_GET_KIND);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            return MEMBER_GET_KIND.equals(member) || isDefaultMember(member);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_META:
                    return true;
                case MEMBER_GET_KIND:
                    return type.toString();
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Opaque extends LLVMTypeInteropWrapper<OpaqueType> {

        Opaque(OpaqueType type, DataLayout dataLayout) {
            super(type, dataLayout);
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
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
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
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_OPAQUE:
                    return true;
                case MEMBER_HAS_NAME:
                    return !LLVMIdentifier.UNKNOWN.equals(type.getName());
                case MEMBER_GET_NAME:
                    return type.getName();
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Primitive extends LLVMTypeInteropWrapper<PrimitiveType> {

        Primitive(PrimitiveType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        private static final String MEMBER_BIT_SIZE = "getBitSize";

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_BIT_SIZE);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            return MEMBER_BIT_SIZE.equals(member) || isDefaultMember(member);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_INTEGER:
                    switch (type.getPrimitiveKind()) {
                        case I1:
                        case I8:
                        case I16:
                        case I32:
                        case I64:
                            return true;
                        default:
                            return false;
                    }
                case MEMBER_IS_FLOATING_POINT:
                    switch (type.getPrimitiveKind()) {
                        case HALF:
                        case FLOAT:
                        case DOUBLE:
                        case X86_FP80:
                        case F128:
                        case PPC_FP128:
                            return true;
                        default:
                            return false;
                    }
                case MEMBER_BIT_SIZE:
                    return type.getBitSize();
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class VarBitInteger extends LLVMTypeInteropWrapper<VariableBitWidthType> {

        VarBitInteger(VariableBitWidthType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        private static final String MEMBER_BIT_SIZE = "getBitSize";

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys(MEMBER_BIT_SIZE);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            return MEMBER_BIT_SIZE.equals(member) || isDefaultMember(member);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            assert member != null;
            switch (member) {
                case MEMBER_IS_INTEGER:
                    return true;
                case MEMBER_BIT_SIZE:
                    return type.getBitSize();
                default:
                    return readDefaultMember(member);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class Void extends LLVMTypeInteropWrapper<VoidType> {

        Void(VoidType type, DataLayout dataLayout) {
            super(type, dataLayout);
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean hasMembers() {
            return true;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public LLVMKeysObject getMembers(@SuppressWarnings("unused") boolean includeInternal) {
            return getDefaultTypeKeys();
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        public boolean isMemberReadable(String member) {
            return isDefaultMember(member);
        }

        @ExportMessage
        public Object readMember(String member) throws UnknownIdentifierException {
            if (MEMBER_IS_VOID.equals(member)) {
                return true;
            } else {
                return readDefaultMember(member);
            }
        }
    }
}
