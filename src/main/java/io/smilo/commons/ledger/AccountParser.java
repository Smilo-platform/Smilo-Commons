package io.smilo.commons.ledger;

import io.smilo.commons.block.data.Parser;
import org.apache.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.buffer.ArrayBufferInput;
import org.msgpack.core.buffer.MessageBufferInput;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
//private String address;
//private BigInteger balance;
//private int signatureCount;

@Component
public class AccountParser implements Parser<Account> {
    private static final Logger LOGGER = Logger.getLogger(AccountParser.class);
    private static final byte CURRENT_VERSION = (byte) 1;
    private static final StateParser stateParser = new StateParser();

    @Override
    public Account deserialize(byte[] raw) {
        if (raw.length == 0) return null;
        MessageBufferInput data = new ArrayBufferInput(raw);
        MessageUnpacker msgpack = MessagePack.newDefaultUnpacker(data);
        try {
            msgpack.unpackByte(); // Skip version number
            String address = msgpack.unpackString();
            BigInteger balance = msgpack.unpackBigInteger();
            int sigCount = msgpack.unpackInt();
            Account acc = new Account(address, balance, sigCount);
            int size = msgpack.unpackBinaryHeader();
            byte[] temp = msgpack.readPayload(size);
            acc.setCode(temp);
            acc.codeHash();
            size = msgpack.unpackBinaryHeader();
            temp = msgpack.readPayload(size);
            acc.setState(stateParser.deserialize(temp));
            return acc;
        } catch (ArrayIndexOutOfBoundsException | IOException ex) {
            LOGGER.error("Unable to deserialize account", ex);
        }
        return null;
    }

    @Override
    public byte[] serialize(Account data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MessagePacker msgpack = MessagePack.newDefaultPacker(out)) {
            msgpack.packByte(CURRENT_VERSION);
            msgpack.packString(data.getAddress());
            msgpack.packBigInteger(data.getBalance());
            msgpack.packInt(data.getSignatureCount());
            msgpack.packBinaryHeader(data.getCode().length);
            msgpack.addPayload(data.getCode());
            byte[] state = stateParser.serialize(data.getState());
            msgpack.packBinaryHeader(state.length);
            msgpack.addPayload(state);
        } catch (ArrayIndexOutOfBoundsException | IOException ex) {
            LOGGER.error("Unable to serialize account", ex);
        }
        return out.toByteArray();
    }

    @Override
    public byte[] serializeWithoutSignature(Account data) {
        return serialize(data);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Account.class.isAssignableFrom(clazz);
    }

}
