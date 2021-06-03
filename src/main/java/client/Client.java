package client;

import data.DataArray;
import results.Results;
import util.DataGenerator;
import util.StreamUtils;
import util.Timer;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable {
    private final ArrayList<Timer> timers = new ArrayList<>();
    private final Results results;
    private final CountDownLatch startLatch;
    private final CountDownLatch stopLatch;
    private final AtomicBoolean isCounting;
    private final int iterations;
    private final int dataSize;
    private final ArrayList<Integer> values;
    private DataInputStream input;
    private DataOutputStream output;


    public Client(CountDownLatch startLatch,
                  CountDownLatch stopLatch,
                  AtomicBoolean isCounting,
                  int iterations,
                  int dataSize,
                  Results results
    ) {
        this.startLatch = startLatch;
        this.stopLatch = stopLatch;
        this.isCounting = isCounting;
        this.iterations = iterations;
        this.dataSize = dataSize;
        this.results = results;
        values = DataGenerator.gen(dataSize);
    }

    @Override
    public void run() {
        startLatch.countDown();
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try (Socket socket = new Socket("localhost", 228)) {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            Thread senderThread = new Thread(new DataSender());
            senderThread.start();
            readResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isCounting.set(false);
        stopLatch.countDown();
    }

    private void readResponse() throws IOException {
        for (int i = 0; i < iterations; i++) {
            int id = StreamUtils.readData(input).getId();
            //TODO check data
            if (isCounting.get()) {
                results.addResult(timers.get(id).time());
            }
            //TODO do smth else?
        }
    }


    private class DataSender implements Runnable {
        @Override
        public void run() {
            int[] valuesAsArray = values.stream().mapToInt(x -> x).toArray();
            for (int i = 0; i < iterations; i++) {
                Timer timer = new Timer();
                timers.add(timer);
                timer.start();
                try {
                    StreamUtils.writeData(output, new DataArray(i, valuesAsArray));
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
