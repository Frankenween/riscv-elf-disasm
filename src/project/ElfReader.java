package project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ElfReader {
    private byte[] text;

    ElfReader(String path) {
        try {
            text = Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            System.err.println("Couldn't read file");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (OutOfMemoryError e) {
            System.err.println("File is too big or not enough memory was provided");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public byte[] getText() {
        return text;
    }
}
