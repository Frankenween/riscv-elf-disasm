package project;

public class ElfHeader {
    public static byte EI_CLASS;
    public static byte EI_DATA;
    public static byte EI_OSABI;
    public static byte EI_ABIVERSION;

    public static int E_TYPE;
    public static int E_MACHINE;

    public static int E_ENTRY;
    public static int PROGRAM_HEADERS_OFFSET;
    public static int SECTION_HEADERS_OFFSET;

    public static int E_FLAGS;
    public static int E_ELF_HEADER_SIZE;
    public static int E_PROGRAM_HEADER_SIZE;
    public static int E_PROGRAM_HEADERS_NUMBER;
    public static int E_PROGRAM_SECTION_SIZE;
    public static int E_PROGRAM_SECTION_NUMBER;
    public static int E_SHSTRNDX;


    public static String getHeader() {
        return "ELFHeader{" + "\n" +
                "EI_CLASS=" + EI_CLASS + "\n" +
                ", EI_DATA=" + EI_DATA + "\n" +
                ", EI_OSABI=" + EI_OSABI + "\n" +
                ", EI_ABIVERSION=" + EI_ABIVERSION + "\n" +
                ", E_TYPE=" + E_TYPE + "\n" +
                ", E_MACHINE=" + E_MACHINE + "\n" +
                ", E_ENTRY=" + E_ENTRY + "\n" +
                ", PROGRAM_HEADERS_OFFSET=" + PROGRAM_HEADERS_OFFSET + "\n" +
                ", SECTION_HEADERS_OFFSET=" + SECTION_HEADERS_OFFSET + "\n" +
                ", E_FLAGS=" + E_FLAGS + "\n" +
                ", E_ELF_HEADER_SIZE=" + E_ELF_HEADER_SIZE + "\n" +
                ", E_PROGRAM_HEADER_SIZE=" + E_PROGRAM_HEADER_SIZE + "\n" +
                ", E_PROGRAM_HEADERS_NUMBER=" + E_PROGRAM_HEADERS_NUMBER + "\n" +
                ", E_PROGRAM_SECTION_SIZE=" + E_PROGRAM_SECTION_SIZE + "\n" +
                ", E_PROGRAM_SECTION_NUMBER=" + E_PROGRAM_SECTION_NUMBER + "\n" +
                ", E_SHSTRNDX=" + E_SHSTRNDX + "\n" +
                '}' +  "\n";
    }
}
