# ELF disassembler for RISC-V
Disassembler for ELF binary format, which supports RISC-V assembler(RV32I, RV32M and RVC instruction sets).

Sections `.text` and `.symtab` are disassembled, jumps are labeled automatically. 
Labels from symbol table are also supported.

There are two examples of disassembling.

# Usage
Executable requires two arguments - path of ELF binary and output file name.