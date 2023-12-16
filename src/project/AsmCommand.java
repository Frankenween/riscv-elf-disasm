package project;

import java.util.Map;

public class AsmCommand {
    enum InstructionType {
        COMPRESSED_Q0,
        COMPRESSED_Q1,
        COMPRESSED_Q2,
        RV32;

        public static InstructionType getIsaInstructionType(int x) {
            Assert.ensure(x < 4 && x >= 0, "Expected first two bits, got %s", x);
            return switch (x) {
                case 0 -> COMPRESSED_Q0;
                case 1 -> COMPRESSED_Q1;
                case 2 -> COMPRESSED_Q2;
                case 3 -> RV32;
                default -> throw new AssertionError("WTF");
            };
        }
    }

    int inst;
    int address;
    String tag;
    Map<Integer, String> go;

    private static int getBitSubseq(int x, int l, int r) {
        return (x >> l) & ((1 << (r - l + 1)) - 1);
    }

    private static int getBit(int x, int b) {
        return (x >> b) & 1;
    }

    private static int signExtend(int x, int signBit) {
        int len = 32 - signBit;
        int mask = ((getBit(x, signBit) << len) - getBit(x, signBit)) << signBit;
        return x | mask;
    }

    private static int reorder(int immediate, int... wherePut) {
        int answer = 0;
        for (int i = 0; i < wherePut.length; i++) {
            answer |= getBit(immediate, i) << wherePut[i];
        }
        return answer;
    }

    AsmCommand(int inst, int pos, Map<Integer, String> go) {
        this.inst = inst;
        this.address = pos;
        this.tag = "";
        this.go = go;
    }

    public int getAddress() {
        return address;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getJumpAddress() {
        int opcode = getBitSubseq(inst, 0, 6);
        if ((opcode & 0b11) != 0b11) {
            return getCompressedJumpAddress();
        }
        return switch (opcode) {
            case 0b1101111 ->
                    // JAL
                    getJImmediate() + address;
            case 0b1100011 ->
                    // Branch
                    getBImmediate() + address;
            default -> null;
        };
    }

    private Integer getCompressedJumpAddress() {
        int q = getBitSubseq(inst, 0, 1);
        int funct = getBitSubseq(inst, 13, 15);
        if (q == 0b01) {
            if (funct == 0b001 || funct == 0b101) { // J and JAL
                return address + getJalCompressedImmediate();
            }
            if (funct == 0b110 || funct == 0b111) { // branch
                return address + getBranchCompressedImmediate();
            }
        }
        return null;
    }

    private String xRegToAbi(int x) {
        if (x >= 10 && x <= 17) {
            return "a" + (x - 10);
        }
        if (18 <= x && x <= 27) {
            return "s" + (x - 16);
        }
        if (x >= 28) {
            return "t" + (x - 25);
        }
        if (5 <= x && x <= 7) {
            return "t" + (x - 5);
        }
        return switch (x) {
            case 0 -> "zero";
            case 1 -> "ra";
            case 2 -> "sp";
            case 3 -> "gp";
            case 4 -> "tp";
            case 8 -> "s0";
            case 9 -> "s1";
            default -> "??";
        };
    }

    private String xRegToC(int x) {
        if (x <= 1) {
            return "s" + x;
        }
        return "a" + (x - 2);
    }

    private String toCsrRegister(int imm) {
        String name = switch (imm) {
            case 0xf11 -> "mvandorid";
            case 0xf12 -> "marchid";
            case 0xf13 -> "mimpid";
            case 0xf14 -> "mhartid";
            case 0x300 -> "mstatus";
            case 0x301 -> "misa";
            case 0x302 -> "medeleg";
            case 0x303 -> "mideleg";
            case 0x304 -> "mie";
            case 0x305 -> "mtvec";
            case 0x306 -> "mcounteren";
            case 0x310 -> "mstatush";
            case 0x340 -> "mscratch";
            case 0x341 -> "mepc";
            case 0x342 -> "mcause";
            case 0x343 -> "mtval";
            case 0x344 -> "mip";
            case 0x34A -> "mtinst";
            case 0x34B -> "mtval2";
            case 0xb00 -> "mcycle";
            case 0xb02 -> "minstret";
            case 0xb80 -> "mcycleh";
            case 0xb82 -> "minstreth";
            case 0x320 -> "mcountinhibit";
            case 0x7a0 -> "tselect";
            case 0x7a1 -> "tdata1";
            case 0x7a2 -> "tdata2";
            case 0x7a3 -> "tdata3";
            case 0x7b0 -> "dcsr";
            case 0x7b1 -> "dpc";
            case 0x7b2 -> "dscratch0";
            case 0x7b3 -> "dscratch1";
            case 0x100 -> "sstatus";
            case 0x102 -> "sedeleg";
            case 0x103 -> "sideleg";
            case 0x104 -> "sie";
            case 0x105 -> "stvec";
            case 0x106 -> "scounteren";
            case 0x140 -> "sscratch";
            case 0x141 -> "sepc";
            case 0x142 -> "scause";
            case 0x143 -> "stval";
            case 0x144 -> "sip";
            case 0x180 -> "satp";
            default -> null;
        };
        if (name == null) {
            if (0x3a0 <= imm && imm <= 0x3af) {
                name = "pmpcfg" + (imm - 0x3a0);
            } else if (0x3b0 <= imm && imm <= 0x3ef) {
                name = "pmpaddr" + (imm - 0x3b0);
            } else if (0xb03 <= imm && imm <= 0xb1f) {
                name = "mhpmcounter" + (imm - 0xb03 + 3);
            } else if (0xb83 <= imm && imm <= 0xb9f) {
                name = "mhpmcounter" + (imm - 0xb83 + 3) + "h";
            } else if (0x323 <= imm && imm <= 0x33f) {
                name = "mhpmevent" + (imm - 0x323 + 3);
            }
        }
        return name;
    }

    private int getIImmediate() {
        int imm = getBitSubseq(inst, 20, 31);
        return signExtend(imm, 11);
    }

    private int getSImmediate() {
        int imm = getBitSubseq(inst, 7, 11);
        imm |= getBitSubseq(inst, 25, 31) << 5;
        return signExtend(imm, 11);
    }

    private int getBImmediate() {
        int imm = (getBitSubseq(inst, 8, 11) << 1) |
                (getBitSubseq(inst, 25, 30) << 5) |
                (getBitSubseq(inst, 7, 7) << 11) |
                (getBit(inst, 31) << 12);
        return signExtend(imm, 12);
    }

    private int getUImmediate() {
        return getBitSubseq(inst, 12, 31) << 12;
    }

    private int getJImmediate() {
        int imm = getBitSubseq(inst, 21, 24) << 1;
        imm |= getBitSubseq(inst, 25, 30) << 5;
        imm |= getBitSubseq(inst, 20, 20) << 11;
        imm |= getBitSubseq(inst, 12, 19) << 12;
        imm |= getBit(inst, 31) << 20;

        return signExtend(imm, 20);
    }

    private int getJalCompressedImmediate() {
        int imm = getBitSubseq(inst, 2, 12);
        imm = reorder(imm, 5, 1, 2, 3, 7, 6, 10, 8, 9, 4, 11);
        imm = signExtend(imm, 11);
        return imm;
    }

    private int getBranchCompressedImmediate() {
        int imm = getBitSubseq(inst, 2, 6) | (getBitSubseq(inst, 10, 12) << 5);
        imm = reorder(imm, 5, 1, 2, 6, 7, 3, 4, 8);
        imm = signExtend(imm, 8);
        return imm;
    }

    private String decodeIType() {
        int opcode = getBitSubseq(inst, 0, 6);
        int rd = getBitSubseq(inst, 7, 11);
        int funct3 = getBitSubseq(inst, 12, 14);
        int rs1 = getBitSubseq(inst, 15, 19);
        int immediate = getIImmediate();
        String name;

        switch (opcode) {
            case 0b1100111:
                // JALR
                return String.format("%08x %10s: JALR %s, %s(%s)\n",
                        address, tag, xRegToAbi(rd), immediate, xRegToAbi(rs1));
            case 0b0000011: // Loads
                int memAddress = immediate;
                switch (funct3) {
                    case 0b000 -> name = "LB";
                    case 0b001 -> name = "LH";
                    case 0b010 -> name = "LW";
                    case 0b100 -> name = "LBU";
                    case 0b101 -> name = "LHU";
                    default -> {
                        System.err.printf("Bad I instruction %s\n" +
                                        "opcode is %s, funct3 is %s%n",
                                Integer.toString(inst, 2),
                                Integer.toString(opcode, 2),
                                Integer.toString(funct3, 2));
                        name = "UNKNOWN_COMMAND";
                    }
                }
                return String.format("%08x %10s: %s %s, %s(%s)\n",
                        address, tag, name, xRegToAbi(rd), memAddress, xRegToAbi(rs1));
            case 0b0010011: // Number op
                switch (funct3) {
                    case 0b000 -> name = "ADDI";
                    case 0b010 -> name = "SLTI";
                    case 0b011 -> name = "SLTIU";
                    case 0b100 -> name = "XORI";
                    case 0b110 -> name = "ORI";
                    case 0b111 -> name = "ANDI";
                    case 0b001 -> {
                        name = "SLLI";
                        immediate = getBitSubseq(inst, 20, 24);
                    }
                    case 0b101 -> { // Shifts
                        immediate = getBitSubseq(inst, 20, 24);
                        switch (getBitSubseq(inst, 25, 31)) {
                            case 0b0 -> name = "SRLI";
                            case 0b0100000 -> name = "SRAI";
                            default -> {
                                System.err.printf("Bad I instruction %s\n" +
                                                "opcode is %s, funct3 is %s%n",
                                        Integer.toString(inst, 2),
                                        Integer.toString(opcode, 2),
                                        Integer.toString(funct3, 2));
                                name = "UNKNOWN_COMMAND";
                            }
                        }
                    }
                    default -> {
                        System.err.printf("Bad I instruction %s\n" +
                                        "opcode is %s, funct3 is %s%n",
                                Integer.toString(inst, 2),
                                Integer.toString(opcode, 2),
                                Integer.toString(funct3, 2));
                        name = "UNKNOWN_COMMAND";
                    }
                }
                break;
            case 0b1110011:
                // System
                if (rd == 0b0) {
                    if (immediate == 0b0) {
                        name = "ECALL";
                    } else if (immediate == 0b1){
                        name = "EBREAK";
                    } else {
                        name = "UNKNOWN_COMMAND";
                    }
                    return String.format("%08x %10s: %s\n", address, tag, name);
                } else {
                    switch (funct3) {
                        case 0b001 -> name = "CSRRW";
                        case 0b010 -> name = "CSRRS";
                        case 0b011 -> name = "CSRRC";
                        case 0b101 -> name = "CSRRWI";
                        case 0b110 -> name = "CSRRSI";
                        case 0b111 -> name = "CSRRCI";
                        default -> name = "UNKNOWN_COMMAND";
                    }
                    if (funct3 <= 0b011) {
                        return String.format("%08x %10s: %s, %s, %s, %s\n",
                                address, tag, name, xRegToAbi(rd), toCsrRegister(immediate), xRegToAbi(rs1));
                    } else {
                        return String.format("%08x %10s: %s, %s, %s, %s\n",
                                address, tag, name, xRegToAbi(rd), toCsrRegister(immediate), rs1);
                    }
                }
            default:
                System.err.printf("Bad I instruction %s\n" +
                                "opcode is %s, funct3 is %s%n",
                        Integer.toString(inst, 2),
                        Integer.toString(opcode, 2),
                        Integer.toString(funct3, 2));
                name = "UNKNOWN_COMMAND";
        }
        return String.format("%08x %10s: %s, %s, %s, %s\n",
                address, tag, name, xRegToAbi(rd), xRegToAbi(rs1), immediate);
    }

    private String decodeRType() {
        int rd = getBitSubseq(inst, 7, 11);
        int funct3 = getBitSubseq(inst, 12, 14);
        int rs1 = getBitSubseq(inst, 15, 19);
        int rs2 = getBitSubseq(inst, 20, 24);
        int funct7 = getBitSubseq(inst, 25, 31);

        String name;

        switch (funct7) {
            case 0b0100000:
                switch (funct3) {
                    case 0b0 -> name = "SUB";
                    case 0b101 -> name = "SRA";
                    default -> {
                        System.err.printf("Bad R instruction %s\n" +
                                        "funct7 is %s, but funct3 is %s%n",
                                Integer.toString(inst, 2),
                                Integer.toString(funct7, 2),
                                Integer.toString(funct3, 2));
                        name = "UNKNOWN_COMMAND";
                    }
                }
                break;
            case 0b0:
                switch (funct3) {
                    case 0b000 -> name = "ADD";
                    case 0b001 -> name = "SLL";
                    case 0b010 -> name = "SLT";
                    case 0b011 -> name = "SLTU";
                    case 0b100 -> name = "XOR";
                    case 0b101 -> name = "SRL";
                    case 0b110 -> name = "OR";
                    case 0b111 -> name = "AND";
                    default -> {
                        System.err.printf("Bad R instruction %s\n" +
                                        "funct7 is %s, but funct3 is %s%n",
                                Integer.toString(inst, 2),
                                Integer.toString(funct7, 2),
                                Integer.toString(funct3, 2));
                        name = "UNKNOWN_COMMAND";
                    }
                }
                break;
            case 0b0000001:
                // RV32M
                switch (funct3) {
                    case 0b000 -> name = "MUL";
                    case 0b001 -> name = "MULH";
                    case 0b010 -> name = "MULHSU";
                    case 0b011 -> name = "MULHU";
                    case 0b100 -> name = "DIV";
                    case 0b101 -> name = "DIVU";
                    case 0b110 -> name = "REM";
                    case 0b111 -> name = "REMU";
                    default -> {
                        System.err.printf("Bad R instruction %s\n" +
                                    "funct7 is %s, but funct3 is %s%n",
                            Integer.toString(inst, 2),
                            Integer.toString(funct7, 2),
                            Integer.toString(funct3, 2));
                        name = "UNKNOWN_COMMAND";
                    }
                }
                break;
            default:
                System.err.printf("Bad R instruction %s\n" +
                                "funct7 is %s, but funct3 is %s%n",
                        Integer.toString(inst, 2),
                        Integer.toString(funct7, 2),
                        Integer.toString(funct3, 2));
                name = "UNKNOWN_COMMAND";
        }

        return String.format("%08x %10s: %s, %s, %s, %s\n",
                address, tag, name, xRegToAbi(rd), xRegToAbi(rs1), xRegToAbi(rs2));
    }

    private String decodeSType() {
        int immediate = getSImmediate();
        int opcode = getBitSubseq(inst, 0, 6);
        int funct3 = getBitSubseq(inst, 12, 14);
        int rs1 = getBitSubseq(inst, 15, 19);
        int rs2 = getBitSubseq(inst, 20, 24);

        String name;

        if (opcode == 0b0100011) {
            switch (funct3) {
                case 0b000 -> name = "SB";
                case 0b001 -> name = "SH";
                case 0b010 -> name = "SW";
                default -> {
                    System.err.println("Bad store instruction, funct3 is " + Integer.toString(funct3, 2));
                    name = "UNKNOWN_COMMAND";
                }
            }
            return String.format("%08x %10s: %s, %s, %s(%s)\n",
                    address, tag, name, xRegToAbi(rs2), immediate, xRegToAbi(rs1));
        }
        return String.format("%08x %10s: UNKNOWN_COMMAND", address, tag);
    }

    private String decodeBType() {
        int immediate = getBImmediate();
        int funct3 = getBitSubseq(inst, 12, 14);
        int rs1 = getBitSubseq(inst, 15, 19);
        int rs2 = getBitSubseq(inst, 20, 24);
        String name;

        switch (funct3) {
            case 0b000 -> name = "BEQ";
            case 0b001 -> name = "BNE";
            case 0b100 -> name = "BLT";
            case 0b101 -> name = "BGE";
            case 0b110 -> name = "BLTU";
            case 0b111 -> name = "BGEU";
            default -> {
                System.err.println("Bad branch, funct3 is " + Integer.toString(funct3, 2));
                name = "UNKNOWN_COMMAND";
            }
        }
        return String.format("%08x %10s: %s, %s, %s, %s\n",
                address, tag, name, xRegToAbi(rs1), xRegToAbi(rs2), go.get(immediate + address));

    }

    private String decodeUType() {
        int opcode = getBitSubseq(inst, 0, 6);
        int immediate = getUImmediate();
        int rd = getBitSubseq(inst, 7, 11);
        String name;

        switch (opcode) {
            case 0b0110111 -> name = "LUI";
            case 0b0010111 -> {
                name = "AUIPC";
            }
            default -> name = "UNKNOWN_COMMAND";
        }
        return String.format("%08x %10s: %s, %s, %s\n",
                address, tag, name, xRegToAbi(rd), immediate);
    }

    private String decodeJType() {
        int immediate = getJImmediate();
        int rd = getBitSubseq(inst, 7, 11);
        return String.format("%08x %10s: %s, %s, %s\n",
                address, tag, "JAL", xRegToAbi(rd), go.get(immediate + address));
    }

    private String decodeQ0() {
        if (inst == 0) {
            return String.format("%08x %10s: INVALID\n", address, tag);
        }
        int funct = getBitSubseq(inst, 13, 15);
        int rd = getBitSubseq(inst, 2, 4);
        int rs1 = getBitSubseq(inst, 7, 9);
        int uimm = getBitSubseq(inst, 5, 6) | (getBitSubseq(inst, 10, 12) << 2);
        switch (funct) {
            case 0b0: // addi4spn
                int imm = getBitSubseq(inst, 5, 12);
                imm = reorder(imm, 3, 2, 6, 7, 8, 9, 4, 5);
                return String.format("%08x %10s: C.ADDI4SPN %s, sp, %s\n",
                        address, tag, xRegToC(rd), imm);
            case 0b010:
                uimm = reorder(uimm, 6, 2, 3, 4, 5);
                return String.format("%08x %10s: C.LW %s, %s(%s)\n",
                        address, tag, xRegToC(rd), uimm, xRegToC(rs1));
            case 0b110:
                uimm = reorder(uimm, 6, 2, 3, 4, 5);
                return String.format("%08x %10s: C.SW %s, %s(%s)\n",
                        address, tag, xRegToC(rd), uimm, xRegToC(rs1));
            default:
                return String.format("%08x %10s: Q0 UNKNOWN_COMMAND\n", address, tag);
        }
    }

    private String decodeQ1() {
        int funct = getBitSubseq(inst, 13, 15);
        switch (funct) {
            case 0b0:
                if (getBitSubseq(inst, 2, 15) == 0) {
                    return String.format("%08x %10s: C.NOP\n", address, tag);
                } else {
                    int rd = getBitSubseq(inst, 7, 11);
                    int nzimm = getBitSubseq(inst, 2, 6) |
                            (getBit(inst, 12) << 5);
                    nzimm = signExtend(nzimm, 5);
                    return String.format("%08x %10s: C.ADDI %s, %s\n",
                            address, tag, xRegToAbi(rd), nzimm);
                }
            case 0b001: // JAL
                int imm = getJalCompressedImmediate();
                return String.format("%08x %10s: C.JAL %s\n",
                        address, tag, go.get(address + imm));
            case 0b101: // J
                imm = getJalCompressedImmediate();
                return String.format("%08x %10s: C.J %s\n",
                        address, tag, go.get(address + imm));
            case 0b110: // BEQZ
                int rs1 = getBitSubseq(inst, 7, 9);
                imm = getBranchCompressedImmediate();
                return String.format("%08x %10s: C.BEQZ %s, %s\n",
                        address, tag, xRegToC(rs1), go.get(imm + address));
            case 0b111: // BNEZ
                rs1 = getBitSubseq(inst, 7, 9);
                imm = getBranchCompressedImmediate();
                return String.format("%08x %10s: C.BNEZ %s, %s\n",
                        address, tag, xRegToC(rs1), go.get(imm + address));
            case 0b010: // LI
                imm = getBitSubseq(inst, 2, 6) | (getBit(inst, 12) << 5);
                imm = signExtend(imm, 5);
                int rd = getBitSubseq(inst, 7, 11);
                return String.format("%08x %10s: C.LI %s, %s\n",
                        address, tag, xRegToAbi(rd), imm);
            case 0b011:
                rd = getBitSubseq(inst, 7, 11);
                imm = getBitSubseq(inst, 2, 6) | (getBit(inst, 12) << 5);
                if (rd == 2) { // ADDI16SP
                    imm = reorder(imm, 5, 7, 8, 6, 4, 9);
                    imm = signExtend(imm, 9);
                    return String.format("%08x %10s: C.ADDI16SP sp, %s\n",
                            address, tag, imm);
                } else { // LUI
                    imm = signExtend(imm, 5);
                    return String.format("%08x %10s: C.LUI %s, %s\n",
                            address, tag, xRegToAbi(rd), imm);
                }
            case 0b100:
                int rdF = getBitSubseq(inst, 10, 11);
                rd = getBitSubseq(inst, 7, 9);
                imm = getBitSubseq(inst, 2, 6) | (getBit(inst, 12) << 5);
                int rs = getBitSubseq(inst, 2, 4);
                int rsF = getBitSubseq(inst, 5, 6);
                return switch (rdF) {
                    case 0b00 -> // SRLI
                            String.format("%08x %10s: C.SRLI %s, %s\n",
                                    address, tag, xRegToC(rd), imm);
                    case 0b01 -> // SRAI
                            String.format("%08x %10s: C.SRAI %s, %s\n",
                                    address, tag, xRegToC(rd), imm);
                    case 0b10 -> {
                        imm = signExtend(imm, 5);
                        yield String.format("%08x %10s: C.ANDI %s, %s\n",
                                address, tag, xRegToC(rd), imm);
                    }
                    case 0b11 -> {
                        String name = switch (rsF) {
                            case 0b00 -> "C.SUB";
                            case 0b01 -> "C.XOR";
                            case 0b10 -> "C.OR";
                            case 0b11 -> "C.AND";
                            default -> "UNKNOWN_COMMAND"; // Incorrect with RV64-128. SUBW/ADDW ignored
                        };
                        yield String.format("%08x %10s: %s, %s, %s\n",
                                address, tag, name, xRegToC(rd), xRegToC(rs)); // Incorrect with RV64-128. SUBW/ADDW ignored
                    }
                    default -> String.format("%08x %10s: VERY BAD\n", address, tag);
                };
            default:
                return String.format("%08x %10s: Q1 UNKNOWN_COMMAND\n", address, tag);
        }
    }

    private String decodeQ2() {
        int funct = getBitSubseq(inst, 13, 15);
        int imm = getBitSubseq(inst, 2, 6) | (getBit(inst, 12) << 5);
        int rd = getBitSubseq(inst, 7, 11);
        int rdFlag = getBit(inst, 12);
        int rs = getBitSubseq(inst, 2, 6);
        switch (funct) {
            case 0b000: // SLLI
                return String.format("%08x %10s: C.SLLI %s, %s\n",
                        address, tag, xRegToAbi(rd), imm);
            case 0b010: // LWSP
                imm = reorder(imm, 6, 7, 2, 3, 4, 5);
                return String.format("%08x %10s: C.LWSP %s, %s(sp)\n",
                        address, tag, xRegToAbi(rd), imm);
            case 0b110: // SWSP
                int uimm = getBitSubseq(inst, 7, 12);
                uimm = reorder(uimm, 6, 7, 2, 3, 4, 5);
                return String.format("%08x %10s: C.SWSP %s, %s(sp)\n",
                        address, tag, xRegToAbi(rs), uimm);
            case 0b100: // JR, MV, EBREAK, JALR, ADD
                switch (rdFlag) {
                    case 0: // JR, MV
                        if (rs == 0) { // JR
                            return String.format("%08x %10s: C.JR %s\n",
                                    address, tag, xRegToAbi(rd));
                        } else { // MV
                            return String.format("%08x %10s: C.MV %s, %s\n",
                                    address, tag, xRegToAbi(rd), xRegToAbi(rs));
                        }
                    case 1:
                        if (rd == 0 && rs == 0) {
                            return String.format("%08x %10s: C.EBREAK\n",
                                    address, tag);
                        } else if (rs == 0) {
                            return String.format("%08x %10s: C.JALR %s\n",
                                    address, tag, xRegToAbi(rd));
                        } else {
                            return String.format("%08x %10s: C.ADD %s, %s\n",
                                    address, tag, xRegToAbi(rd), xRegToAbi(rs));
                        }
                    default:
                        return String.format("%08x %10s: BAD Q2\n",
                                address, tag);
                }
            default:  // FLDSP, LQSP, FLWSP, LDSP and so on
                return String.format("%08x %10s: UNKNOWN_COMMAND Q2\n",
                        address, tag);
        }
    }

    @Override
    public String toString() {
        int opcode = getBitSubseq(inst, 0, 6); // first 7 bits
        String answer = switch (opcode) {
            case 0b0110011 -> decodeRType();
            case 0b0000011, 0b0010011, 0b1100111, 0b1110011 -> decodeIType();
            case 0b0100011 -> decodeSType();
            case 0b1100011 -> decodeBType();
            case 0b0110111, 0b0010111 -> decodeUType();
            case 0b1101111 -> decodeJType();
            default -> null;
        };
        if (answer == null) {
            answer = switch (opcode & 0b11) {
                case 0b00 -> decodeQ0();
                case 0b01 -> decodeQ1();
                case 0b10 -> decodeQ2();
                default -> String.format("%08x %10s: UNKNOWN_COMMAND\n", address, tag);
            };
        }
        return answer;
    }
}
