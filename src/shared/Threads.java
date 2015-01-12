package shared;

public class Threads {
    public static void fork(Runnable r) {
        new Thread(r).start();
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
