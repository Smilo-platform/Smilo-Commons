/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smilo.commons.peer.payloadhandler;

import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.peer.Capability;
import io.smilo.commons.peer.IPeer;
import io.smilo.commons.peer.PeerEncoder;
import org.apache.log4j.Logger;
import org.spongycastle.util.encoders.Hex;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RequestIdentifierHandler implements PayloadHandler {

    private final static Logger LOGGER = Logger.getLogger(io.smilo.commons.peer.payloadhandler.RequestIdentifierHandler.class);

    private final AddressManager addressManager;
    private final PeerEncoder peerEncoder;

    public RequestIdentifierHandler(AddressManager addressManager, PeerEncoder peerEncoder) {
        this.addressManager = addressManager;
        this.peerEncoder = peerEncoder;
    }

    @Override
    public void handlePeerPayload(List<String> parts, IPeer peer) {
        byte[] capabilities = peerEncoder.encodeCapabilties(RequestIdentifierHandler.CAPABILITIES);

        peer.write(PayloadType.RESPOND_IDENTIFIER.name() + " " + addressManager.getDefaultAddress() + " " + Hex.toHexString(capabilities));
    }

    // List of all capabilities supported by this client
    public static final List<Capability> CAPABILITIES = new ArrayList<Capability>() {{
        add(new Capability(Capability.P2P, (byte)1));
    }};

    @Override
    public PayloadType supports() {
        return PayloadType.REQUEST_IDENTIFIER;
    }
}
