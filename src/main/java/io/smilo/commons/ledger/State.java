package io.smilo.commons.ledger;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class State {
    private static final Logger LOGGER = Logger.getLogger(State.class);
    Map<ByteBuffer, byte[]> internalState = new HashMap<>();

    public void put(byte[] key, byte[] value) {
        this.internalState.put(wrapBytes(key), value);
    }

    public byte[] get(byte[] key) {
        return this.internalState.get(wrapBytes(key));
    }

    private ByteBuffer wrapBytes(byte[] key) {
        ByteBuffer kb = ByteBuffer.allocate(32);
        kb.put(key).flip();
        return kb;
    }

    public void putAll(Map<byte[],byte[]> entries) {
        entries.entrySet().stream().forEach(entry -> { put(entry.getKey(), entry.getValue()); });
    }

    public Map<byte[], byte[]> getAll() {
        return this.internalState.entrySet().stream()
                .collect(Collectors.toMap(p -> {
                        ByteBuffer bb = ((ByteBuffer)((Map.Entry)p).getKey());
                        bb.flip();
                        return bb.array();
                    },
                        p -> (byte[])((Map.Entry)p).getValue()));
    }
}
