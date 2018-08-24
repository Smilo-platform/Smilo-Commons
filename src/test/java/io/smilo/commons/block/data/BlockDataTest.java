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

package io.smilo.commons.block.data;


import io.smilo.commons.HashUtility;
import io.smilo.commons.block.data.transaction.Transaction;
import io.smilo.commons.block.data.transaction.TransactionOutput;
import io.smilo.commons.block.data.transaction.TransactionParser;
import io.smilo.commons.ledger.MerkleTreeGenerator;
import io.smilo.commons.pendingpool.PendingBlockDataPool;
import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.StableTests;
import io.smilo.commons.block.genesis.GenesisLoader;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@Category({StableTests.class})
public class BlockDataTest extends AbstractSpringTest {

    @Autowired
    private PendingBlockDataPool pendingBlockDataPool;

    @Autowired
    private MerkleTreeGenerator treeGenerator;

    @Autowired
    private TransactionParser transactionParser;

    @Autowired
    private GenesisLoader genesisLoader;


    @Test
    public void testVerifyParsedTransactionsAreMarkedAsDuplicates() {
        genesisLoader.loadGenesis();
        treeGenerator.generateMerkleTree("BOdbzfoE9Za9a4cGQTpExBYw7mQNFo2B", 14, 16, 128);
        int pendingBlockDataPoolSizeBefore = pendingBlockDataPool.allBroadcastBlockData.size();
        TransactionOutput txOutput = new TransactionOutput();
        txOutput.setOutputAmount(BigInteger.valueOf(10L));
        txOutput.setOutputAddress("S1CGDE3KP4QTKFDS4METM5FWDWDIATTRDQJ5IK");

        Transaction transaction1 = new Transaction();
        transaction1.setTimestamp(1530705940694L);
        transaction1.setAssetId("dummy");
        transaction1.setInputAmount(BigInteger.valueOf(10L));
        transaction1.setInputAddress("S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR");
        transaction1.setFee(BigInteger.ZERO);
        transaction1.setTransactionOutputs(Collections.singletonList(txOutput));
        transactionParser.hash(transaction1);
        transactionParser.sign(transaction1, "BOdbzfoE9Za9a4cGQTpExBYw7mQNFo2B", 0);

        Transaction transaction2 = new Transaction();
        transaction2.setTimestamp(1530705940694L);
        transaction2.setAssetId("dummy");
        transaction2.setInputAmount(BigInteger.valueOf(10L));
        transaction2.setInputAddress("S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR");
        transaction2.setFee(BigInteger.ZERO);
        transaction2.setTransactionOutputs(Collections.singletonList(txOutput));
        transactionParser.hash(transaction2);
        transactionParser.sign(transaction2, "BOdbzfoE9Za9a4cGQTpExBYw7mQNFo2B", 0);

        AddBlockDataResult result1 = pendingBlockDataPool.addBlockDataToPool(transaction1);
        AddBlockDataResult result2 = pendingBlockDataPool.addBlockDataToPool(transaction2);

        String rawTransaction = HashUtility.encodeToBase64(transactionParser.serialize(transaction1));
        pendingBlockDataPool.addTransaction(rawTransaction);
        pendingBlockDataPool.addTransaction(rawTransaction);

        assertEquals(result1.getType().name(),"ADDED");
        assertEquals(result2.getType().name(),"DUPLICATE");

        assertEquals(pendingBlockDataPool.allBroadcastBlockData.size(),pendingBlockDataPoolSizeBefore + 1);
    }

}
