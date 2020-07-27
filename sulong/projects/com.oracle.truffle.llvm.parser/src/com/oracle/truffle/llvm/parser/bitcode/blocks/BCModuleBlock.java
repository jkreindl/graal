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

import com.oracle.truffle.llvm.parser.model.IRScope;
import com.oracle.truffle.llvm.parser.model.ModelModule;
import com.oracle.truffle.llvm.parser.model.ValueSymbol;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesCodeEntry;
import com.oracle.truffle.llvm.parser.model.enums.Linkage;
import com.oracle.truffle.llvm.parser.model.enums.Visibility;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalAlias;
import com.oracle.truffle.llvm.parser.model.symbols.globals.GlobalVariable;
import com.oracle.truffle.llvm.parser.model.target.TargetDataLayout;
import com.oracle.truffle.llvm.parser.model.target.TargetTriple;
import com.oracle.truffle.llvm.parser.text.LLSourceBuilder;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.Type;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

final class BCModuleBlock extends BCBlockParser {

    static final int BLOCK_ID = 8;

    private final ModelModule module;
    private final StringTable stringTable;
    private final IRScope scope;
    private final LLSourceBuilder llSource;

    private final ParameterAttributes paramAttributes;
    private final ArrayDeque<FunctionDefinition> functionQueue;
    private final AtomicInteger index;

    private int mode;
    private Type[] types;

    BCModuleBlock(ModelModule module, StringTable stringTable, IRScope scope, LLSourceBuilder llSource) {
        this.module = module;
        this.stringTable = stringTable;
        this.scope = scope;
        this.llSource = llSource;

        this.paramAttributes = new ParameterAttributes();
        this.functionQueue = new ArrayDeque<>();
        this.index = new AtomicInteger(0);

        this.mode = 1;
        this.types = null;
    }

    private static final int MODULE_VERSION = 1;
    private static final int MODULE_TARGET_TRIPLE = 2;
    private static final int MODULE_TARGET_DATALAYOUT = 3;
    // private static final int MODULE_ASM = 4;
    // private static final int MODULE_SECTION_NAME = 5;
    // private static final int MODULE_DEPLIB = 6;
    private static final int MODULE_GLOBAL_VARIABLE = 7;
    private static final int MODULE_FUNCTION = 8;
    private static final int MODULE_ALIAS_OLD = 9;
    // private static final int MODULE_PURGE_VALUES = 10;
    // private static final int MODULE_GC_NAME = 11;
    // private static final int MODULE_COMDAT = 12;
    // private static final int MODULE_VSTOFFSET = 13;
    private static final int MODULE_ALIAS = 14;
    // private static final int MODULE_METADATA_VALUES = 15;
    // private static final int MODULE_SOURCE_FILENAME = 16;
    // private static final int MODULE_CODE_HASH = 17;
    // private static final int MODULE_CODE_IFUNC = 18;

    @Override
    void parseRecord(LLVMBitcodeRecord record) {
        switch (record.getId()) {
            case MODULE_VERSION:
                mode = record.readInt();
                break;

            case MODULE_TARGET_TRIPLE:
                module.addTargetInformation(new TargetTriple(record.readString()));
                break;

            case MODULE_TARGET_DATALAYOUT:
                final TargetDataLayout layout = TargetDataLayout.fromString(record.readString());
                module.setTargetDataLayout(layout);
                break;

            case MODULE_GLOBAL_VARIABLE:
                createGlobalVariable(record);
                break;

            case MODULE_FUNCTION:
                createFunction(record);
                break;

            case MODULE_ALIAS:
                createGlobalAliasNew(record);
                break;
            case MODULE_ALIAS_OLD:
                createGlobalAliasOld(record);
                break;

            default:
                break;
        }
    }

    @Override
    BCBlockParser getParserForSubblock(BCBlockScanner.ScannerData scannerData, int blockId) {
        switch (blockId) {
            case BCConstantsBlock.BLOCK_ID:
                return new BCConstantsBlock(types, scope);

            case BCFunctionBlock.BLOCK_ID: {
                final FunctionDefinition definition = functionQueue.removeFirst();
                final LazyBitcodeFunctionBlockParser lazyParser = new LazyBitcodeFunctionBlockParser(scannerData, llSource, definition, scope, types, mode, paramAttributes);
                module.addFunctionParser(definition, lazyParser);
                return null;
            }

            case BCTypeBlock.BLOCK_ID:
                return new BCTypeBlock(this);

            case BCValueSymTabBlock.BLOCK_ID:
                return new BCValueSymTabBlock(scope);

            case BCStrTabBlock.BLOCK_ID:
                return new BCStrTabBlock(stringTable);

            case BCParamBlocks.BLOCK_ID_PARAMATTR:
            case BCParamBlocks.BLOCK_ID_PARAMATTR_GROUP:
                return new BCParamBlocks(types, paramAttributes);

            case BCMetadataBlock.BLOCK_ID_METADATA:
            case BCMetadataBlock.BLOCK_ID_METADATA_KIND:
                return new BCMetadataBlock(scope, types);

            default:
                return super.getParserForSubblock(scannerData, blockId);
        }
    }

    ModelModule getModule() {
        return module;
    }

    void setTypes(Type[] types) {
        assert types != null;
        assert Arrays.stream(types).noneMatch(Objects::isNull);
        this.types = types;
    }

    // private static final int STRTAB_RECORD_OFFSET = 2;
    // private static final int STRTAB_RECORD_OFFSET_INDEX = 0;
    // private static final int STRTAB_RECORD_LENGTH_INDEX = 1;

    private boolean useStrTab() {
        return mode == 2;
    }

    private long readNameFromStrTab(LLVMBitcodeRecord record) {
        if (useStrTab()) {
            final int offset = record.readInt();
            final int length = record.readInt();
            return offset | (((long) length) << 32);
        } else {
            return 0;
        }
    }

    private void assignNameFromStrTab(long name, ValueSymbol target) {
        if (useStrTab()) {
            final int offset = (int) (name & 0xFFFFFFFFL);
            final int length = (int) (name >> 32);
            stringTable.requestName(offset, length, target);
        }
    }

    // private static final int FUNCTION_TYPE = 0;
    // private static final int FUNCTION_ISPROTOTYPE = 2;
    // private static final int FUNCTION_LINKAGE = 3;
    // private static final int FUNCTION_PARAMATTR = 4;
    // private static final int FUNCTION_VISIBILITY = 7;

    private void createFunction(LLVMBitcodeRecord record) {
        long name = readNameFromStrTab(record);
        Type type = types[record.readInt()];
        if (type instanceof PointerType) {
            type = ((PointerType) type).getPointeeType();
        }

        record.skip();
        final FunctionType functionType = BCTypeBlock.castToFunction(type);
        final boolean isPrototype = record.readBoolean();
        final Linkage linkage = Linkage.decode(record.read());

        final AttributesCodeEntry paramAttr = paramAttributes.getCodeEntry(record.read());
        record.skip();
        record.skip();

        Visibility visibility = Visibility.DEFAULT;
        if (record.remaining() > 0) {
            visibility = Visibility.decode(record.read());
        }

        if (isPrototype) {
            final FunctionDeclaration function = new FunctionDeclaration(functionType, linkage, paramAttr, index.getAndIncrement());
            module.addFunctionDeclaration(function);
            scope.addSymbol(function, function.getType());
            assignNameFromStrTab(name, function);
        } else {
            final FunctionDefinition function = new FunctionDefinition(functionType, linkage, visibility, paramAttr, index.getAndIncrement());
            module.addFunctionDefinition(function);
            scope.addSymbol(function, function.getType());
            assignNameFromStrTab(name, function);
            functionQueue.addLast(function);
        }
    }

    // private static final int GLOBALVAR_TYPE = 0;
    // private static final int GLOBALVAR_FLAGS = 1;
    private static final long GLOBALVAR_EXPLICICTTYPE_MASK = 0x2;
    private static final long GLOBALVAR_ISCONSTANT_MASK = 0x1;
    // private static final int GLOBALVAR_INTITIALIZER = 2;
    // private static final int GLOBALVAR_LINKAGE = 3;
    // private static final int GLOBALVAR_ALIGN = 4;
    // private static final int GLOBALVAR_VISIBILITY = 6;

    private void createGlobalVariable(LLVMBitcodeRecord record) {
        long name = readNameFromStrTab(record);
        final int typeField = record.readInt();
        final long flagField = record.read();

        Type type = types[typeField];
        if ((flagField & GLOBALVAR_EXPLICICTTYPE_MASK) != 0) {
            type = new PointerType(type);
        }

        final boolean isConstant = (flagField & GLOBALVAR_ISCONSTANT_MASK) != 0;
        final int initialiser = record.readInt();
        final long linkage = record.read();
        final int align = record.readInt();
        record.skip();

        final long visibility = record.remaining() > 0 ? record.read() : Visibility.DEFAULT.getEncodedValue();

        final GlobalVariable global = GlobalVariable.create(isConstant, (PointerType) type, align, linkage, visibility, scope.getSymbols(), initialiser, index.getAndIncrement());
        assignNameFromStrTab(name, global);
        module.addGlobalVariable(global);
        scope.addSymbol(global, global.getType());
    }

    // private static final int GLOBALALIAS_TYPE = 0;
    // private static final int GLOBALALIAS_NEW_VALUE = 2;
    // private static final int GLOBALALIAS_NEW_LINKAGE = 3;

    private void createGlobalAliasNew(LLVMBitcodeRecord record) {
        final long name = readNameFromStrTab(record);
        final PointerType type = new PointerType(types[record.readInt()]);

        record.skip(); // idx = 1 is address space information
        final int value = record.readInt();
        final long linkage = record.read();

        final GlobalAlias global = GlobalAlias.create(type, linkage, Visibility.DEFAULT.ordinal(), scope.getSymbols(), value);
        assignNameFromStrTab(name, global);
        module.addAlias(global);
        scope.addSymbol(global, global.getType());
    }

    // private static final int GLOBALALIAS_OLD_VALUE = 1;
    // private static final int GLOBALALIAS_OLD_LINKAGE = 2;

    private void createGlobalAliasOld(LLVMBitcodeRecord record) {
        long name = readNameFromStrTab(record);
        final PointerType type = BCTypeBlock.castToPointer(types[record.readInt()]);
        final int value = record.readInt();
        final long linkage = record.read();

        final GlobalAlias global = GlobalAlias.create(type, linkage, Visibility.DEFAULT.ordinal(), scope.getSymbols(), value);
        assignNameFromStrTab(name, global);
        module.addAlias(global);
        scope.addSymbol(global, global.getType());
    }

}
