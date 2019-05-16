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

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.llvm.runtime.datalayout.DataLayout;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType.PrimitiveKind;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

import java.util.IdentityHashMap;

public abstract class Type {

    @CompilationFinal private int byteSize = -1;
    @CompilationFinal private int byteAlignment = -1;

    public static final Type[] EMPTY_ARRAY = {};

    /**
     * Initialize size and alignment properties of this type after it has been fully parsed. Also
     * initializes all types referenced by this type. Type structures with circular dependencies are
     * supported, and each contained type is only initialized once.
     *
     * @param targetDataLayout the data layout to use for determining the size
     * @param previouslyInitialized a set of types that are currently being or have already been
     *            initialized
     */
    protected abstract void initialize(DataLayout targetDataLayout, IdentityHashMap<Type, Void> previouslyInitialized);

    /**
     * Set the size and alignment properties of this type.
     *
     * @param byteSize the size of values of this type in number of bytes
     * @param byteAlignment the byte-alignment of values of this type
     */
    protected void setInitializedProperties(int byteSize, int byteAlignment) {
        CompilerAsserts.neverPartOfCompilation("Type must be initialized before compilation");
        this.byteSize = byteSize;
        this.byteAlignment = byteAlignment;
    }

    /**
     * Clear any computed size and alignment properties. This is necessary after a data-layout
     * change. In execution, this means that type constants, e.g. {@link PrimitiveType#I32}, will
     * always have the size and alignment specified in the last data-layout parsed. This action is
     * not transitively applied to contained types.
     */
    private void clearInitialization() {
        byteSize = -1;
        byteAlignment = -1;
    }

    boolean isInitialized() {
        return byteSize != -1 && byteAlignment != -1;
    }

    public abstract int getBitSize();

    public abstract void accept(TypeVisitor visitor);

    /**
     * Get the byte-size of values of this type. This type and all types referenced by it must have
     * been {@link Initializer initialized} before this method can be used.
     *
     * @return the size in number of bytes
     *
     * @throws IllegalStateException if this type was not properly initialized
     */
    public int getByteSize() {
        if (!isInitialized()) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Size of type was not initialized");
        }

        return byteSize;
    }

    /**
     * Get the byte-alignment of values of this type. This type and all types referenced by it must
     * have been {@link Initializer initialized} before this method can be used.
     *
     * @return the byte-alignment
     *
     * @throws IllegalStateException if this type was not properly initialized
     */
    public int getByteAlignment() {
        if (!isInitialized()) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException("Alignment of type was not initialized");
        }

        return byteAlignment;
    }

    public abstract Type shallowCopy();

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

    public static boolean isFunctionOrFunctionPointer(Type type) {
        return type instanceof FunctionType || (type instanceof PointerType && ((PointerType) type).getPointeeType() instanceof FunctionType);
    }

    public static Type createConstantForType(Type type, Object value) {
        if (type instanceof PrimitiveType) {
            return new PrimitiveType(((PrimitiveType) type).getPrimitiveKind(), value);
        } else {
            return new VariableBitWidthType(type.getBitSize(), value);
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
        final long padding = alignment == 0 ? 0 : (alignment - (offset % alignment)) % alignment;
        assert padding == (int) padding;
        return (int) padding;
    }

    public static int getPadding(long offset, Type type) {
        final int alignment = type.getByteAlignment();
        return getPadding(offset, alignment);
    }

    /**
     * Class to initialize any number of {@link Type Type} objects.
     */
    public static final class Initializer {

        private final DataLayout targetDataLayout;
        private final IdentityHashMap<Type, Void> previouslyInitialized;

        /**
         * Constructor setting up the data layout to use for determining the size and alignment
         * properties of types to initialize.
         *
         * @param targetDataLayout the data layout containing target-specific size and alignment
         *            information
         */
        public Initializer(DataLayout targetDataLayout) {
            this.targetDataLayout = targetDataLayout;
            this.previouslyInitialized = new IdentityHashMap<>();
        }

        /**
         * Initialize the given type and all types referenced by it.
         *
         * @param type the type to initialize
         */
        public void initializeType(Type type) {
            type.initialize(targetDataLayout, previouslyInitialized);
        }

        /**
         * Clear size and alignment properties of the given type only.
         *
         * @param type the type to un-initialize
         */
        public static void clearTypeInitialization(Type type) {
            type.clearInitialization();
        }
    }
}
