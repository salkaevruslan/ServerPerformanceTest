package server.async;

import com.google.protobuf.InvalidProtocolBufferException;
import data.DataArray;
import exception.ServerException;
import server.Server;
import util.BubbleSorter;
import util.StreamUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncServer implements Server {
    private final ExecutorService workers;
    private final CountDownLatch startLatch;
    private final int port;
    private AsynchronousServerSocketChannel asynchronousServerSocketChannel;

    public AsyncServer(CountDownLatch startLatch, int poolSize, int port) {
        this.startLatch = startLatch;
        workers = Executors.newFixedThreadPool(poolSize);
        this.port = port;
    }

    @Override
    public void start() throws ServerException {
        try {
            asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
            asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
            asynchronousServerSocketChannel.accept(asynchronousServerSocketChannel, new AcceptHandler());
            startLatch.countDown();
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    public void shutdown() throws ServerException {
        workers.shutdown();
        try {
            asynchronousServerSocketChannel.close();
        } catch (IOException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

        @Override
        public void completed(AsynchronousSocketChannel asynchronousSocketChannel, AsynchronousServerSocketChannel asynchronousServerSocketChannel) {
            asynchronousServerSocketChannel.accept(asynchronousServerSocketChannel, this);
            ClientData client = new ClientData(asynchronousSocketChannel);
            asynchronousSocketChannel.read(client.infoBuffer, client, new ReadHandler());
        }

        @Override
        public void failed(Throwable ex, AsynchronousServerSocketChannel assc) {
        }
    }

    private class ReadHandler implements CompletionHandler<Integer, ClientData> {

        @Override
        public void completed(Integer bytes, ClientData client) {
            if (bytes < 0) {
                client.shutdown();
                return;
            }
            if (client.isInfoReadDone.get()) {
                if (!client.dataBuffer.hasRemaining()) {
                    processData(client);
                    client.asynchronousSocketChannel.read(client.infoBuffer, client, this);
                } else {
                    client.asynchronousSocketChannel.read(client.dataBuffer, client, this);
                }
            } else {
                if (!client.infoBuffer.hasRemaining()) {
                    processInfo(client);
                    client.asynchronousSocketChannel.read(client.dataBuffer, client, this);
                } else {
                    client.asynchronousSocketChannel.read(client.infoBuffer, client, this);
                }
            }
        }

        @Override
        public void failed(Throwable exc, ClientData client) {
            client.shutdown();
        }
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
                    client.lock.unlock();
                    client.asynchronousSocketChannel.write(client.responses.peek(), client, new WriteHandler());
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

    private void processInfo(ClientData client) {
        client.infoBuffer.flip();
        int size = client.infoBuffer.getInt();
        client.infoBuffer.clear();
        client.dataBuffer = ByteBuffer.allocate(size);
        client.isInfoReadDone.set(true);
    }

    private static class WriteHandler implements CompletionHandler<Integer, ClientData> {

        @Override
        public void completed(Integer bytes, ClientData client) {
            if (bytes < 0) {
                client.shutdown();
                return;
            }
            if (!client.responses.isEmpty() && client.responses.peek().hasRemaining()) {
                client.asynchronousSocketChannel.write(client.responses.peek(), client, this);
            } else {
                client.lock.lock();
                client.responses.remove();
                if (!client.responses.isEmpty()) {
                    client.lock.unlock();
                    client.asynchronousSocketChannel.write(client.responses.peek(), client, this);
                } else {
                    client.isWriting = false;
                    client.lock.unlock();
                }
            }
        }

        @Override
        public void failed(Throwable exc, ClientData client) {
            client.shutdown();
        }
    }

    private static class ClientData {
        private final AsynchronousSocketChannel asynchronousSocketChannel;
        private ByteBuffer dataBuffer;
        private final ByteBuffer infoBuffer = ByteBuffer.allocate(Integer.BYTES);
        private final AtomicBoolean isInfoReadDone = new AtomicBoolean(false);
        private final Queue<ByteBuffer> responses = new ConcurrentLinkedQueue<>();
        private boolean isWriting = false;
        private final Lock lock = new ReentrantLock();

        public ClientData(AsynchronousSocketChannel asynchronousSocketChannel) {
            this.asynchronousSocketChannel = asynchronousSocketChannel;
        }

        public void shutdown() {
            try {
                asynchronousSocketChannel.close();
            } catch (IOException ignored) {
            }
        }
    }
}
