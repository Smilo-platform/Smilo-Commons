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

package io.smilo.commons.peer;

import com.google.common.base.Preconditions;
import org.apache.commons.codec.Charsets;
import org.apache.log4j.Logger;
import org.ethereum.util.ByteUtil;
import org.ethereum.util.RLP;
import org.ethereum.util.RLPList;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

@Component
public class PeerEncoder {

    private static final Logger LOGGER = Logger.getLogger(PeerEncoder.class);

    private final PeerInitializer peerInitializer;

    public PeerEncoder(PeerInitializer peerInitializer) {
        this.peerInitializer = peerInitializer;
    }

    /**
     * Decode the byte array to a IPeer implementation
     *
     * @param raw byte array to decodeFromBase64
     * @return IPeer implementation
     */
    public IPeer decode(byte[] raw) {
        RLPList list = (RLPList) RLP.decode2(raw).get(0);

        byte[] ipBytes = list.get(0).getRLPData();
        byte[] portBytes = list.get(1).getRLPData();
        byte[] peerIdRaw = list.get(2).getRLPData();
        List<Capability> capabilities = decodeCapabilities(list.get(3).getRLPData());

        try {
            int peerPort = ByteUtil.byteArrayToInt(portBytes);

            String peerIdentifier = peerIdRaw == null ? "" : new String(peerIdRaw, Charsets.UTF_8);
            String host = ipBytes == null ? "" : new String(ipBytes, Charsets.UTF_8);
            Preconditions.checkArgument(!isEmpty(host), "Connect host is empty!");
            Preconditions.checkArgument(peerPort > 0 , "Connect port is empty!");

            IPeer peer = peerInitializer.setUp(peerIdentifier, host, peerPort);
            peer.setConnectPort(peerPort);
            peer.setConnectHost(host);

            if (peer != null) {
                peer.setCapabilities(capabilities);
            }
            return peer;
        } catch (Exception e) {
            LOGGER.error("Unable to decode peer", e);
            return null;
        }
    }

    /**
     * Encodes the peer implementation to a byte array.
     * Will encodeToBase64 the fields address, remote port and identifier.
     *
     * @param peer peer to encodeToBase64
     * @return serialized byte array
     */
    public byte[] encode(IPeer peer) {
        Preconditions.checkArgument(!isEmpty(peer.getConnectHost()), "Connect host is empty!");
        Preconditions.checkArgument(peer.getConnectPort() > 0 , "Connect port is empty!");

        byte[] ip = RLP.encodeElement(peer.getConnectHost().getBytes(StandardCharsets.UTF_8));
        byte[] port = RLP.encodeInt(peer.getConnectPort());
        byte[] peerId = RLP.encodeElement(peer.getIdentifier().getBytes(StandardCharsets.UTF_8));

        byte[] capabilities = encodeCapabilties(peer.getCapabilities());

        byte[][] encodedPeer = new byte[4][];
        encodedPeer[0] = ip;
        encodedPeer[1] = port;
        encodedPeer[2] = peerId;
        encodedPeer[3] = capabilities;


        return RLP.encodeList(encodedPeer);
    }

    /**
     * Encodes the peer capabilities to a byte array
     *
     * @param capabilities capabilities to encodeToBase64
     * @return encoded byte array
     */
    public byte[] encodeCapabilties(List<Capability> capabilities) {
        byte[][] encodedCaps = new byte[capabilities.size() * 2][];
        for (int i = 0; i < capabilities.size(); i++) {
            encodedCaps[i * 2] = RLP.encodeElement(capabilities.get(i).getName().getBytes(StandardCharsets.UTF_8));
            encodedCaps[i * 2 + 1] = RLP.encodeByte(capabilities.get(i).getVersion());
        }
        return RLP.encodeList(encodedCaps);
    }

    /**
     * Decodes an encoded byte array of capabilities
     *
     * @param bytes capabilities byte array
     * @return decoded list of capabilities
     */
    public List<Capability> decodeCapabilities(byte[] bytes) {
        RLPList capabilitiesList = (RLPList) RLP.decode2(bytes).get(0);
        List<Capability> capabilities = new ArrayList<>();

        for (int i = 0; i < capabilitiesList.size(); i = i + 2) {
            byte[] capability = capabilitiesList.get(i).getRLPData();
            byte version = capabilitiesList.get(i + 1).getRLPData()[0];

            String capabilityName = new String(capability, StandardCharsets.UTF_8);
            Capability cap = new Capability(capabilityName, version);
            capabilities.add(cap);
        }

        return capabilities;
    }

}