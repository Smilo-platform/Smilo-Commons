package io.smilo.commons.peer;

import io.smilo.commons.ledger.MerkleTreeGenerator;
import io.smilo.commons.peer.Capability;
import io.smilo.commons.peer.PeerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Component
public class PeerBuilder {

    @Autowired
    private PeerClient peerClient;

    @Autowired
    private MerkleTreeGenerator treeGen;

    public PeerBuildCommand blank() {
        return new PeerBuildCommand().blank("S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR","localhost", 80);
    }

    public PeerBuildCommand blank(String hostname, int port) {
        return new PeerBuildCommand().blank("S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR", hostname, port);
    }

    public PeerBuildCommand peer_ready() {
        return blank().withInitialized(true);
    }

    public PeerBuildCommand peer_ready_with_random_address() {
        String address = treeGen.generateMerkleTree(UUID.randomUUID().toString(), 14, 16, 128);
        return peer_ready().withIdentifier(address);
    }

    public PeerBuildCommand peer_uninitialized() {
        return blank().withInitialized(false);
    }

    public class PeerBuildCommand {

        private MockPeer peer;

        public PeerBuildCommand blank(String identifier, String hostname, int port) {
            try {
                InetAddress inetAddress = InetAddress.getByName(hostname);
                this.peer = new MockPeer(identifier, inetAddress, port);
                this.peer.setLastSeen(System.currentTimeMillis());
                return this;
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        public PeerBuildCommand withInitialized(boolean initialized) {
            peer.setInitialized(initialized);
            return this;
        }

        public PeerBuildCommand withRemoteHost(String remoteHost) {
            try {
                peer.setAddress(InetAddress.getByName(remoteHost));
            } catch (UnknownHostException e) {
                throw new RuntimeException((e));
            }
            return this;
        }

        public PeerBuildCommand withRemotePort(int remotePort) {
            peer.setRemotePort(remotePort);
            return this;
        }

        public PeerBuildCommand withIdentifier(String identifier) {
            peer.setIdentifier(identifier);
            return this;
        }

        public PeerBuildCommand withLastSeen(Long timestamp) {
            peer.setLastSeen(timestamp);
            return this;
        }

        public PeerBuildCommand withCapability(String capability, byte version) {
            Capability c = new Capability(capability, version);
            return withCapability(c);
        }

        public PeerBuildCommand withCapability(Capability capability) {
            peer.getCapabilities().add(capability);
            return this;
        }

        public MockPeer save() {
            peerClient.connectToPeer(peer);
            return peer;
        }

        public MockPeer construct() {
            return peer;
        }
    }

}
