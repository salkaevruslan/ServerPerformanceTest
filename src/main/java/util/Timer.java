package util;

import java.util.concurrent.atomic.AtomicLong;

public class Timer {
    private final AtomicLong start = new AtomicLong(0);

    public void start() {
        start.set(System.currentTimeMillis());
    }

    public long time() {
        return System.currentTimeMillis() - start.get();
    }
}
