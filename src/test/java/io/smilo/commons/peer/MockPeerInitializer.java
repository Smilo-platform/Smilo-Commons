package io.smilo.commons.peer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.Socket;

@Component
@Profile("test")
public class MockPeerInitializer implements PeerInitializer {

    @Override
    public IPeer setUp(String identifier, String connectHost, int connectPort) {
       return new MockPeer(identifier, connectHost, connectPort);
    }

    @Override
    public IPeer initializePeer(String identifier, String connectHost, int connectPort) {
        return new MockPeer(identifier, connectHost, connectPort);
    }

    @Override
    public IPeer initializePeer(String identifier, Socket socket) {
        return new MockPeer(identifier);
    }
}
