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
package com.oracle.truffle.llvm.runtime.instruments;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.LLVMIVarBitLarge;
import com.oracle.truffle.llvm.runtime.LLVMIVarBitSmall;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMTypes;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;

@TypeSystemReference(LLVMTypes.class)
abstract class PrintValueNode extends Node {

    final boolean hideNativePointers;

    PrintValueNode(boolean hideNativePointers) {
        this.hideNativePointers = hideNativePointers;
    }

    abstract String executeWithTarget(Object value);

    @Specialization
    protected String doSmallIVarBit(LLVMIVarBitSmall value) {
        return String.valueOf(value.getLongValue());
    }

    @Specialization
    @TruffleBoundary
    protected String doLargeIVarBit(LLVMIVarBitLarge value) {
        return value.asBigInteger().toString();
    }

    @Specialization
    protected String doLLVMFunctionDescriptor(LLVMFunctionDescriptor value) {
        return value.getName();
    }

    protected static PrintValueNode createRecursive(boolean hideNativePointers) {
        return PrintValueNodeGen.create(hideNativePointers);
    }

    @Specialization
    protected String doLLVMManagedPointer(LLVMManagedPointer value, @Cached("createRecursive(hideNativePointers)") PrintValueNode childFormatter) {
        final String target = childFormatter.executeWithTarget(value.getObject());

        final long offset = value.getOffset();
        if (offset == 0L) {
            return "Managed Pointer(target = \'" + target + "\')";
        } else {
            return "Managed Pointer(target = \'" + target + "\', offset = " + offset + ")";
        }
    }

    @Specialization
    protected String doLLVMNativePointer(LLVMNativePointer value) {
        if (hideNativePointers) {
            return "Native Pointer";
        } else {
            return formatNativePointer(value.asNative());
        }
    }

    @TruffleBoundary
    private static String formatNativePointer(long value) {
        return String.format("0x%x", value);
    }

    protected static boolean hasCheckedToString(Object value) {
        return !(value instanceof LLVMIVarBit) && !(value instanceof LLVMFunctionDescriptor) && !LLVMManagedPointer.isInstance(value);
    }

    @Specialization(guards = "hasCheckedToString(value)")
    protected String doGeneric(Object value) {
        return String.valueOf(value);
    }
}
