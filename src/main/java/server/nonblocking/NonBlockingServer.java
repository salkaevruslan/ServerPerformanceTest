package server.nonblocking;

import com.google.protobuf.InvalidProtocolBufferException;
import data.DataArray;
import exception.ServerException;
import server.Server;
import util.BubbleSorter;
import util.StreamUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NonBlockingServer implements Server {
    private final ExecutorService workers;
    private final CountDownLatch startLatch;
    private final int port;
    private ServerSocketChannel socketChannel;
    private final AtomicBoolean isWorking = new AtomicBoolean(false);
    private Selector inputSelector;
    private Selector outputSelector;
    private final Queue<ClientData> readRegisterQueue = new ConcurrentLinkedQueue<>();
    private final Queue<ClientData> writeRegisterQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService serverSocketService = Executors.newSingleThreadExecutor();
    private final ExecutorService requestThread = Executors.newSingleThreadExecutor();
    private final ExecutorService responseThread = Executors.newSingleThreadExecutor();

    public NonBlockingServer(CountDownLatch startLatch, int poolSize, int port) {
        this.startLatch = startLatch;
        workers = Executors.newFixedThreadPool(poolSize);
        this.port = port;
    }

    @Override
    public void start() throws ServerException {
        try {
            inputSelector = Selector.open();
            outputSelector = Selector.open();
            isWorking.set(true);
            socketChannel = ServerSocketChannel.open();
            socketChannel.bind(new InetSocketAddress(port));
            requestThread.submit(() -> {
                try {
                    readInput();
                } catch (IOException ignored) {
                }
            });
            responseThread.submit(() -> {
                try {
                    writeOutput();
                } catch (IOException ignored) {
                }
            });
            serverSocketService.submit(() -> acceptClients(socketChannel));
            startLatch.countDown();
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() throws ServerException {
        isWorking.set(false);
        serverSocketService.shutdown();
        responseThread.shutdown();
        requestThread.shutdown();
        workers.shutdown();
        try {
            inputSelector.close();
            outputSelector.close();
            socketChannel.close();
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    private void acceptClients(ServerSocketChannel serverSocketChannel) {
        try (ServerSocketChannel ignored = serverSocketChannel) {
            while (isWorking.get()) {
                SocketChannel socketChannel = serverSocketChannel.accept();
                socketChannel.configureBlocking(false);
                ClientData client = new ClientData(socketChannel);
                readRegisterQueue.add(client);
                inputSelector.wakeup();
            }
        } catch (IOException ignored) {
        }
    }

    private void readInput() throws IOException {
        while (isWorking.get()) {
            int readyChannels = inputSelector.select();
            if (readyChannels > 0) {
                Set<SelectionKey> keys = inputSelector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    SocketChannel channel = (SocketChannel) key.channel();
                    ClientData client = (ClientData) key.attachment();
                    if (client.isInfoReadDone.get()) {
                        int bytes = channel.read(client.dataBuffer);
                        if (bytes < 0) {
                            client.shutdown();
                            key.cancel();
                        }
                        if (!client.dataBuffer.hasRemaining()) {
                            processData(client);
                        }
                    } else {
                        int bytes = channel.read(client.infoBuffer);
                        if (bytes < 0) {
                            client.shutdown();
                            key.cancel();
                        }
                        if (!client.infoBuffer.hasRemaining()) {
                            processInfo(client);
                        }
                    }
                    it.remove();
                }
            }
            while (!readRegisterQueue.isEmpty()) {
                ClientData client = readRegisterQueue.poll();
                client.channel.register(inputSelector, SelectionKey.OP_READ, client);
            }
        }
    }

    private void processInfo(ClientData client) {
        client.infoBuffer.flip();
        int size = client.infoBuffer.getInt();
        client.infoBuffer.clear();
        client.dataBuffer = ByteBuffer.allocate(size);
        client.isInfoReadDone.set(true);
    }

    private void processData(ClientData client) {
        try {
            client.dataBuffer.flip();
            DataArray data = StreamUtils.readData(client.dataBuffer);
            workers.submit(() -> {
                BubbleSorter.sort(data.getValues());
                client.lock.lock();
                client.responses.add(StreamUtils.toByteBuffer(data));
                if (!client.isWriting) {
                    client.isWriting = true;
                    writeRegisterQueue.add(client);
                    client.lock.unlock();
                    outputSelector.wakeup();
                } else {
                    client.lock.unlock();
                }
            });
        } catch (InvalidProtocolBufferException ignored) {
            //invalid protocol = no task
        }
        client.dataBuffer.clear();
        client.isInfoReadDone.set(false);
    }

    private void writeOutput() throws IOException {
        while (isWorking.get()) {
            int readyChannels = outputSelector.select();
            if (readyChannels > 0) {
                Set<SelectionKey> keys = outputSelector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    SocketChannel channel = (SocketChannel) key.channel();
                    ClientData client = (ClientData) key.attachment();
                    ByteBuffer buffer = client.responses.peek();
                    int bytes = channel.write(buffer);
                    if (bytes < 0) {
                        client.shutdown();
                        key.cancel();
                    }
                    if (buffer != null && !buffer.hasRemaining()) {
                        client.lock.lock();
                        client.responses.remove();
                        if (client.responses.isEmpty()) {
                            client.isWriting = false;
                            key.cancel();
                        }
                        client.lock.unlock();
                    }
                    it.remove();
                }
            }
            while (!writeRegisterQueue.isEmpty()) {
                ClientData client = writeRegisterQueue.poll();
                client.channel.register(outputSelector, SelectionKey.OP_WRITE, client);
            }
        }
    }

    //TODO public/private everywhere
    private static class ClientData {
        public final SocketChannel channel;
        private ByteBuffer dataBuffer;
        private final ByteBuffer infoBuffer = ByteBuffer.allocate(Integer.BYTES);
        public AtomicBoolean isInfoReadDone = new AtomicBoolean(false);
        public Queue<ByteBuffer> responses = new ConcurrentLinkedQueue<>();
        public boolean isWriting = false;
        public Lock lock = new ReentrantLock();

        public ClientData(SocketChannel channel) {
            this.channel = channel;
        }

        public void shutdown() {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
