package project;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        ElfReader reader = new ElfReader(args[0]);
        ElfParser parser = new ElfParser(reader.getText());
        try (PrintWriter out = new PrintWriter(args[1], StandardCharsets.UTF_8)) {
            parser.parse(out);
        } catch (IOException e) {
            System.err.println("Problems with file");
        }
    }
}
