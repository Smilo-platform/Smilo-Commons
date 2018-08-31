package io.smilo.commons.peer;

import io.smilo.commons.peer.IPeer;
import io.smilo.commons.peer.PeerInitializer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.Socket;

@Component
@Profile("test")
public class MockPeerInitializer implements PeerInitializer {

    @Override
    public IPeer initializePeer(String identifier, InetAddress inetAddress, int port) {
        return new MockPeer(identifier, inetAddress, port);
    }

    @Override
    public IPeer initializePeer(String identifier, Socket socket) {
        int remotePort = socket.getPort();
        return new MockPeer(identifier, socket.getInetAddress(), remotePort);
    }
}
