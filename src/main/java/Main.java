import client.Client;
import results.Results;
import server.Server;
import server.blocking.BlockingServer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        ExecutorService clientsPool = Executors.newFixedThreadPool(5);
        int n = 5;
        CountDownLatch startLatch = new CountDownLatch(n + 1);
        AtomicBoolean isCounting = new AtomicBoolean(true);
        Results result = new Results();
        List<Future<Void>> futures = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            futures.add(clientsPool.submit(new Client(startLatch, isCounting, 100, 1000, result, i)));
        }
        Server server = new BlockingServer(startLatch, 2);
        server.start();
        for (Future<Void> future : futures) {
            future.get();
        }
        clientsPool.shutdown();
        server.shutdown();
        System.out.println("Result: " + result.getAverage());
    }
}
