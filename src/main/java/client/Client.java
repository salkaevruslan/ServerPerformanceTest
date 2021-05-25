package client;

import util.DataGenerator;
import util.Timer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable {
    private final ArrayList<Timer> timers = new ArrayList<>();
    private final ArrayList<Long> results;
    private final CountDownLatch startLatch;
    private final CountDownLatch stopLatch;
    private final AtomicBoolean isCounting;
    private SocketChannel channel;
    private final int iterations;
    private final int dataSize;


    public Client(CountDownLatch startLatch,
                  CountDownLatch stopLatch,
                  AtomicBoolean isCounting,
                  int iterations,
                  int dataSize,
                  ArrayList<Long> forResults
    ) {
        this.startLatch = startLatch;
        this.stopLatch = stopLatch;
        this.isCounting = isCounting;
        this.iterations = iterations;
        this.dataSize = dataSize;
        results = forResults;
    }

    @Override
    public void run() {
        startLatch.countDown();
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress("localhost", 228));
            Thread senderThread = new Thread(new DataSender());
            senderThread.start();
            getResponse();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isCounting.set(false);
        stopLatch.countDown();
    }

    private void getResponse() throws IOException {
        for (int i = 0; i < iterations; i++) {
            ByteBuffer arrSizeBuf = ByteBuffer.allocate(4);
            channel.read(arrSizeBuf);
            arrSizeBuf.flip();
            //TODO assertion?
            ByteBuffer dataBuf = ByteBuffer.allocate(4 * arrSizeBuf.getInt());
            while (dataBuf.hasRemaining()) {
                channel.read(dataBuf);
            }
            if (isCounting.get()) {
                results.add(timers.get(i).time());
            }
            //TODO do smth else?
        }
    }

    private class DataSender implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < iterations; i++) {
                ArrayList<Integer> data = DataGenerator.gen(dataSize);
                Timer timer = new Timer();
                timers.add(timer);
                timer.start();
                ByteBuffer arrSizeBuf = ByteBuffer.allocate(4);
                arrSizeBuf.asIntBuffer().put(dataSize);
                ByteBuffer dataBuf = ByteBuffer.allocate(4 * data.size());
                dataBuf.asIntBuffer().put(data.stream().mapToInt(x -> x).toArray());
                arrSizeBuf.flip();
                dataBuf.flip();
                ByteBuffer[] bufferArray = {arrSizeBuf, dataBuf};
                try {
                    channel.write(bufferArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(100); //TODO config
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
