package config;

import server.Server;
import server.async.AsyncServer;
import server.blocking.BlockingServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    public static int serverPort;

    private enum ServerType {
        BLOCKING {
            @Override
            public Server createServer(CountDownLatch startLatch) {
                return new BlockingServer(startLatch, serverPoolSize, serverPort);
            }
        },
        ASYNC {
            @Override
            public Server createServer(CountDownLatch startLatch) {
                return new AsyncServer(startLatch, serverPoolSize, serverPort);
            }
        },
        NON_BLOCKING {
            @Override
            public Server createServer(CountDownLatch startLatch) {
                return null;
            }
        };

        public abstract Server createServer(CountDownLatch startLatch);

        private static ServerType getFromString(String s) {
            switch (s) {
                case ("ASYNC"):
                    return ASYNC;
                case ("BLOCKING"):
                    return BLOCKING;
                case ("NON_BLOCKING"):
                    return NON_BLOCKING;
                default:
                    return null;
            }
        }
    }

    public static Server createServer(CountDownLatch startLatch) {
        return serverType.createServer(startLatch);
    }

    public enum Param {
        DATA_ARRAY_SIZE,
        TIME_BETWEEN_REQUESTS,
        CLIENTS_NUMBER;

        private static Param getFromString(String s) {
            switch (s) {
                case ("DATA_ARRAY_SIZE"):
                    return DATA_ARRAY_SIZE;
                case ("CLIENTS_NUMBER"):
                    return CLIENTS_NUMBER;
                case ("TIME_BETWEEN_REQUESTS"):
                    return TIME_BETWEEN_REQUESTS;
                default:
                    return null;
            }
        }
    }

    public static void parseConfig() throws IOException {
        Files.lines(Paths.get("src/main/resources/config.cfg")).forEach(line ->
                {
                    String value = getValue(line);
                    if (line.startsWith("SERVER_TYPE")) {
                        serverType = ServerType.getFromString(value);
                        return;
                    }
                    if (line.startsWith("SERVER_THREADS")) {
                        serverPoolSize = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("CLIENT_ITERATIONS")) {
                        clientIterations = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("DYNAMIC_PARAM")) {
                        dynamicParam = Param.getFromString(value);
                        return;
                    }
                    if (line.startsWith("LOWER_BOUND")) {
                        lowerBound = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("UPPER_BOUND")) {
                        upperBound = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("STEP")) {
                        step = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("CLIENTS_NUMBER")) {
                        clientsNumber = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("DATA_ARRAY_SIZE")) {
                        dataArraySize = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("TIME_BETWEEN_REQUESTS")) {
                        timeBetweenRequests = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("PORT")) {
                        serverPort = Integer.parseInt(value);
                        return;
                    }
                    if (line.startsWith("METRIC_TYPE")) {
                        //TODO add metric type setting
                    }
                }
        );
    }

    public static String getValue(String configString) {
        int idx = configString.indexOf('=');
        if (idx == -1) {
            return "";
        } else {
            return configString.substring(1 + idx);
        }
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
