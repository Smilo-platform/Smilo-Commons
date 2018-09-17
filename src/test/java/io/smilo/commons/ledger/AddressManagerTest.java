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

package io.smilo.commons.ledger;

import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.StableTests;
import io.smilo.commons.block.data.transaction.Transaction;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category({StableTests.class})
public class AddressManagerTest extends AbstractSpringTest {

    @Autowired
    private AddressManager addressManager;
    
    @Autowired
    private PrivateKeyGenerator privateKeyGenerator;
    
    @Autowired
    private MerkleTreeGenerator treeGen;
    
    @Mocked
    private PrivateKeyGenerator keyGenerator;

    @Value("${WALLET_FILE:wallet.keys}")
    private String walletFileName;

    
    @Before
    public void setup() {
        ReflectionTestUtils.setField(addressManager, "privateKeyGenerator", privateKeyGenerator);
    }
    
    @Test
    public void testRegenerateDatabaseFiles() throws IOException {
        super.cleanUpFiles();
        privateKeyGenerator = new PrivateKeyGenerator() {
            @Override
            public String getPrivateKey() {
                return "supersecretkey";
            }
        };
        ReflectionTestUtils.setField(addressManager, "privateKeyGenerator", privateKeyGenerator);
        addressManager.readOrRegenerateDatabase(walletFileName);
        String privateKey = privateKeyGenerator.getPrivateKey();
        assertContainsString(walletFileName, treeGen.generateMerkleTree(privateKey, 14, 16, 128) + ":" + privateKey);
        // TODO: test if addresses directory is correctly generated
    }
    
    @Test
    public void testLoadExistingDatabaseFiles() {
        cleanUpFiles();
        // manually create wallet file
        File file = new File(walletFileName);
        try (PrintWriter out = new PrintWriter(file)) {
            out.println("address:privatekey");
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        addressManager.readOrRegenerateDatabase(walletFileName);
        assertEquals("address", addressManager.getDefaultAddress());
        assertEquals("privatekey", addressManager.getDefaultPrivateKey());
    }
    
    @Test
    public void testGetSignedTransaction() {
        List<AbstractMap.SimpleEntry<String, String>> privateKeys = (List<AbstractMap.SimpleEntry<String, String>>) ReflectionTestUtils.getField(addressManager, "privateKeys");
        privateKeys.add(new AbstractMap.SimpleEntry<>(AccountBuilder.SECRET,"supersecretkey"));
        Transaction signedTransaction = addressManager.getSignedTransaction("destination", BigInteger.valueOf(100L), addressManager.getDefaultAddressIndexOffset());
        // TODO: verify if the signed transaction is correct
    }
    
    @Test
    public void testGetNewAddress() {
        new Expectations() {{ 
            keyGenerator.getPrivateKey();
            result = "supersecretkey";
        }};
        
        String newAddress = addressManager.getNewAddress();
        // Always the same outcome because private keys are always the same in the test environment
        assertEquals(AccountBuilder.SECRET, newAddress);

        // Should probably be retrieved in a different way as soon as the list of addresses and private keys is actually used
        List<AbstractMap.SimpleEntry<String, String>> privateKeys = (List<AbstractMap.SimpleEntry<String, String>>) ReflectionTestUtils.getField(addressManager, "privateKeys");

        assertTrue(privateKeys.stream().filter(p -> p.getKey() != null).anyMatch(p -> { return p.getValue().equals("supersecretkey") && p.getKey().equals(AccountBuilder.SECRET); }));
    }
    
    @Test
    public void testIndexOffset() {
        addressManager.resetDefaultAddressIndexOffset();
        assertEquals(Long.valueOf(1), addressManager.getDefaultAddressIndexOffset());
        addressManager.incrementDefaultAddressIndexOffset();
        assertEquals(Long.valueOf(2), addressManager.getDefaultAddressIndexOffset());
    }

    private void assertContainsString(String filename, String match) {
        try {
            List<String> lines = Files.lines(Paths.get(filename)).collect(toList());
            assertTrue(lines.stream().anyMatch(line -> line.contains(match)));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
