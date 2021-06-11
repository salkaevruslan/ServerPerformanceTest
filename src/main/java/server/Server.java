package server;

import exception.ServerException;

import java.io.IOException;

public interface Server {
    void start() throws ServerException;

    void shutdown() throws ServerException;
}
