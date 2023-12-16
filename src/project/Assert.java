package project;

public class Assert {
    public static void ensure(boolean val, String format, Object... data) {
        if (!val) {
            throw new AssertionError(String.format(format, data));
        }
    }
}
