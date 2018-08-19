package io.smilo.commons.ledger;

import io.smilo.commons.block.data.Parser;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.ArrayBufferInput;
import org.msgpack.core.buffer.MessageBufferInput;

import java.io.IOException;
import java.util.Map;

public class StateParser implements Parser<State> {
    private static final Logger LOGGER = Logger.getLogger(StateParser.class);
    private static final byte CURRENT_VERSION = (byte) 1;

    @Override
    public State deserialize(byte[] raw) {
        State state = new State();
        if (raw.length == 0) return state;
        MessageBufferInput data = new ArrayBufferInput(raw);
        MessageUnpacker msgpack = MessagePack.newDefaultUnpacker(data);
        try {
            msgpack.unpackByte(); // Ignore version
            int items = msgpack.unpackArrayHeader();
            for (int i = 0; i < items/2; i++) {
                int size = msgpack.unpackBinaryHeader();
                byte[] key = msgpack.readPayload(size);
                size = msgpack.unpackBinaryHeader();
                byte[] value = msgpack.readPayload(size);
                state.put(key, value);
            }
            // If all is going right, the following code is unreachable.
            if (items % 2 == 1) {
                int size = msgpack.unpackBinaryHeader();
                byte[] key = msgpack.readPayload(size);
                state.put(key, null);
                throw new IOException("Lack of value, adding a (key,null)");
            }
        } catch (IOException e) {
            LOGGER.error("Unable to deserialize State properly",e);
        }
        return state;
    }

    @Override
    public byte[] serialize(State data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MessagePacker msgpack = MessagePack.newDefaultPacker(out)) {
            msgpack.packByte(CURRENT_VERSION);
            Map<byte[],byte[]> entries = data.getAll();
            msgpack.packArrayHeader(entries.size()*2);
            for (Map.Entry<byte[], byte[]> entry : entries.entrySet()) {
                msgpack.packBinaryHeader(entry.getKey().length);
                msgpack.addPayload(entry.getKey());
                msgpack.packBinaryHeader(entry.getValue().length);
                msgpack.addPayload(entry.getValue());
            }
            msgpack.flush();
            return out.toByteArray();
        } catch (ArrayIndexOutOfBoundsException | IOException ex) {
            LOGGER.error("Unable to serialize State", ex);
        }
        return null;
    }

    @Override
    public byte[] serializeWithoutSignature(State data) {
        return serialize(data);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return State.class.isAssignableFrom(clazz);
    }
}
