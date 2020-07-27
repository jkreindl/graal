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
package com.oracle.truffle.llvm.parser.metadata;

import com.oracle.truffle.llvm.parser.model.IRScope;
import com.oracle.truffle.llvm.runtime.types.Type;

public final class MDTemplateTypeParameter extends MDNamedLocation implements MDBaseNode {

    private final long column;

    private MDBaseNode baseType;

    private MDTemplateTypeParameter(long line, long column) {
        super(line);
        this.column = column;

        this.baseType = MDVoidNode.INSTANCE;
    }

    @Override
    public void accept(MetadataVisitor visitor) {
        visitor.visit(this);
    }

    public MDBaseNode getBaseType() {
        return baseType;
    }

    public long getColumn() {
        return column;
    }

    @Override
    public void replace(MDBaseNode oldValue, MDBaseNode newValue) {
        super.replace(oldValue, newValue);
        if (baseType == oldValue) {
            baseType = newValue;
        }
    }

    private static final int ARGINDEX_SCOPE = 1;
    private static final int ARGINDEX_NAME = 2;
    private static final int ARGINDEX_TYPE = 3;
    private static final int ARGINDEX_FILE = 4;
    private static final int ARGINDEX_LINE = 5;
    private static final int ARGINDEX_COLUMN = 6;

    public static MDTemplateTypeParameter create32(long[] args, Type[] types, IRScope scope) {
        final long line = ParseUtil.asLong(args, ARGINDEX_LINE, types, scope);
        final long column = ParseUtil.asLong(args, ARGINDEX_COLUMN, types, scope);

        final MDTemplateTypeParameter parameter = new MDTemplateTypeParameter(line, column);

        parameter.setScope(ParseUtil.resolveReference(args, ARGINDEX_SCOPE, parameter, types, scope));
        parameter.baseType = ParseUtil.resolveReference(args, ARGINDEX_TYPE, parameter, types, scope);
        parameter.setFile(ParseUtil.resolveReference(args, ARGINDEX_FILE, parameter, types, scope));
        parameter.setName(ParseUtil.resolveReference(args, ARGINDEX_NAME, parameter, types, scope));

        return parameter;
    }
}
