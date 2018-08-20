package io.smilo.commons.peer;

import io.smilo.commons.db.Store;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

@Component
public class PeerStore {

    private final static String COLLECTION_NAME = "peer";

    private final Store store;
    private final PeerEncoder peerEncoder;

    private Map<String, IPeer> peers = new HashMap<>();

    public PeerStore(Store store, PeerEncoder peerEncoder) {
        this.store = store;
        this.peerEncoder = peerEncoder;
        store.initializeCollection(COLLECTION_NAME);
    }

    public void save(IPeer peer) {
        store.put(COLLECTION_NAME, peer.getIdentifier().getBytes(UTF_8), peerEncoder.encode(peer));
        peers.put(peer.getIdentifier(), peer);
    }

    public void remove(IPeer peer) {
        boolean result = store.remove(COLLECTION_NAME, peer.getIdentifier().getBytes(UTF_8));
        if (result) {
            peers.remove(peer.getIdentifier());
        }
    }

    public Collection<IPeer> getPeers() {
        if (peers.isEmpty()) {
            Set<IPeer> peers =  store.getAll(COLLECTION_NAME).entrySet()
                    .stream()
                    .map(entry -> peerEncoder.decode(entry.getValue()))
                    .filter(Objects::nonNull)
                    .collect(toSet());
            peers.forEach(p -> this.peers.put(p.getIdentifier(), p));

        }
        return this.peers.values();
    }

    public IPeer getPeer(String identifier) {
        if (peers.isEmpty()) {
            getPeers();
        }
        return this.peers.get(identifier);
    }

    public void clear() {
        store.clear(COLLECTION_NAME);
        peers.clear();
    }

}
