/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
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

import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.llvm.parser.metadata.MDBaseNode;
import com.oracle.truffle.llvm.parser.metadata.MDString;
import com.oracle.truffle.llvm.parser.metadata.MDVoidNode;
import com.oracle.truffle.llvm.parser.metadata.MetadataValueList;
import com.oracle.truffle.llvm.parser.model.IRScope;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_ATTACHMENT;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_BASIC_TYPE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_COMMON_BLOCK;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_COMPILE_UNIT;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_COMPOSITE_TYPE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_DERIVED_TYPE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_DISTINCT_NODE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_ENUMERATOR;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_EXPRESSION;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_FILE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_GENERIC_DEBUG;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_GLOBAL_DECL_ATTACHMENT;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_GLOBAL_VAR;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_GLOBAL_VAR_EXPR;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_IMPORTED_ENTITY;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_INDEX;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_INDEX_OFFSET;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_KIND;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_LABEL;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_LEXICAL_BLOCK;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_LEXICAL_BLOCK_FILE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_LOCAL_VAR;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_LOCATION;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_MACRO;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_MACRO_FILE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_MODULE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_NAME;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_NAMED_NODE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_NAMESPACE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_NODE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_OBJC_PROPERTY;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_OLD_FN_NODE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_OLD_NODE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_STRING;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_STRINGS;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_SUBPROGRAM;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_SUBRANGE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_SUBROUTINE_TYPE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_TEMPLATE_TYPE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_TEMPLATE_VALUE;
import static com.oracle.truffle.llvm.parser.bitcode.blocks.BCMetadataBlock.METADATA_VALUE;

final class ParseLinkageName {

    static final class LinkageNameFunctionParser extends BCBlockParser {

        private final FunctionDefinition function;
        private final IRScope scope;

        LinkageNameFunctionParser(FunctionDefinition function, IRScope scope) {
            this.function = function;
            this.scope = scope;
        }

        @Override
        void onEnter() {
            scope.startLocalScope(function);
        }

        @Override
        void parseRecord(LLVMBitcodeRecord record) {
            /*
             * We care only about metadata in this mode. We still need to scan the instruction
             * records so that we can actually find the beginning of the metadata block, but there
             * is no need to build up a symbol table here.
             */
        }

        @Override
        BCBlockParser getParserForSubblock(BCBlockScanner.ScannerData scannerData, int blockId) {
            /*
             * Since we care only about metadata, there is also no need to parse any other blocks
             * than the ones filling the metadata table.
             */
            switch (blockId) {
                case BCMetadataBlock.BLOCK_ID_METADATA:
                case BCMetadataBlock.BLOCK_ID_METADATA_ATTACHMENT:
                case BCMetadataBlock.BLOCK_ID_METADATA_KIND:
                    return new LinkageNameMetadataParser(scope);

                default:
                    return super.getParserForSubblock(scannerData, blockId);
            }
        }

        @Override
        void onExit() {
            // exit the local scope here in case we did not find a linkage name
            scope.exitLocalScope();
        }
    }

    static final class LinkageNameMetadataParser extends BCBlockParser {

        private final IRScope scope;

        LinkageNameMetadataParser(IRScope scope) {
            this.scope = scope;
        }

        @Override
        void onEnter() {
            // nothing to do here
        }

        @Override
        void parseRecord(LLVMBitcodeRecord record) {
            /*
             * We only want the name and the linkage name of the currently parsed function here.
             * However, we still need to build up a dummy metadata table so that the indices stored
             * in the MD_SUBPROGRAM record can be interpreted correctly. However, there is no need
             * to semantically parse any of the nodes other than the string here.
             */
            final MetadataValueList md = scope.getMetadata();
            switch (record.getId()) {
                case METADATA_STRING:
                    md.add(MDString.create(record));
                    break;

                case METADATA_STRINGS: {
                    // since llvm 3.9 all metadata strings are emitted as a single blob
                    final MDString[] strings = MDString.createFromBlob(record.getOps());
                    for (final MDString string : strings) {
                        md.add(string);
                    }
                    break;
                }

                case METADATA_SUBPROGRAM: {
                    // parse the (possibly forward referenced) name and linkage name fields
                    final LinkageName linkageName = new LinkageName();
                    final int originalNameIndex = LLVMBitcodeRecord.toUnsignedIntExact(record.getAt(2));
                    final int linkageNameIndex = LLVMBitcodeRecord.toUnsignedIntExact(record.getAt(3));
                    if (originalNameIndex != 0) {
                        md.onParse(originalNameIndex - 1, linkageName::setOriginalName);
                    }
                    if (linkageNameIndex != 0) {
                        md.onParse(linkageNameIndex - 1, linkageName::setLinkageName);
                    }
                    // if neither name is provided we just parse until the end of the block, maybe
                    // we get another function
                    md.add(MDVoidNode.INSTANCE);
                    break;
                }

                case METADATA_NAME:
                case METADATA_KIND:
                case METADATA_NAMED_NODE:
                case METADATA_ATTACHMENT:
                case METADATA_GLOBAL_DECL_ATTACHMENT:
                case METADATA_INDEX_OFFSET:
                case METADATA_INDEX:
                    break;

                case METADATA_VALUE:
                case METADATA_DISTINCT_NODE:
                case METADATA_NODE:
                case METADATA_LOCATION:
                case METADATA_OLD_NODE:
                case METADATA_OLD_FN_NODE:
                case METADATA_GENERIC_DEBUG:
                case METADATA_SUBRANGE:
                case METADATA_ENUMERATOR:
                case METADATA_BASIC_TYPE:
                case METADATA_FILE:
                case METADATA_SUBROUTINE_TYPE:
                case METADATA_LEXICAL_BLOCK:
                case METADATA_LEXICAL_BLOCK_FILE:
                case METADATA_LOCAL_VAR:
                case METADATA_NAMESPACE:
                case METADATA_GLOBAL_VAR:
                case METADATA_DERIVED_TYPE:
                case METADATA_COMPOSITE_TYPE:
                case METADATA_COMPILE_UNIT:
                case METADATA_TEMPLATE_TYPE:
                case METADATA_TEMPLATE_VALUE:
                case METADATA_EXPRESSION:
                case METADATA_OBJC_PROPERTY:
                case METADATA_IMPORTED_ENTITY:
                case METADATA_MODULE:
                case METADATA_MACRO:
                case METADATA_MACRO_FILE:
                case METADATA_GLOBAL_VAR_EXPR:
                case METADATA_COMMON_BLOCK:
                case METADATA_LABEL:
                    md.add(MDVoidNode.INSTANCE);
                    break;

                default:
                    md.add(null);
                    throw new LLVMParserException("Unsupported opCode in metadata block: " + record.getId());
            }
        }

        private final class LinkageName {

            private MDBaseNode originalName = null;
            private MDBaseNode linkageName = null;

            void setOriginalName(MDBaseNode node) {
                originalName = node;
                afterParse();
            }

            void setLinkageName(MDBaseNode node) {
                linkageName = node;
                afterParse();
            }

            private void afterParse() {
                if (originalName != null && linkageName != null) {
                    scope.exitLocalScope();
                    final String resolvedLinkageName = MDString.getIfInstance(linkageName);
                    final String resolvedOriginalName = MDString.getIfInstance(originalName);
                    throw new ParsedLinkageName(resolvedLinkageName, resolvedOriginalName);
                }
            }
        }

        @Override
        void onExit() {
            /*
             * We only end up here if the linkage name has not been found. In this case the function
             * block that is currently being parsed will take care of closing the local scope. No
             * need to do that here.
             */
        }
    }

    static class ParsedLinkageName extends ControlFlowException {

        private static final long serialVersionUID = 1L;
        final String linkageName;
        final String originalName;

        ParsedLinkageName(String linkageName, String originalName) {
            this.linkageName = linkageName;
            this.originalName = originalName;
        }
    }

    private ParseLinkageName() {
        // no instances
    }
}
