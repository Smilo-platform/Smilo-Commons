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

import io.smilo.commons.peer.IPeer;
import io.smilo.commons.peer.sport.INetworkState;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NetworkStateHandler implements PayloadHandler {

    private final INetworkState networkState;
    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(NetworkStateHandler.class);


    public NetworkStateHandler(INetworkState networkState) {
        this.networkState = networkState;
    }

    @Override
    public void handlePeerPayload(List<String> parts, IPeer peer) {
        Integer topBlock = Integer.parseInt(parts.get(1));
        if (networkState.getTopBlock() < topBlock) {
            LOGGER.debug("Going to update topBlock, topBlock from network is: " + topBlock + ", Actual topBlock is: " + networkState.getTopBlock());
            networkState.setTopBlock(topBlock);
        }else {
            LOGGER.debug("topBlock that we got from the network is lower than our own topBlock ... "+ topBlock + ", Actual topBlock is: " + networkState.getTopBlock());
        }
    }

    @Override
    public PayloadType supports() {
        return PayloadType.NETWORK_STATE;
    }
}
