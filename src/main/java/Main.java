import client.Client;
import config.Config;
import exception.ServerException;
import results.Results;
import server.Server;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static void main(String[] args) {
        Config.parseConfig();
        List<Config.ClientsConfig> clientsConfigs = Config.getClientsConfigs();
        for (Config.ClientsConfig clientsConfig : clientsConfigs) {
            try {
                long result = runTest(clientsConfig);
                System.out.println(result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static long runTest(Config.ClientsConfig clientsConfig) throws ServerException, ExecutionException, InterruptedException {
        ExecutorService clientsPool = Executors.newFixedThreadPool(clientsConfig.clientsNumber);
        CountDownLatch startLatch = new CountDownLatch(clientsConfig.clientsNumber + 1);
        AtomicBoolean isCounting = new AtomicBoolean(true);
        Results result = new Results();
        List<Future<Void>> futures = new LinkedList<>();
        for (int i = 0; i < clientsConfig.clientsNumber; i++) {
            futures.add(clientsPool.submit(new Client(
                    startLatch,
                    isCounting,
                    clientsConfig.clientIterations,
                    clientsConfig.dataArraySize,
                    clientsConfig.timeBetweenRequests,
                    result,
                    i
            )));
        }
        Server server = Config.createServer(startLatch);
        server.start();
        //TODO handle serverException
        for (Future<Void> future : futures) {
            future.get();
        }
        clientsPool.shutdown();
        server.shutdown();
        return result.getAverage();
    }
}
