package results;

import java.util.concurrent.atomic.AtomicLong;

public class Results {
    private final AtomicLong sumTime = new AtomicLong(0);
    private final AtomicLong totalNumber = new AtomicLong(0);

    public void addResult(Long millis) {
        sumTime.addAndGet(millis);
        totalNumber.incrementAndGet();
    }

    public long getAverage() {
        if (totalNumber.get() == 0) {
            return 0;
        }
        return sumTime.get() / totalNumber.get();
    }
}
