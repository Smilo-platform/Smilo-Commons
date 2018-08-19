package io.smilo.commons.peer;

import java.net.InetAddress;
import java.net.Socket;

public interface PeerInitializer {

    IPeer initializePeer(String identifier, InetAddress address, int port);

    IPeer initializePeer(String identifier, Socket socket);
}
