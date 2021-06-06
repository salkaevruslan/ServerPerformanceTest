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
    private DataInputStream input;
    private DataOutputStream output;
    public int id;


    public Client(CountDownLatch startLatch,
                  AtomicBoolean isCounting,
                  int iterations,
                  int dataSize,
                  Results results,
                  int id
    ) {
        this.startLatch = startLatch;
        this.isCounting = isCounting;
        this.iterations = iterations;
        this.results = results;
        values = DataGenerator.gen(dataSize);
        this.id = id;
    }

    @Override
    public Void call() {
        startLatch.countDown();
        System.out.println("Client " + id + "countdown" + startLatch.getCount());
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Client " + id + "start");
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
        return null;
    }

    private void readResponse() throws IOException {
        for (int i = 0; i < iterations; i++) {
            int responseId = StreamUtils.readData(input).getId();
            //TODO check data
            System.out.println("Client " + id + "recieved" + responseId);
            if (isCounting.get()) {
                results.addResult(timers.get(responseId).time());
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
                System.out.println("Client " + id + "sent" + i);
                try {
                    Thread.sleep(10); //TODO config
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
