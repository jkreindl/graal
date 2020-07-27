/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates.
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

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.parser.LLVMParserRuntime;
import com.oracle.truffle.llvm.parser.metadata.debuginfo.DebugInfoFunctionProcessor;
import com.oracle.truffle.llvm.parser.model.IRScope;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.parser.text.LLSourceBuilder;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;
import com.oracle.truffle.llvm.runtime.types.Type;

/**
 * Parser for the body of a bitcode function. Function bodies are only parsed after the module scope
 * has been parsed fully and may be parsed asynchronously.
 */
public final class LazyBitcodeFunctionBlockParser {

    private final LLSourceBuilder llSource;
    private final FunctionDefinition function;
    private final IRScope moduleScope;
    private final BCFunctionBlock fullBlockParser;
    private final ParseLinkageName.LinkageNameFunctionParser subprogramNameParser;
    private final BCBlockScanner.ScannerData scannerData;

    private boolean isParsed;

    LazyBitcodeFunctionBlockParser(BCBlockScanner.ScannerData scannerData, LLSourceBuilder llSource, FunctionDefinition function, IRScope moduleScope, Type[] types, int mode,
                    ParameterAttributes paramAttributes) {
        this.llSource = llSource;
        this.function = function;
        this.moduleScope = moduleScope;
        this.fullBlockParser = new BCFunctionBlock(function, moduleScope, types, mode, paramAttributes);
        this.subprogramNameParser = new ParseLinkageName.LinkageNameFunctionParser(function, moduleScope);
        this.scannerData = scannerData;
        this.isParsed = false;

        assert function != null;
        assert moduleScope != null;
    }

    /**
     * Parses the entire function block.
     *
     * @param diProcessor processor for function-local debug information
     * @param bitcodeSource the bitcode file containing the function to parse
     * @param runtime parser context
     */
    public void parse(DebugInfoFunctionProcessor diProcessor, Source bitcodeSource, LLVMParserRuntime runtime) {
        synchronized (moduleScope) {
            if (!isParsed) {
                BCBlockScanner.scanBlock(scannerData, BCFunctionBlock.BLOCK_ID, fullBlockParser);
                diProcessor.process(function, moduleScope, bitcodeSource, runtime.getContext());
                if (runtime.getContext().getEnv().getOptions().get(SulongEngineOption.LL_DEBUG)) {
                    llSource.applySourceLocations(function, runtime);
                }
                isParsed = true;
            }
        }
    }

    /**
     * Attaches the source-level name of this function to its entry in the bitcode scope.
     *
     * @param runtime parser context
     */
    public void parseLinkageName(LLVMParserRuntime runtime) {
        synchronized (moduleScope) {
            try {
                BCBlockScanner.scanBlock(scannerData, BCFunctionBlock.BLOCK_ID, subprogramNameParser);
            } catch (ParseLinkageName.ParsedLinkageName e) {
                /*
                 * If linkageName/displayName is found, an exception is thrown (such that
                 * parsing/searching does not have to be continued).
                 */
                final String displayName = e.originalName;
                final String linkageName = e.linkageName;

                if (linkageName != null && runtime.getFileScope().getFunction(displayName) == null) {
                    runtime.getFileScope().registerLinkageName(displayName, linkageName);
                }
            }
        }
    }
}
