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
package trace;

import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.options.SulongEngineOption;
import com.oracle.truffle.llvm.tests.options.TestOptions;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public final class LLVMTraceTest {

    private static final Path BC_DIR_PATH = Paths.get(TestOptions.TEST_SUITE_PATH, "ir");
    private static final Path TRACE_DIR_PATH = Paths.get(TestOptions.PROJECT_ROOT, "..", "tests", "com.oracle.truffle.llvm.tests.irtrace.native", "trace");

    private static final String BC_FILE_NAME = "O0.bc";
    private static final String TRACE_FILE_EXTENSION = ".txt";

    // To reduce memory usage Sulong uses one single SourceSection for all IR-level instruction
    // nodes, which makes it impossible to discern which bitcode file a node belongs to. However, we
    // do not want to trace libSulong since it is platform specific. As a work-around we explicitly
    // specify the names of the functions to trace.
    private static final String[] TRACE_FUNCTIONS = new String[]{//
                    "main", //
                    "valueFunc", //
                    "voidFunc" //
    };

    private static final boolean RECORD_TRACE = false;

    private Context context;
    private OutputStream errStream;

    private final String testName;

    public LLVMTraceTest(String testName) {
        this.testName = testName;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> getConfigurations() {
        try (Stream<Path> dirs = Files.walk(BC_DIR_PATH)) {
            return dirs.filter(path -> path.endsWith(BC_FILE_NAME)).map(path -> new Object[]{path.getParent().getFileName().toString()}).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new AssertionError("Error while finding tests!", e);
        }
    }

    @Before
    public void before() {
        final Context.Builder contextBuilder = Context.newBuilder(LLVMLanguage.ID);
        contextBuilder.allowAllAccess(true);
        contextBuilder.allowExperimentalOptions(true);
        contextBuilder.option(SulongEngineOption.TRACE_IR_NAME, "stderr");
        contextBuilder.option(SulongEngineOption.TRACE_IR_FUNCTIONS_NAME, String.join(":", TRACE_FUNCTIONS));
        contextBuilder.option(SulongEngineOption.TRACE_IR_HIDE_NATIVE_POINTERS_NAME, String.valueOf(true));

        errStream = new ByteArrayOutputStream();
        contextBuilder.err(errStream);

        context = contextBuilder.build();
    }

    @Test
    public void test() throws IOException {
        final Path bcPath = BC_DIR_PATH.resolve(testName).resolve(BC_FILE_NAME);
        final Source source = Source.newBuilder(LLVMLanguage.ID, bcPath.toFile()).build();

        final Value sulongLibrary = context.eval(source);
        assert sulongLibrary.canInvokeMember("main");
        sulongLibrary.invokeMember("main");

        final String actualTrace = errStream.toString();
        final Path tracePath = TRACE_DIR_PATH.resolve(testName + TRACE_FILE_EXTENSION);
        if (RECORD_TRACE) {
            writeTrace(tracePath, actualTrace);
        } else {
            final String expectedString = readTrace(tracePath);
            assertEquals("Trace mismatch", expectedString, actualTrace);
        }
    }

    @After
    public void after() throws IOException {
        context.close();
        errStream.close();
    }

    private static String readTrace(Path tracePath) {
        try (Stream<String> lines = Files.lines(tracePath)) {
            return lines.collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read trace", e);
        }
    }

    private static void writeTrace(Path tracePath, String trace) {
        try {
            Files.write(tracePath, trace.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write trace", e);
        }
    }
}
