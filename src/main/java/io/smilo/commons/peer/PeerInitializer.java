package io.smilo.commons.peer;

import java.net.Socket;

public interface PeerInitializer {

    IPeer setUp(String identifier, String connectHost, int connectPort);

    IPeer initializePeer(String identifier, String connectHost, int connectPort);

    IPeer initializePeer(String identifier, Socket socket);
}
