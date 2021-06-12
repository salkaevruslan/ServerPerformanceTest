import client.Client;
import config.Config;
import exception.ServerException;
import results.Results;
import server.Server;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {

    public static void main(String[] args) {
        try {
            Config.parseConfig();
        } catch (IOException e) {
            System.err.println("Cannot open file");
            e.printStackTrace();
            return;
        }
        System.out.println("Server type:" + Config.serverType);
        System.out.println("Server pool size:" + Config.serverPoolSize);
        System.out.println("Dynamic param:" + Config.dynamicParam);
        System.out.println("Client iterations:" + Config.clientIterations);
        if (Config.dynamicParam != Config.Param.DATA_ARRAY_SIZE) {
            System.out.println("Data array size:" + Config.dataArraySize);
        }
        if (Config.dynamicParam != Config.Param.CLIENTS_NUMBER) {
            System.out.println("Clients number:" + Config.clientsNumber);
        }
        if (Config.dynamicParam != Config.Param.TIME_BETWEEN_REQUESTS) {
            System.out.println("Time between requests:" + Config.timeBetweenRequests);
        }
        List<Config.ClientsConfig> clientsConfigs = Config.getClientsConfigs();
        for (Config.ClientsConfig clientsConfig : clientsConfigs) {
            try {
                Results result = runTest(clientsConfig);
                if (Config.dynamicParam == Config.Param.DATA_ARRAY_SIZE) {
                    System.out.println(clientsConfig.dataArraySize + " " + result.getAverage());
                }
                if (Config.dynamicParam == Config.Param.CLIENTS_NUMBER) {
                    System.out.println(clientsConfig.clientsNumber + " " + result.getAverage());
                }
                if (Config.dynamicParam == Config.Param.TIME_BETWEEN_REQUESTS) {
                    System.out.println(clientsConfig.timeBetweenRequests + " " + result.getAverage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Results runTest(Config.ClientsConfig clientsConfig) throws ServerException, ExecutionException, InterruptedException {
        ExecutorService clientsPool = Executors.newCachedThreadPool();
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
                    i,
                    Config.serverPort
            )));
        }
        Server server = Config.createServer(startLatch);
        server.start();
        for (Future<Void> future : futures) {
            future.get();
        }
        clientsPool.shutdown();
        server.shutdown();
        return result;
    }
}
