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

import io.smilo.commons.block.BlockStore;
import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.peer.IPeer;
import io.smilo.commons.pendingpool.PendingBlockDataPool;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;

public class RequestNetStateHandler implements PayloadHandler {

    private final static Logger LOGGER = Logger.getLogger(RequestNetStateHandler.class);

    private BlockStore blockStore;
    private PendingBlockDataPool pendingBlockDataPool;

    public RequestNetStateHandler(BlockStore blockStore, PendingBlockDataPool pendingBlockDataPool) {
        this.blockStore = blockStore;
        this.pendingBlockDataPool = pendingBlockDataPool;
    }

    @Override
    public void handlePeerPayload(List<String> parts, IPeer peer) {
        LOGGER.debug("Data: NETWORK_STATE, BlockchainLength: " + blockStore.getBlockchainLength() + ", LatestBlock: " + blockStore.getLastBlock().getBlockHash());
        peer.write("NETWORK_STATE " + blockStore.getBlockchainLength() + " " + blockStore.getLastBlock().getBlockHash());
        pendingBlockDataPool.getPendingData(Transaction.class).stream()
                .forEach(t -> {
                    peer.write("TRANSACTION " + t);
                });
    }

    @Override
    public PayloadType supports() {
        return PayloadType.REQUEST_NET_STATE;
    }
}
