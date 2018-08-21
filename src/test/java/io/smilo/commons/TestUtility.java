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

package io.smilo.commons;

import io.smilo.commons.block.BlockStore;
import io.smilo.commons.peer.PeerClient;
import io.smilo.commons.peer.PeerStore;
import io.smilo.commons.peer.network.NetworkConsensus;
import io.smilo.commons.peer.network.link.NetworkLinker;
import io.smilo.commons.peer.sport.NetworkState;
import io.smilo.commons.block.genesis.GenesisLoader;
import io.smilo.commons.db.Store;
import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.ledger.LedgerStore;
import io.smilo.commons.ledger.PrivateKeyGenerator;
import io.smilo.commons.peer.sport.SportState;
import io.smilo.commons.pendingpool.PendingBlockDataPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.*;

@Component
public class TestUtility {

    @Value("${WALLET_FILE:wallet_test.keys}")
    private String walletFileName;

    @Value("${DB_FOLDER:database_test}")
    private String databaseDirName;

    @Autowired
    private AddressManager addressManager;

    @Autowired
    private LedgerStore ledgerStore;

    @Autowired
    private LedgerManager ledgerManager;

    @Autowired
    private GenesisLoader genesis;

    @Autowired
    private SportState sportState;

    @Autowired
    private PendingBlockDataPool pendingBlockDataPool;

    @Autowired
    private BlockStore blockStore;

    @Autowired
    private PeerStore peerStore;

    @Autowired
    private PeerClient peerClient;

    @Autowired
    private Store store;

    @Autowired
    private NetworkState networkState;

    @Autowired
    private NetworkLinker networkLinker;

    @Autowired
    private NetworkConsensus networkConsensus;

    private String address;
    private String privateKey;

    /**
     * Initialize the environment. Some classes need to be reset to their original state after every test.
     */
    public void initialize() {

        cleanUp();
        createFolders();

        ReflectionTestUtils.setField(addressManager, "privateKeyGenerator", new  PrivateKeyGenerator() {
            @Override
            public String getPrivateKey() {
                return "supersecretkey";
            }
        });

        ReflectionTestUtils.setField(blockStore, "chains", new ArrayList<>());


        // Retrieve the initially generated private keys
        List<AbstractMap.SimpleEntry<String,String>> privateKeys = (List<AbstractMap.SimpleEntry<String,String>>) ReflectionTestUtils.getField(addressManager, "privateKeys");
        // Save the initial address and privateKey to be reused for all tests
        if (address == null) {
            address = privateKeys.get(0).getKey();
            privateKey = privateKeys.get(0).getValue();
        }

        // Save the generated address and key in the addressManager without generating a new one using the merkle tree
        // This is a huge performance boost for the unit test suite
        privateKeys.clear();
        privateKeys.add(new AbstractMap.SimpleEntry<>(address, privateKey));
        privateKeys.add(new AbstractMap.SimpleEntry<>("S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR","BOdbzfoE9Za9a4cGQTpExBYw7mQNFo2B"));

        genesis.loadGenesis();

        sportState.initialize(addressManager.getDefaultAddress());
    }

    /**
     * Clean up the mess we made while trying to break the application with our tests
     */
    public void cleanUp() {
        deleteDir(new File(walletFileName));
        deleteDir(new File(databaseDirName));
        pendingBlockDataPool.getPendingBlockData().clear();
        ReflectionTestUtils.setField(blockStore, "chains", new ArrayList<>());
        peerStore.clear();
        sportState.reset();
        ledgerManager.getPendingTransactions().clear();
        ledgerStore.clearAccounts();
        ReflectionTestUtils.setField(peerClient, "pendingPeers", new HashSet<>());
        store.clear("block");
        store.initializeCollection("block");
        store.initializeCollection("account");
        ReflectionTestUtils.setField(networkState, "networks", new HashSet<>());
        ReflectionTestUtils.setField(networkLinker, "networks", new ArrayList<>());
        ReflectionTestUtils.setField(networkConsensus, "approvedNodes", new HashMap<>());
    }

    private void createFolders() {
        new File(walletFileName);
        new File(databaseDirName).mkdirs();
    }

    private void deleteDir(File file) {
        if (file.exists()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    deleteDir(f);
                }
            }
            file.delete();
        }
    }

}
