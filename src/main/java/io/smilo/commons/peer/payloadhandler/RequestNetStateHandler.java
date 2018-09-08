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

@Component
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
//        long blockNum = blockStore.getLastBlock() != null ? blockStore.getLastBlock().getBlockNum() : 0;
        long blockNum = blockStore.getBlockchainLength();
        String blockhash = blockStore.getLastBlock() != null ? blockStore.getLastBlock().getBlockHash() : "";

        LOGGER.debug("Data: NETWORK_STATE, BlockchainLength: " + blockNum + ", BlockHash: "
                + blockhash);
        peer.write("NETWORK_STATE " + blockNum + " " + blockhash );

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
