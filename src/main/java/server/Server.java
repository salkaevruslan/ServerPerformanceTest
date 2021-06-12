package server;

import exception.ServerException;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public interface Server {
    void start() throws ServerException;

    void shutdown() throws ServerException;
}
