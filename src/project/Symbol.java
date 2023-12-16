package project;

import static project.ElfParser.getStringFromStringTable;

public class Symbol {
    private int symbolIndex;
    private int name;
    private int value;
    private int size;
    private int info;
    private int other;
    private int shndx;

    public String getStringRepresentation(SectionHeader strTable, byte[] file) {

        String type = getTypeString();
        String bind = getBindString();
        String name = getStringFromStringTable(getName(), strTable, file);
        String vis = getVisString();
        String index = getIndexString();

        String ans = String.format("[%4d] 0x%-15X %5d %-8s %-8s %-8s %6s %s\n",
                symbolIndex, getValue(), getSize(), type, bind, vis, index, name);

        return ans;
    }

    private String getIndexString() {
        if (shndx == 0) {
            return "UNDEF";
        }
        if (shndx >= 0xff00 && shndx <= 0xff1f) {
            return "PROC_SPEC";
        }
        if (shndx >= 0xff20 && shndx <= 0xff3f) {
            return "OS_SPEC";
        }
        if (shndx == 0xfff1) {
            return "ABS";
        }
        if (shndx == 0xfff2) {
            return "COMMON";
        }
        if (shndx == 0xffff) {
            return "XINDEX";
        }
        return Integer.toString(shndx);
    }

    private String getBindString() {
        return switch (getBind()) {
            case 0 -> "LOCAL";
            case 1 -> "GLOBAL";
            case 2 -> "WEAK";
            case 13, 14, 15 -> "PROC";
            default -> "UNK";
        };
    }

    private String getTypeString() {
        return switch (getType()) {
            case 0 -> "NOTYPE";
            case 1 -> "OBJECT";
            case 2 -> "FUNC";
            case 3 -> "SECTION";
            case 4 -> "FILE";
            case 13, 14, 15 -> "PROC_SPEC";
            default -> "UNK";
        };
    }

    private String getVisString() {
        return switch (getVis()) {
            case 0 -> "DEFAULT";
            case 1 -> "INTERNAL";
            case 2 -> "HIDDEN";
            case 3 -> "PROTECTED";
            default -> "UNK";
        };
    }

    public int getBind() {
        return info >> 4;
    }

    public int getType() {
        return info & 0xf;
    }

    public int getVis() {
        return other & 0x3;
    }

    public void setOther(int other) {
        this.other = other;
    }

    public void setSymbolIndex(int symbolIndex) {
        this.symbolIndex = symbolIndex;
    }

    public int getName() {
        return name;
    }

    public void setName(int name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setInfo(int info) {
        this.info = info;
    }

    public void setShndx(int shndx) {
        this.shndx = shndx;
    }
}