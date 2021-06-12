package client;

import data.DataArray;
import results.Results;
import util.DataGenerator;
import util.StreamUtils;
import util.Timer;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Callable<Void> {
    private final ArrayList<Timer> timers = new ArrayList<>();
    private final Results results;
    private final CountDownLatch startLatch;
    private final AtomicBoolean isCounting;
    private final int iterations;
    private final ArrayList<Integer> values;
    private final int timeBetweenRequests;
    private final int port;
    private DataInputStream input;
    private DataOutputStream output;
    public int id;


    public Client(CountDownLatch startLatch,
                  AtomicBoolean isCounting,
                  int iterations,
                  int dataSize,
                  int timeBetweenRequests,
                  Results results,
                  int id,
                  int port
    ) {
        this.startLatch = startLatch;
        this.isCounting = isCounting;
        this.iterations = iterations;
        this.results = results;
        this.timeBetweenRequests = timeBetweenRequests;
        values = DataGenerator.gen(dataSize);
        this.id = id;
        this.port = port;
    }

    @Override
    public Void call() {
        startLatch.countDown();
        // System.out.println("Client " + id + "countdown" + startLatch.getCount());
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // System.out.println("Client " + id + "start");
        try (Socket socket = new Socket("localhost", port)) {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
            Thread senderThread = new Thread(new DataSender());
            senderThread.start();
            readResponse();
        } catch (IOException ignored) {
        }
        isCounting.set(false);
        return null;
    }

    private void readResponse() throws IOException {
        for (int i = 0; i < iterations; i++) {
            DataArray data = StreamUtils.readData(input);
            if (isCounting.get()) {
                results.addResult(timers.get(data.getId()).time());
            }
            // System.out.println("Response " + id + " id: " + data.getId());
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
                }
                // System.out.println("Request " + id + " id: " + i);
                try {
                    Thread.sleep(timeBetweenRequests);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
