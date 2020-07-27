package com.oracle.truffle.llvm.parser.bitcode.blocks;

import com.oracle.truffle.llvm.runtime.except.LLVMParserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

final class BCBlockScanner {

    private static final int END_BLOCK = 0;
    private static final int ENTER_SUBBLOCK = 1;
    private static final int DEFINE_ABBREV = 2;
    private static final int UNABBREV_RECORD = 3;
    private static final int CUSTOM_ABBREV_OFFSET = 4;

    private static final int UNABBREVIATED_RECORD_ID_WIDTH = 6;
    private static final int UNABBREVIATED_RECORD_OPERAND_WIDTH = 6;
    private static final int UNABBREVIATED_RECORD_OPS_WIDTH = 6;

    private static final int SUBBLOCK_ID_WIDTH = 8;
    private static final int SUBBLOCK_ID_SIZE_WIDTH = 4;

    private static final Function<Long, ArrayList<LLVMBitcodeAbbreviatedRecord[]>> ABBREV_LIST_ALLOCATOR = k -> new ArrayList<>();

    /**
     * Invokes a supplied parser to parse all records and subblocks of a specific block an an llvm
     * bitstream.
     *
     * @param scannerData current state of the scanner including the lexical range of the bitstream
     *            to scan
     * @param localBlockId id of the block to parse
     * @param blockParser parser specific to the block to parse
     */
    static void scanBlock(ScannerData scannerData, long localBlockId, BCBlockParser blockParser) {
        scanBlock(scannerData, new LLVMBitcodeRecord(), localBlockId, blockParser);
    }

    /**
     * Invokes a supplied parser to parse all records and subblocks of a specific block an an llvm
     * bitstream.
     *
     * @param scannerData current state of the scanner including the lexical range of the bitstream
     *            to scan
     * @param record buffer for record contents
     * @param localBlockId id of the block to parse
     * @param blockParser parser specific to the block to parse
     */
    static void scanBlock(ScannerData scannerData, LLVMBitcodeRecord record, long localBlockId, BCBlockParser blockParser) {
        final LLVMBitcodeReader bitstream = scannerData.bitstream;
        final long startOffset = scannerData.blockStartOffset;
        final long endOffset = scannerData.blockEndOffset;
        final int localIdSize = scannerData.entryIdSize;
        final ArrayList<LLVMBitcodeAbbreviatedRecord[]> abbreviationDefinitions = new ArrayList<>(scannerData.defaultAbbreviations.computeIfAbsent(localBlockId, ABBREV_LIST_ALLOCATOR));

        if (blockParser instanceof BCBlockInfoBlock) {
            ((BCBlockInfoBlock) blockParser).connect(scannerData.defaultAbbreviations, abbreviationDefinitions);
        }

        blockParser.onEnter();

        // ensure the offset is correct if the block is parsed lazily
        bitstream.setOffset(startOffset);

        while (bitstream.getOffset() < endOffset) {

            final int abbreviationId = Math.toIntExact(bitstream.readFixed(localIdSize));
            switch (abbreviationId) {
                case END_BLOCK: {
                    bitstream.alignInt();
                    assert bitstream.getOffset() == endOffset;
                    break;
                }

                case ENTER_SUBBLOCK: {
                    final int newBlockId = Math.toIntExact(bitstream.readVBR(SUBBLOCK_ID_WIDTH));
                    final int newIdSize = Math.toIntExact(bitstream.readVBR(SUBBLOCK_ID_SIZE_WIDTH));
                    bitstream.alignInt();
                    final long numWords = bitstream.readFixed(Integer.SIZE);
                    final long blockEndOffset = bitstream.getOffset() + (numWords * Integer.SIZE);

                    final ScannerData newScannerData = new ScannerData(scannerData.defaultAbbreviations, bitstream, bitstream.getOffset(), blockEndOffset, newIdSize);
                    final BCBlockParser subblockParser = BCBlockInfoBlock.BLOCK_ID == newBlockId ? new BCBlockInfoBlock() : blockParser.getParserForSubblock(newScannerData, newBlockId);
                    if (subblockParser != null) {
                        final int localAbbreviationDefinitionsOffset = scannerData.defaultAbbreviations.computeIfAbsent(localBlockId, ABBREV_LIST_ALLOCATOR).size();

                        scanBlock(newScannerData, newBlockId, subblockParser);

                        // update default abbreviations if the new block contained a BLOCKINFO block
                        final ArrayList<LLVMBitcodeAbbreviatedRecord[]> localRecords = subList(abbreviationDefinitions, localAbbreviationDefinitionsOffset);
                        abbreviationDefinitions.clear();
                        abbreviationDefinitions.addAll(scannerData.defaultAbbreviations.computeIfAbsent(localBlockId, ABBREV_LIST_ALLOCATOR));
                        abbreviationDefinitions.addAll(localRecords);
                    } else {
                        bitstream.setOffset(blockEndOffset);
                    }

                    assert bitstream.getOffset() == blockEndOffset;

                    break;
                }

                case DEFINE_ABBREV: {
                    final LLVMBitcodeAbbreviatedRecord[] operandScanners = LLVMBitcodeAbbreviatedRecord.defineAbbreviation(bitstream);
                    abbreviationDefinitions.add(operandScanners);
                    break;
                }

                case UNABBREV_RECORD: {
                    final long recordId = bitstream.readVBR(UNABBREVIATED_RECORD_ID_WIDTH);
                    record.addOp(recordId);

                    final long opCount = bitstream.readVBR(UNABBREVIATED_RECORD_OPS_WIDTH);
                    record.ensureFits(opCount);

                    for (int i = 0; i < opCount; i++) {
                        final long op = bitstream.readVBR(UNABBREVIATED_RECORD_OPERAND_WIDTH);
                        record.addOpNoCheck(op);
                    }
                    blockParser.parseRecord(record);
                    record.invalidate();
                    break;
                }

                default:
                // not a predefined id, the bitcode file must have defined it in a previous
                // DEFINE_ABBREV
                {
                    // the id of the abbreviated record is not stored separately
                    final LLVMBitcodeAbbreviatedRecord[] recordComponentParsers = abbreviationDefinitions.get(abbreviationId - CUSTOM_ABBREV_OFFSET);
                    for (LLVMBitcodeAbbreviatedRecord recordComponentParser : recordComponentParsers) {
                        if (recordComponentParser != null) {
                            recordComponentParser.scan(bitstream, record);
                        }
                    }
                    blockParser.parseRecord(record);
                    record.invalidate();
                    break;
                }
            }
        }

        /*
         * Run this here instead of when we read an END_BLOCK so that it is executed also for the
         * implicit root block. The implicit root block is a concept that Sulong uses to handle the
         * fact that a bitcode file can contain multiple top-level blocks. There is no actual
         * ENTER_SUBBLOCK or END_BLOCK record for it, so any finalization for it would not be run in
         * case we called onExit only when we see an END_BLOCK.
         */
        blockParser.onExit();
    }

    private static <V> ArrayList<V> subList(ArrayList<V> original, int from) {
        final ArrayList<V> newList = new ArrayList<>(original.size() - from);
        for (int i = from; i < original.size(); i++) {
            newList.add(original.get(i));
        }
        return newList;
    }

    private BCBlockScanner() {
        // no instances
    }

    private static final int DEFAULT_ID_SIZE = 2;

    /**
     * Initialize a scanner for a bitcode file.
     *
     * @param reader stream containing the bitcode to parse
     * @param magicWordSize offset into the stream, usually points to after the magic word
     * @return state for a scanner that parses all bitcode from the givens tream starting at the
     *         given offset
     */
    static ScannerData createScannerData(LLVMBitcodeReader reader, int magicWordSize) {
        return new ScannerData(new HashMap<>(), reader, magicWordSize, reader.size(), DEFAULT_ID_SIZE);
    }

    /**
     * Container for the current scanner state.
     */
    static final class ScannerData {

        private final HashMap<Long, ArrayList<LLVMBitcodeAbbreviatedRecord[]>> defaultAbbreviations;
        private final LLVMBitcodeReader bitstream;
        private final long blockStartOffset;
        private final long blockEndOffset;
        private final int entryIdSize;

        private ScannerData(HashMap<Long, ArrayList<LLVMBitcodeAbbreviatedRecord[]>> defaultAbbreviations, LLVMBitcodeReader bitstream, long blockStartOffset, long blockEndOffset,
                        int entryIdSize) {
            this.defaultAbbreviations = defaultAbbreviations;
            this.bitstream = bitstream;
            this.blockStartOffset = blockStartOffset;
            this.blockEndOffset = blockEndOffset;
            this.entryIdSize = entryIdSize;

            assert bitstream != null : "Unavailable bitstream";
            assert blockStartOffset >= 0 : "Invalid bitstream start offset";
            assert blockEndOffset >= blockStartOffset : "Invalid bitstream end offset";
            assert entryIdSize > 0 : "Invalid id size";
        }
    }

    /**
     * Parser for the BLOCKINFO block. The BLOCKINFO block is special in that it affects the scanner
     * state of other blocks by defining abbreviations for them that precede the definitions each
     * block contains internally.
     */
    private static final class BCBlockInfoBlock extends BCBlockParser {

        static final long BLOCK_ID = 0;

        private static final long NO_CURRENT_BLOCK = -1L;
        private long currentBlockId;

        private HashMap<Long, ArrayList<LLVMBitcodeAbbreviatedRecord[]>> currentDefaultAbbreviations;
        private ArrayList<LLVMBitcodeAbbreviatedRecord[]> localAbbreviationDefinitions;

        BCBlockInfoBlock() {
            this.currentBlockId = NO_CURRENT_BLOCK;
            this.currentDefaultAbbreviations = null;
            this.localAbbreviationDefinitions = null;
        }

        void connect(HashMap<Long, ArrayList<LLVMBitcodeAbbreviatedRecord[]>> defaultAbbreviations, ArrayList<LLVMBitcodeAbbreviatedRecord[]> abbreviationDefinitions) {
            this.currentDefaultAbbreviations = defaultAbbreviations;
            this.localAbbreviationDefinitions = abbreviationDefinitions;
        }

        @Override
        void onEnter() {
            assert currentDefaultAbbreviations != null;
            assert localAbbreviationDefinitions != null;
            localAbbreviationDefinitions.clear();
        }

        @Override
        void parseRecord(LLVMBitcodeRecord record) {
            if (record.getId() == 1) {
                // SETBID tells us which blocks is currently being described
                // we simply ignore SETRECORDNAME since we do not need it
                setDefaultAbbreviations();
                currentBlockId = record.getAt(0);
            }
        }

        @Override
        BCBlockParser getParserForSubblock(BCBlockScanner.ScannerData scannerData, int blockId) {
            throw new LLVMParserException("BLOCKINFO block contains another block, id of contained block is " + blockId);
        }

        @Override
        void onExit() {
            setDefaultAbbreviations();
            currentDefaultAbbreviations = null;
            localAbbreviationDefinitions = null;
        }

        private void setDefaultAbbreviations() {
            if (currentBlockId != NO_CURRENT_BLOCK) {
                final ArrayList<LLVMBitcodeAbbreviatedRecord[]> targetAbbreviationsList = currentDefaultAbbreviations.computeIfAbsent(currentBlockId, ABBREV_LIST_ALLOCATOR);
                targetAbbreviationsList.addAll(localAbbreviationDefinitions);
                localAbbreviationDefinitions.clear();
            }
        }
    }
}
