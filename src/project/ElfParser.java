package project;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ElfParser {
    private final byte[] file;
    private SectionHeader[] sectionHeaders;
    Map<Integer, String> tags;
    private int LOC = 0;

    public ElfParser(byte[] file) {
        this.file = file;
        tags = new HashMap<>();
    }

    private byte take(int pos) {
        Assert.ensure(pos < file.length, "Tried to read byte %s, while there are %s bytes only",
                pos + 1, file.length);
        return file[pos];
    }

    private void ensureBlock(int pos, int... block) {
        for (int j = 0; j < block.length; j++) {
            Assert.ensure(block[j] == take(pos + j),
                    "Expected byte %s to be %s, but got %s",
                    pos + j + 1, block[j], take(pos + j));
        }
    }

    private byte[] readSeq(int pos, int len) {
        Assert.ensure(pos + len <= file.length,
                "Couldn't read sequence. Pos is %s, len is %s but file size is %s",
                pos, len, file.length);
        return Arrays.copyOfRange(file, pos, pos + len);
    }

    private int littleEndianToInt(byte[] number) {
        int res = 0;
        for (int i = number.length - 1; i >= 0; i--) {
            res <<= 8;
            res |= Byte.toUnsignedInt(number[i]);
        }
        return res;
    }

    private int read4BytesAsInt(int pos) {
        return littleEndianToInt(readSeq(pos,4));
    }

    private int read2BytesAsInt(int pos) {
        return littleEndianToInt(readSeq(pos, 2));
    }

    private void readElfHeader() {
        int pos = 0;
        ensureBlock(0, 0x7f, 0x45, 0x4c, 0x46);
        pos += 4;

        ElfHeader.EI_CLASS = take(pos++);
        Assert.ensure(ElfHeader.EI_CLASS == 0x01, "This is not a 32-bit file");

        ElfHeader.EI_DATA = take(pos++);
        Assert.ensure(ElfHeader.EI_DATA == 0x01, "This is not a Little-Endian file");

        ensureBlock(0x01); // EI_VERSION
        pos++;
        ElfHeader.EI_OSABI = take(pos++);
        ElfHeader.EI_ABIVERSION = take(pos++);
        readSeq(pos, 7); // Padding
        pos += 7;


        ElfHeader.E_TYPE = read2BytesAsInt(pos); pos += 2;
        ElfHeader.E_MACHINE = read2BytesAsInt(pos); pos += 2;
        Assert.ensure(ElfHeader.E_MACHINE == 0xf3, "RISC-V is supported only");

        Assert.ensure(read4BytesAsInt(pos) == 1, "Format version must be equal to 1"); pos += 4;

        ElfHeader.E_ENTRY = read4BytesAsInt(pos); // This is a 32-bit file
        pos += 4;
        ElfHeader.PROGRAM_HEADERS_OFFSET = read4BytesAsInt(pos); pos += 4;
        ElfHeader.SECTION_HEADERS_OFFSET = read4BytesAsInt(pos); pos += 4;

        ElfHeader.E_FLAGS = read4BytesAsInt(pos); pos += 4;
        ElfHeader.E_ELF_HEADER_SIZE = read2BytesAsInt(pos); pos += 2;

        ElfHeader.E_PROGRAM_HEADER_SIZE = read2BytesAsInt(pos); pos += 2;
        ElfHeader.E_PROGRAM_HEADERS_NUMBER = read2BytesAsInt(pos); pos += 2;

        ElfHeader.E_PROGRAM_SECTION_SIZE = read2BytesAsInt(pos); pos += 2;
        ElfHeader.E_PROGRAM_SECTION_NUMBER = read2BytesAsInt(pos); pos += 2;

        ElfHeader.E_SHSTRNDX = read2BytesAsInt(pos);
    }

    private SectionHeader readSectionHeader(int pos) {
        SectionHeader sectionHeader = new SectionHeader();

        sectionHeader.setSh_name(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_type(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_flags(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_address(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_offset(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_size(read4BytesAsInt(pos)); pos += 4;

        sectionHeader.setSh_link(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_info(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_address_align(read4BytesAsInt(pos)); pos += 4;
        sectionHeader.setSh_entry_size(read4BytesAsInt(pos));

        return sectionHeader;
    }

    private SectionHeader[] readAllSectionHeaders() {
        int pos = ElfHeader.SECTION_HEADERS_OFFSET;
        SectionHeader[] sectionHeaders = new SectionHeader[ElfHeader.E_PROGRAM_SECTION_NUMBER];
        for (int i = 0; i < sectionHeaders.length; i++) {
            sectionHeaders[i] = readSectionHeader(pos);
            pos += ElfHeader.E_PROGRAM_SECTION_SIZE;
        }
        return sectionHeaders;
    }

    public static String getStringFromStringTable(int index, SectionHeader stringTable, byte[] file) {
        Assert.ensure(index < stringTable.getSh_size(), "Bad index: %s, but stringTable size is %s",
                    index, stringTable.getSh_size());
        StringBuilder val = new StringBuilder();
        while (file[index + stringTable.getSh_offset()] != 0) {
            val.append((char) (file[index + stringTable.getSh_offset()]));
            index++;
        }
        return val.toString();
    }

    private Symbol readSymbol(int pos) {

        Symbol symbol = new Symbol();
        symbol.setName(read4BytesAsInt(pos)); pos += 4;
        symbol.setValue(read4BytesAsInt(pos)); pos += 4;
        symbol.setSize(read4BytesAsInt(pos)); pos += 4;
        symbol.setInfo(take(pos)); pos++;
        symbol.setOther(take(pos)); pos++;
        symbol.setShndx(read2BytesAsInt(pos));

        return symbol;
    }

    private Symbol[] readSymtab(SectionHeader symtab, SectionHeader stringTable) {
        int index = symtab.getSh_offset();
        Symbol[] res = new Symbol[symtab.getSh_size() / symtab.getSh_entry_size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = readSymbol(index);
            res[i].setSymbolIndex(i);
            if (res[i].getType() == 2) { // Function
                tags.put(res[i].getValue(), getStringFromStringTable(res[i].getName(), stringTable, file));
            }
            index += symtab.getSh_entry_size();
        }
        return res;
    }

    private AsmCommand[] readText(SectionHeader textHeader) {
        int pos = textHeader.getSh_offset();
        int address = textHeader.getAddress();

        ArrayList<AsmCommand> res = new ArrayList<>();
        while (pos < textHeader.getSh_offset() + textHeader.getSh_size()) {
            AsmCommand cmd;
            if (AsmCommand.InstructionType.getIsaInstructionType(take(pos) & 0x3) == AsmCommand.InstructionType.RV32) {
                cmd = new AsmCommand(read4BytesAsInt(pos), address, tags);
                pos += 4;
                address += 4;
            } else {
                cmd = new AsmCommand(read2BytesAsInt(pos), address, tags);
                pos += 2;
                address += 2;
            }
            res.add(cmd);

            Integer jmp = cmd.getJumpAddress();
            if (jmp != null) {
                if (!tags.containsKey(jmp)) {
                    tags.put(jmp, String.format("LOC_%05x", LOC++));
                }
            }
        }
        AsmCommand[] answer = new AsmCommand[res.size()];
        res.toArray(answer);

        return answer;
    }

    public void parse(PrintWriter out) {
        try {
            readElfHeader();
        } catch (AssertionError e) {
            out.print("Unsupported file\n");
            out.print(e.getMessage());
            return;
        }
        sectionHeaders = readAllSectionHeaders();

        SectionHeader symtab = null, textData = null;
        for (SectionHeader header : sectionHeaders) {
            if (header.getSh_type() == 2) {
                symtab = header;
            }
            if (header.getSh_type() == 1 && header.getSh_flags() == 2 + 4) {
                textData = header;
            }
        }

        Symbol[] symbols = new Symbol[0];
        if (symtab != null) {
            symbols = readSymtab(symtab, sectionHeaders[symtab.getSh_link()]);
        }
        AsmCommand[] text = new AsmCommand[0];
        if (textData != null) {
            text = readText(textData);
        }

        for (AsmCommand cmd : text) {
            String tag = "";
            if (tags.containsKey(cmd.getAddress())) {
                tag = tags.get(cmd.getAddress());
            }
            cmd.setTag(tag);
        }

        out.println(".text");
        for (AsmCommand cmd : text) {
            out.print(cmd);
        }
        out.println();
        out.println(".symtab");
        out.print(String.format("%s %-15s %7s %-8s %-8s %-8s %6s %s\n",
                "Symbol", "Value", "Size", "Type", "Bind", "Vis", "Index", "Name"));
        for (Symbol s : symbols) {
            out.print(s.getStringRepresentation(sectionHeaders[symtab.getSh_link()], file));
        }
    }
}
