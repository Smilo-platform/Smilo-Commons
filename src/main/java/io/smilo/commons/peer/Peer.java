/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.peer;

import org.apache.log4j.Logger;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

public class Peer implements Runnable, IPeer {

    private final static Logger LOGGER = Logger.getLogger(Peer.class);
    private String identifier;
    private Socket socket;
    private InetAddress address;
    private int remotePort;
    private PeerInput peerInput;
    private PeerOutput peerOutput;
    private boolean initialized;
    private Long lastSeen;
    private Long lastPing;
    private List<Capability> capabilities = new ArrayList<>();
    private int connectionAttempts = 0;
    private final static int PORT = 8020;

    /**
     * Constructor sets socket
     *
     * @param socket Socket with peer
     */
    public Peer(String identifier, Socket socket) {
        this.socket = socket;
        this.initialized = false;
        this.address = socket.getInetAddress();
        this.remotePort = PORT;
        this.identifier = identifier;
        setKeepAlive();
    }

    public Peer(String identifier, InetAddress address, int port) throws IOException {
        this(identifier, new Socket(address, port));
        this.identifier = identifier;
        this.remotePort = port;
    }

    public Peer() {

    }

    /**
     * As the name might suggest, each PeerThread runs on its own thread. Additionally, each child network IO thread runs on its own thread.
     */
    @Override
    public void run() {
        String id = (identifier != "") ? identifier : socket.getInetAddress().toString();

        LOGGER.info("Got connection from " + id + ".");
        peerInput = new PeerInput(socket);
        peerInput.start();
        peerOutput = new PeerOutput(socket);
        initialized = true;
        LOGGER.info("Initialized connection " + id);
        peerOutput.run();
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public List<String> readData() {
        if (initialized) {
            return peerInput.readData();
        } else {
            LOGGER.warn("Peer was not yet initialized! Not reading data!");
            return new ArrayList<>();
        }
    }

    @Override
    public void write(String string) {
        if (peerOutput == null) {
            LOGGER.error("Trying to write to peerOutput, but its null ... " + string);
            return;
        }
        peerOutput.write(string);
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    @Override
    public void setAddress(InetAddress address) {
        this.address = address;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Long getLastSeen() {
        return lastSeen;
    }

    @Override
    public void setLastSeen(Long lastSeen) {
        this.lastSeen = lastSeen;
        this.connectionAttempts = 0;
    }

    @Override
    public Long getLastPing() {
        return lastPing;
    }

    @Override
    public void setLastPing(Long lastPing) {
        this.lastPing = lastPing;
    }

    @Override
    public int getConnectionAttempts() {
        return connectionAttempts;
    }

    @Override
    public void addConnectionAttempt() {
        this.connectionAttempts++;
    }

    private void setKeepAlive() {
        try {
            this.socket.setKeepAlive(true);
        } catch (SocketException e) {
            LOGGER.error("There was a socket exception while setting KeepAlive true!", e);
        }
    }

    @Override
    @PreDestroy
    public void closePeer() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.debug("Nothing to close");
        }
    }

    @Override
    public List<Capability> getCapabilities() {
        return capabilities;
    }

    @Override
    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Peer peer = (Peer) o;
        return !isEmpty(identifier) && identifier.equals(peer.identifier) || isEmpty(identifier) && !isEmpty(address) && address.equals(peer.address) && remotePort == peer.remotePort;
    }

    @Override
    public int hashCode() {
        int result = isEmpty(identifier) ? identifier.hashCode() : 0;
        result = 31 * result + address.hashCode();
        result = 31 * result + remotePort;
        return result;
    }
}
