/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates.
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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.llvm.runtime.datalayout.DataLayout;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.OpaqueType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.visitors.TypeVisitor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class BCTypeBlock extends BCBlockParser {

    static final int BLOCK_ID = 17;

    private static final int TYPE_NUMBER_OF_ENTRIES = 1;
    private static final int TYPE_VOID = 2;
    private static final int TYPE_FLOAT = 3;
    private static final int TYPE_DOUBLE = 4;
    private static final int TYPE_LABEL = 5;
    private static final int TYPE_OPAQUE = 6;
    private static final int TYPE_INTEGER = 7;
    private static final int TYPE_POINTER = 8;
    private static final int TYPE_FUNCTION_OLD = 9;
    private static final int TYPE_HALF = 10;
    private static final int TYPE_ARRAY = 11;
    private static final int TYPE_VECTOR = 12;
    private static final int TYPE_X86_FP80 = 13;
    private static final int TYPE_FP128 = 14;
    private static final int TYPE_PPC_FP128 = 15;
    private static final int TYPE_METADATA = 16;
    private static final int TYPE_X86_MMX = 17;
    private static final int TYPE_STRUCT_ANON = 18;
    private static final int TYPE_STRUCT_NAME = 19;
    private static final int TYPE_STRUCT_NAMED = 20;
    private static final int TYPE_FUNCTION = 21;
    private static final int TYPE_TOKEN = 22;

    private final BCModuleBlock moduleBlockParser;

    private Type[] table;
    private String structName;
    private int size;

    BCTypeBlock(BCModuleBlock moduleBlockParser) {
        this.moduleBlockParser = moduleBlockParser;
        this.table = Type.EMPTY_ARRAY;
        this.structName = null;
        this.size = 0;
    }

    @Override
    void parseRecord(LLVMBitcodeRecord record) {
        Type type;
        int id = record.getId();

        switch (id) {
            case TYPE_NUMBER_OF_ENTRIES:
                table = new Type[record.readInt()];
                return;

            case TYPE_VOID:
                type = VoidType.INSTANCE;
                break;

            case TYPE_FLOAT:
                type = PrimitiveType.FLOAT;
                break;

            case TYPE_DOUBLE:
                type = PrimitiveType.DOUBLE;
                break;

            case TYPE_LABEL:
                type = MetaType.LABEL;
                break;

            case TYPE_OPAQUE:
                if (structName != null) {
                    type = new OpaqueType(structName);
                    structName = null;
                    moduleBlockParser.getModule().addGlobalType(type);
                } else {
                    type = new OpaqueType();
                }
                break;

            case TYPE_INTEGER:
                type = Type.getIntegerType(record.readInt());
                break;

            case TYPE_POINTER: {
                final PointerType pointerType = new PointerType(null);
                setType(record.readInt(), pointerType::setPointeeType);
                type = pointerType;
                break;
            }
            case TYPE_FUNCTION_OLD: {
                boolean isVarargs = record.readBoolean();
                record.skip();
                int index = record.readInt();
                int numArguments = record.remaining();
                final FunctionType functionType = new FunctionType(null, numArguments, isVarargs);
                setTypes(record, numArguments, functionType::setArgumentType);
                setType(index, functionType::setReturnType);
                type = functionType;
                break;
            }
            case TYPE_HALF:
                type = PrimitiveType.HALF;
                break;

            case TYPE_ARRAY: {
                final ArrayType arrayType = new ArrayType(null, record.read());
                setType(record.readInt(), arrayType::setElementType);
                type = arrayType;
                break;
            }

            case TYPE_VECTOR: {
                final VectorType vectorType = new VectorType(null, record.readInt());
                setType(record.readInt(), vectorType::setElementType);
                type = vectorType;
                break;
            }

            case TYPE_X86_FP80:
                type = PrimitiveType.X86_FP80;
                break;

            case TYPE_FP128:
                type = PrimitiveType.F128;
                break;

            case TYPE_PPC_FP128:
                type = PrimitiveType.PPC_FP128;
                break;

            case TYPE_METADATA:
                type = MetaType.METADATA;
                break;

            case TYPE_X86_MMX:
                type = MetaType.X86MMX;
                break;

            case TYPE_STRUCT_NAME: {
                structName = record.readString();
                return;
            }

            case TYPE_STRUCT_ANON:
            case TYPE_STRUCT_NAMED: {
                final boolean isPacked = record.readBoolean();
                int numMembers = record.remaining();
                StructureType structureType;
                if (structName != null) {
                    structureType = new StructureType(structName, isPacked, numMembers);
                    structName = null;
                    moduleBlockParser.getModule().addGlobalType(structureType);
                } else {
                    structureType = new StructureType(isPacked, numMembers);
                }
                setTypes(record, numMembers, structureType::setElementType);
                type = structureType;
                break;
            }
            case TYPE_FUNCTION: {
                boolean isVarargs = record.readBoolean();
                int index = record.readInt();
                int numArguments = record.remaining();
                FunctionType functionType = new FunctionType(null, numArguments, isVarargs);
                setTypes(record, numArguments, functionType::setArgumentType);
                setType(index, functionType::setReturnType);
                type = functionType;
                break;
            }

            case TYPE_TOKEN:
                type = MetaType.TOKEN;
                break;

            default:
                type = MetaType.UNKNOWN;
                break;
        }

        if (table[size] != null) {
            ((UnresolvedType) table[size]).dependent.accept(type);
        }
        table[size++] = type;
    }

    @Override
    void onExit() {
        moduleBlockParser.setTypes(table);
    }

    private void setType(int typeIndex, Consumer<Type> typeFieldSetter) {
        if (typeIndex < size) {
            typeFieldSetter.accept(table[typeIndex]);

        } else if (table[typeIndex] == null) {
            table[typeIndex] = new UnresolvedType(typeFieldSetter);

        } else {
            ((UnresolvedType) table[typeIndex]).addDependent(typeFieldSetter);
        }
    }

    void setTypes(LLVMBitcodeRecord record, int numTypes, BiConsumer<Integer, Type> indexSetter) {
        assert numTypes == record.remaining();
        for (int i = 0; i < numTypes; i++) {
            final int typeIndex = record.readInt();
            if (typeIndex < size) {
                indexSetter.accept(i, table[typeIndex]);
            } else {
                final Consumer<Type> setter = new MemberDependent(i, indexSetter);
                if (table[typeIndex] == null) {
                    table[typeIndex] = new UnresolvedType(setter);
                } else {
                    ((UnresolvedType) table[typeIndex]).addDependent(setter);
                }
            }
        }
    }

    private static final class MemberDependent implements Consumer<Type> {

        private final int index;
        private final BiConsumer<Integer, Type> setter;

        private MemberDependent(int index, BiConsumer<Integer, Type> setter) {
            this.index = index;
            this.setter = setter;
        }

        @Override
        public void accept(Type type) {
            setter.accept(index, type);
        }
    }

    private static final class UnresolvedType extends Type {

        private Consumer<Type> dependent;

        UnresolvedType(Consumer<Type> dependent) {
            this.dependent = dependent;
        }

        private void addDependent(Consumer<Type> newDependent) {
            dependent = dependent.andThen(newDependent);
        }

        @Override
        public void accept(TypeVisitor visitor) {
            CompilerDirectives.transferToInterpreter();
            throw new LLVMParserException("Unresolved Forward-Referenced Type!");
        }

        @Override
        public long getBitSize() {
            CompilerDirectives.transferToInterpreter();
            throw new LLVMParserException("Unresolved Forward-Referenced Type!");
        }

        @Override
        public int getAlignment(DataLayout targetDataLayout) {
            CompilerDirectives.transferToInterpreter();
            throw new LLVMParserException("Unresolved Forward-Referenced Type!");
        }

        @Override
        public long getSize(DataLayout targetDataLayout) {
            CompilerDirectives.transferToInterpreter();
            throw new LLVMParserException("Unresolved Forward-Referenced Type!");
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        @Override
        public int hashCode() {
            return 0;
        }

    }

    @Override
    public String toString() {
        return "Typetable (size: " + table.length + ", currentIndex: " + size + ")";
    }

    static AggregateType castToAggregate(Type type) {
        if (type instanceof AggregateType) {
            return (AggregateType) type;
        } else {
            throw new LLVMParserException("Expected AggregateType, but received: " + type);
        }
    }

    static FunctionType castToFunction(Type type) {
        if (type instanceof FunctionType) {
            return (FunctionType) type;
        } else {
            throw new LLVMParserException("Expected FunctionType, but received: " + type);
        }
    }

    static PointerType castToPointer(Type type) {
        if (type instanceof PointerType) {
            return (PointerType) type;
        } else {
            throw new LLVMParserException("Expected PointerType, but received: " + type);
        }
    }

    static VectorType castToVector(Type type) {
        if (type instanceof VectorType) {
            return (VectorType) type;
        } else {
            throw new LLVMParserException("Expected VectorType, but received: " + type);
        }
    }
}
