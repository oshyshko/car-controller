package shared;

public class Sleeper {
    private long last = System.currentTimeMillis();

    public void sleep(long millis) {
        long now = System.currentTimeMillis();
        long sinceLast = now - last;
        long intervalAdjusted = millis - sinceLast;
        long intervalToSleep = intervalAdjusted <= 0
                ? millis
                : intervalAdjusted;

        if (intervalAdjusted <= 0) {
            IO.println("! A sleeper has bee called too late (solution: increase sleep interval): target " + millis + " VS calculated " + intervalAdjusted);
        }

        last = now + intervalToSleep;
        Threads.sleep(intervalToSleep);
    }
}
