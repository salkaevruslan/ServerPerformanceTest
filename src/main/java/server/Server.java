package server;

import java.io.IOException;

public interface Server { //TODO nado?
    void start() throws IOException;
    void shutdown();
}
