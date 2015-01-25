package shared;

public class Errors {
    public static RuntimeException die(Throwable t) {
        throw new RuntimeException(t);
    }

    public static void die(String message) {
        throw new RuntimeException(message);
    }
}
