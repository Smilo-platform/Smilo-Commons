package io.smilo.commons.peer;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

public class MockPeer implements IPeer {

    private List<String> writtenData;
    private Boolean initialized = false;
    private List<Capability> capabilities;

    private String identifier;

    private Long lastSeen;
    private Long lastPing;
    private int connectionAttempts;

    private String connectHost;
    private int connectPort;

    public MockPeer(String identifier) {
        this.identifier = identifier;
    }

    public MockPeer(String identifier, String connectHost, int connectPort)  {
        this.writtenData = new ArrayList<>();
        this.setInitialized(true);
        this.identifier = identifier;
        this.connectHost = connectHost;
        this.connectPort = connectPort;
        this.capabilities = new ArrayList<>();
    }

    @Override
    public void run() {
        setInitialized(true);
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
    public String getIdentifier() {
        return identifier;
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

    @Override
    public void closePeer() {
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
    public String getConnectHost() {
        return connectHost;
    }

    @Override
    public void setConnectHost(String connectHost) {
        this.connectHost = connectHost;
    }

    @Override
    public int getConnectPort() {
        return connectPort;
    }

    @Override
    public void setConnectPort(int connectPort) {
        this.connectPort = connectPort;
    }

    @Override
    public void write(String data) {
        this.writtenData.add(data);
    }

    @Override
    public List<String> readData() {
        List<String> receivedData = new ArrayList<>(this.writtenData);
        this.writtenData.clear();
        return receivedData;
    }

    public List<String> getWrittenData() {
        return writtenData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MockPeer peer = (MockPeer) o;
        return !isEmpty(identifier) && identifier.equals(peer.identifier) || isEmpty(identifier) && !isEmpty(connectHost) && connectHost.equals(peer.connectHost) && connectPort == peer.connectPort;
    }

    @Override
    public int hashCode() {
        int result = isEmpty(identifier) ? identifier.hashCode() : 0;
        result = 31 * result + connectHost.hashCode();
        result = 31 * result + connectPort;
        return result;
    }

}
