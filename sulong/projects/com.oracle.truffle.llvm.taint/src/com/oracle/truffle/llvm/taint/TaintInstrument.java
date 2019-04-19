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
package com.oracle.truffle.llvm.taint;

import java.util.HashSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventListener;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;

@TruffleInstrument.Registration(id = "taint", name = "taint", services = TaintInstrument.class)
public class TaintInstrument extends TruffleInstrument {

    @Option(name = "", help = "Enable a Simple Taint Tracking Instrument", category = OptionCategory.USER) //
    static final OptionKey<Boolean> Taint = new OptionKey<>(false);

    private static final HashSet<Object> taintedObjects = new HashSet<>();

    @TruffleBoundary
    public static void taintValue(Object value) {
        taintedObjects.add(value);
    }

    @TruffleBoundary
    public static boolean isTainted(Object value) {
        return taintedObjects.contains(value);
    }

    @Override
    protected void onCreate(Env env) {
        final SourceSectionFilter filter = SourceSectionFilter.newBuilder().build();
        env.getInstrumenter().attachExecutionEventListener(filter, new ExecutionEventListener() {
            @Override
            public void onEnter(EventContext context, VirtualFrame frame) {

            }

            @Override
            public void onReturnValue(EventContext context, VirtualFrame frame, Object result) {

            }

            @Override
            public void onReturnExceptional(EventContext context, VirtualFrame frame, Throwable exception) {

            }
        });
        env.registerService(this);
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new TaintInstrumentOptionDescriptors();
    }
}
