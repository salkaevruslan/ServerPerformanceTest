package server.blocking;

import data.DataArray;
import exception.ServerException;
import server.Server;
import util.BubbleSorter;
import util.StreamUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockingServer implements Server {
    private final ExecutorService workers;
    private final ExecutorService serverSocketService = Executors.newSingleThreadExecutor();
    private final ConcurrentLinkedQueue<ClientData> clients = new ConcurrentLinkedQueue<>();
    private final CountDownLatch startLatch;
    private final int port;
    private ServerSocket socket;
    private final AtomicBoolean isWorking = new AtomicBoolean();

    public BlockingServer(CountDownLatch startLatch, int poolSize, int port) {
        this.startLatch = startLatch;
        workers = Executors.newFixedThreadPool(poolSize);
        this.port = port;
    }

    public void start() throws ServerException {
        try {
            socket = new ServerSocket(port);
            startLatch.countDown();
            // System.out.println("Server countdown");
            try {
                startLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // System.out.println("Server started");
            isWorking.set(true);
            serverSocketService.submit(() -> acceptClients(socket));
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    private void acceptClients(ServerSocket serverSocket) {
        try (ServerSocket ignored = serverSocket) {
            while (isWorking.get()) {
                Socket socket = serverSocket.accept();
                ClientData client = new ClientData(socket);
                clients.add(client);
                client.process();
            }
        } catch (IOException ignored) {
        }
    }


    @Override
    public void shutdown() throws ServerException {
        isWorking.set(false);
        serverSocketService.shutdown();
        workers.shutdown();
        clients.forEach(ClientData::shutdown);
        try {
            socket.close();
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    private class ClientData {
        private final Socket socket;
        private final ExecutorService responseWriter = Executors.newSingleThreadExecutor();
        private final ExecutorService requestReader = Executors.newSingleThreadExecutor();

        private final DataInputStream inputStream;
        private final DataOutputStream outputStream;


        public ClientData(Socket socket) throws IOException {
            this.socket = socket;
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
        }

        public void process() {
            requestReader.submit(() -> {
                try (Socket ignored = socket) {
                    while (isWorking.get() && socket.isConnected()) {
                        DataArray data = StreamUtils.readData(inputStream);
                        // System.out.println("Server: " + id + " request id: " + data.getId() + " " + System.currentTimeMillis());
                        workers.submit(() -> {
                            BubbleSorter.sort(data.getValues());
                            responseWriter.submit(() -> {
                                try {
                                    // System.out.println("Server: " + id + " response id: " + data.getId() + " " + System.currentTimeMillis());
                                    StreamUtils.writeData(outputStream, data);
                                } catch (IOException ignored1) {
                                }
                            });
                        });
                    }
                } catch (IOException ignored) {
                } finally {
                    shutdown();
                }
            });
        }

        public void shutdown() {
            responseWriter.shutdown();
            requestReader.shutdown();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
