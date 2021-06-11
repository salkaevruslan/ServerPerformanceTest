package config;

import server.Server;
import server.async.AsyncServer;
import server.blocking.BlockingServer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Config {
    public static ServerType serverType;
    public static Param dynamicParam;
    public static int serverPoolSize;
    public static int clientsNumber;
    public static int timeBetweenRequests;
    public static int dataArraySize;
    public static int clientIterations;
    public static int lowerBound;
    public static int upperBound;
    public static int step;

    private enum ServerType {
        BLOCKING {
            @Override
            public Server createServer(CountDownLatch startLatch) {
                return new BlockingServer(startLatch, serverPoolSize);
            }
        },
        ASYNC {
            @Override
            public Server createServer(CountDownLatch startLatch) {
                return new AsyncServer(startLatch, serverPoolSize);
            }
        },
        NON_BLOCKING {
            @Override
            public Server createServer(CountDownLatch startLatch) {
                return null;
            }
        };

        public abstract Server createServer(CountDownLatch startLatch);
    }

    public static Server createServer(CountDownLatch startLatch) {
        return serverType.createServer(startLatch);
    }

    private enum Param {
        DATA_ARRAY_SIZE,
        TIME_BETWEEN_REQUESTS,
        CLIENTS_NUMBER
    }

    public static void parseConfig() {

    }

    public static List<ClientsConfig> getClientsConfigs() {
        List<ClientsConfig> result = new LinkedList<>();
        for (int value = lowerBound; value <= upperBound; value += step) {
            if (dynamicParam == Param.DATA_ARRAY_SIZE) {
                result.add(new ClientsConfig(clientsNumber, timeBetweenRequests, value, clientIterations));
                continue;
            }
            if (dynamicParam == Param.TIME_BETWEEN_REQUESTS) {
                result.add(new ClientsConfig(clientsNumber, value, dataArraySize, clientIterations));
                continue;
            }
            if (dynamicParam == Param.CLIENTS_NUMBER) {
                result.add(new ClientsConfig(value, timeBetweenRequests, dataArraySize, clientIterations));
            }
        }
        return result;
    }

    public static class ClientsConfig {
        public int clientsNumber;
        public int timeBetweenRequests;
        public int dataArraySize;
        public int clientIterations;

        public ClientsConfig(int clientsNumber,
                             int timeBetweenRequests,
                             int dataArraySize,
                             int clientIterations) {
            this.clientsNumber = clientsNumber;
            this.clientIterations = clientIterations;
            this.dataArraySize = dataArraySize;
            this.timeBetweenRequests = timeBetweenRequests;
        }
    }
}
