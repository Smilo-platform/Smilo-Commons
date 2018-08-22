package io.smilo.commons.peer.sport;

import io.smilo.commons.peer.network.Network;

import java.util.Optional;
import java.util.Set;


public interface INetworkState {
    void updateCatchupMode();

    boolean getCatchupMode();

    long getTopBlock();

    void setTopBlock(int topBlock);

    Set<Network> getNetworks();


    Optional<Network> getNetworkByIdentifier(String networkIdentifier);

    void addNetwork(Network network);

    void removeNetwork(Network network);

}
