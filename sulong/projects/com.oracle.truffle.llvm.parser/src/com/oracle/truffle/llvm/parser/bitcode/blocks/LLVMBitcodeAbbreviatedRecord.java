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

import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

/**
 * Parser for a component of a {@link LLVMBitcodeRecord record} in an LLVM bitcode file. Each block
 * in an LLVM bitcode file may contain a number of records, which each may contain multiple entries.
 * The bitcode file can itself define specialized parsers for such records (see
 * https://llvm.org/docs/BitCodeFormat.html#data-records). These specialized parsers may themselves
 * contain specialized parsers for these record components. Specialized records are called
 * "abbreviated records", and the corresponding parsers are called "abbreviation definitions".
 */
abstract class LLVMBitcodeAbbreviatedRecord {

    private static final int ABBREVIATED_RECORD_OPERANDS_WIDTH = 5;
    private static final int USER_OPERAND_LITERAL_WIDTH = 8;
    private static final int USER_OPERAND_LITERALBIT_WIDTH = 1;
    private static final int USER_OPERAND_TYPE_WIDTH = 3;
    private static final int USER_OPERAND_DATA_WIDTH = 5;

    private static final int DEFINITION_TYPE_FIXED = 1;
    private static final int DEFINITION_TYPE_VBR = 2;
    private static final int DEFINITION_TYPE_ARRAY = 3;
    private static final int DEFINITION_TYPE_CHAR6 = 4;
    private static final int DEFINITION_TYPE_BLOB = 5;

    /**
     * Enter the value of this component into a provided buffer.
     *
     * @param reader the bitstream from which to read the component value
     * @param record the buffer into which to deposit the component value
     */
    abstract void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record);

    /**
     * Parses an abbreviation definition from the provided bitstream.
     *
     * @param reader the bitstream to parse from
     * @return the newly defined record parser
     */
    static LLVMBitcodeAbbreviatedRecord[] defineAbbreviation(LLVMBitcodeReader reader) {
        final long operandCount = reader.readVBR(ABBREVIATED_RECORD_OPERANDS_WIDTH);

        final LLVMBitcodeAbbreviatedRecord[] operandScanners = new LLVMBitcodeAbbreviatedRecord[(int) operandCount];

        boolean containsArrayOperand = false;
        for (int i = 0; i < operandCount; i++) {
            // first operand contains the record id

            final boolean isLiteral = reader.readFixed(USER_OPERAND_LITERALBIT_WIDTH) == 1;
            if (isLiteral) {
                final long fixedValue = reader.readVBR(USER_OPERAND_LITERAL_WIDTH);
                operandScanners[i] = ConstantAbbreviatedRecord.create(fixedValue);

            } else {

                final long recordType = reader.readFixed(USER_OPERAND_TYPE_WIDTH);

                switch ((int) recordType) {
                    case DEFINITION_TYPE_FIXED: {
                        final int width = Math.toIntExact(reader.readVBR(USER_OPERAND_DATA_WIDTH));
                        operandScanners[i] = FixedAbbreviatedRecord.create(width);
                        break;
                    }

                    case DEFINITION_TYPE_VBR: {
                        final int width = Math.toIntExact(reader.readVBR(USER_OPERAND_DATA_WIDTH));
                        operandScanners[i] = VBRAbbreviatedRecord.create(width);
                        break;
                    }

                    case DEFINITION_TYPE_ARRAY:
                        // arrays only occur as the second to last operand in an abbreviation, just
                        // before their element type
                        // then this can only be executed once for any abbreviation
                        containsArrayOperand = true;
                        break;

                    case DEFINITION_TYPE_CHAR6:
                        operandScanners[i] = Char6AbbreviatedRecord.INSTANCE;
                        break;

                    case DEFINITION_TYPE_BLOB:
                        operandScanners[i] = BlobAbbreviatedRecord.INSTANCE;
                        break;

                    default:
                        throw new LLVMParserException("Unknown ID in for record abbreviation: " + recordType);
                }
            }
        }

        if (containsArrayOperand) {
            final LLVMBitcodeAbbreviatedRecord elementScanner = operandScanners[operandScanners.length - 1];
            final LLVMBitcodeAbbreviatedRecord arrayScanner = new ArrayAbbreviatedRecord(elementScanner);
            operandScanners[operandScanners.length - 1] = arrayScanner;
        }

        return operandScanners;
    }

    /**
     * Provides a constant value.
     */
    private static final class ConstantAbbreviatedRecord extends LLVMBitcodeAbbreviatedRecord {

        /*
         * Record Ids are usually encoded using constant records. As a result, most constant-value
         * record parsers contain only the possible record ids. In practice, function blocks have
         * the highest number of records. At the time of this comment, the highest possible record
         * id was 56. As new record ids get added only rarely, we cache the numbers of the interval
         * [0, 59] to be safe for the foreseeable future. Since not all numbers in that interval are
         * actually used (due to, e.g., deprecation of certain instructions) we cache the ones that
         * are used rather than preallocating them all.
         */
        private static final ConstantAbbreviatedRecord[] CONSTANT_INSTANCES = new ConstantAbbreviatedRecord[60];

        private final long value;

        private ConstantAbbreviatedRecord(long value) {
            this.value = value;
        }

        static ConstantAbbreviatedRecord create(long value) {
            assert value >= 0;

            if (value < CONSTANT_INSTANCES.length) {
                final int idx = (int) value;
                ConstantAbbreviatedRecord parser = CONSTANT_INSTANCES[idx];
                if (parser == null) {
                    CONSTANT_INSTANCES[idx] = parser = new ConstantAbbreviatedRecord(value);
                }
                return parser;
            }

            return new ConstantAbbreviatedRecord(value);
        }

        @Override
        void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record) {
            record.addOp(value);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(value: " + value + ")";
        }
    }

    /**
     * Reads a fixed-width value of defined width from the bitstream.
     */
    private static final class FixedAbbreviatedRecord extends LLVMBitcodeAbbreviatedRecord {

        /*
         * Fixed-width record components seem to fall either below 14 bit width or at exactly 32
         * bits.
         */
        private static final FixedAbbreviatedRecord[] CACHED_INSTANCES = new FixedAbbreviatedRecord[14];
        private static final FixedAbbreviatedRecord INT_INSTANCE = new FixedAbbreviatedRecord(32);

        private final int width;

        private FixedAbbreviatedRecord(int width) {
            this.width = width;
        }

        static FixedAbbreviatedRecord create(int value) {
            /*
             * For some reason, bitcode files actually contain parsers for a 0-bit value. However,
             * while these are allocated, they are never actually executed.
             */
            assert value >= 0;

            if (value < CACHED_INSTANCES.length) {
                FixedAbbreviatedRecord parser = CACHED_INSTANCES[value];
                if (parser == null) {
                    CACHED_INSTANCES[value] = parser = new FixedAbbreviatedRecord(value);
                }
                return parser;
            } else if (value == INT_INSTANCE.width) {
                return INT_INSTANCE;
            }

            return new FixedAbbreviatedRecord(value);
        }

        @Override
        void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record) {
            record.addOp(reader.readFixed(width));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(width: " + width + ")";
        }
    }

    /**
     * Reads a variable-width value with components of defined width from the bitstream.
     */
    private static final class VBRAbbreviatedRecord extends LLVMBitcodeAbbreviatedRecord {

        /* Sulong's various test suites only ever allocate parsers for 4, 6, or 8-bit VBR values. */
        private static final VBRAbbreviatedRecord VBR4 = new VBRAbbreviatedRecord(4);
        private static final VBRAbbreviatedRecord VBR6 = new VBRAbbreviatedRecord(6);
        private static final VBRAbbreviatedRecord VBR8 = new VBRAbbreviatedRecord(8);

        private final int width;

        private VBRAbbreviatedRecord(int width) {
            this.width = width;
        }

        static VBRAbbreviatedRecord create(int value) {
            switch (value) {
                case 4:
                    return VBR4;
                case 6:
                    return VBR6;
                case 8:
                    return VBR8;
                default:
                    return new VBRAbbreviatedRecord(value);
            }
        }

        @Override
        void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record) {
            record.addOp(reader.readVBR(width));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(width: " + width + ")";
        }
    }

    /**
     * Reads a 6--bit character value from the bitstream.
     */
    private static final class Char6AbbreviatedRecord extends LLVMBitcodeAbbreviatedRecord {

        private static final Char6AbbreviatedRecord INSTANCE = new Char6AbbreviatedRecord();

        @Override
        void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record) {
            record.addOp(reader.readChar());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * Reads a blob, i.e., binary data of defined length, from the bitstream.
     */
    private static final class BlobAbbreviatedRecord extends LLVMBitcodeAbbreviatedRecord {

        private static final BlobAbbreviatedRecord INSTANCE = new BlobAbbreviatedRecord();

        private static final int BLOB_LENGTH_WIDTH = 6;
        private static final long MAX_BLOB_PART_LENGTH = Long.SIZE / USER_OPERAND_LITERAL_WIDTH;

        @Override
        void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record) {
            long blobLength = reader.readVBR(BLOB_LENGTH_WIDTH);
            reader.alignInt();
            record.ensureFits(blobLength / MAX_BLOB_PART_LENGTH);
            while (blobLength > 0) {
                final long l = Math.min(blobLength, MAX_BLOB_PART_LENGTH);
                final long blobValue = reader.readFixed((int) (USER_OPERAND_LITERAL_WIDTH * l));
                record.addOp(blobValue);
                blobLength -= l;
            }
            reader.alignInt();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    /**
     * Invokes another Element Parser a defined number of times.
     */
    private static final class ArrayAbbreviatedRecord extends LLVMBitcodeAbbreviatedRecord {

        private static final int USER_OPERAND_ARRAY_LENGTH_WIDTH = 6;

        private final LLVMBitcodeAbbreviatedRecord elementScanner;

        ArrayAbbreviatedRecord(LLVMBitcodeAbbreviatedRecord elementScanner) {
            assert elementScanner != null;
            this.elementScanner = elementScanner;
        }

        @Override
        void scan(LLVMBitcodeReader reader, LLVMBitcodeRecord record) {
            final long arrayLength = reader.readVBR(USER_OPERAND_ARRAY_LENGTH_WIDTH);
            record.ensureFits(arrayLength);
            for (int j = 0; j < arrayLength; j++) {
                elementScanner.scan(reader, record);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(elements: " + elementScanner.toString() + ")";
        }
    }

    private LLVMBitcodeAbbreviatedRecord() {
    }
}
