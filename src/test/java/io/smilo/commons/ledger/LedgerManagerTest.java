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
import io.smilo.commons.block.data.transaction.TransactionBuilder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigInteger;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category({StableTests.class})
public class LedgerManagerTest extends AbstractSpringTest {
    
    @Autowired
    private LedgerManager ledgerManager;

    @Autowired
    private AccountBuilder accountBuilder;
    
    @Autowired
    private TransactionBuilder transactionBuilder;

    @Autowired
    private AddressManager addressManager;
    
    @Test
    public void testGetLedgerHash() {
        accountBuilder.elkan().save();
        accountBuilder.kelly().save();
        assertEquals("D099D9E7A871F645D9C3FB342F1FF84EABDC334AA67AD34F98EF27F2D1BB4241", ledgerManager.getLedgerHash());
    }
    
    @Test
    public void testExecuteTransaction() {
        Account elkan = accountBuilder.elkan().save();
        Account kelly = accountBuilder.kelly().save();
        Account robert = accountBuilder.robert().save();

        assertEquals(BigInteger.valueOf(9999L), ledgerManager.getAddressBalance(elkan.getAddress()));
        assertEquals(BigInteger.ONE, ledgerManager.getAddressBalance(kelly.getAddress()));
        assertEquals(BigInteger.valueOf(10L), ledgerManager.getAddressBalance(robert.getAddress()));

        
        Transaction transaction = transactionBuilder.elkan_shares_wealth().construct();
        assertTrue(ledgerManager.executeTransaction(transaction));

        assertEquals(BigInteger.valueOf(9799L), ledgerManager.getAddressBalance(elkan.getAddress()));
        assertEquals(BigInteger.valueOf(101L), ledgerManager.getAddressBalance(kelly.getAddress()));
        assertEquals(BigInteger.valueOf(110L), ledgerManager.getAddressBalance(robert.getAddress()));

        addressManager.importPrivateKey("Robert");
        Transaction secondTransaction = transactionBuilder.robert_shares_wealth_with_kelly().construct();
        assertTrue(ledgerManager.executeTransaction(secondTransaction));

        assertEquals(BigInteger.valueOf(9799L), ledgerManager.getAddressBalance(elkan.getAddress()));
        assertEquals(BigInteger.valueOf(151L), ledgerManager.getAddressBalance(kelly.getAddress()));
        assertEquals(BigInteger.valueOf(60L), ledgerManager.getAddressBalance(robert.getAddress()));
    }
    
    @Test
    public void testExecuteTransactionEmpty() {
        Transaction transaction = transactionBuilder.empty().construct();
        assertFalse(ledgerManager.executeTransaction(transaction));
    }
    
    @Test
    public void testExecuteTransactionInvalid() {
        Transaction transaction = transactionBuilder.kelly_funds_robert_incorrect_hash().construct();
        assertFalse(ledgerManager.executeTransaction(transaction));
    }
    
    @Test
    public void testReverseTransaction() {
        // TODO: we need a valid transaction
    }
    
    @Test
    public void testReverseTransactionInvalid() {
        Transaction transaction = transactionBuilder.kelly_funds_robert_incorrect_hash().construct();
        assertFalse(ledgerManager.reverseTransaction(transaction));
    }
    
    @Test
    public void testGetAddressSignatureCount() {
        Account elkan = accountBuilder.elkan().withSignatureCount(2).save();

        assertEquals(2, ledgerManager.getAddressSignatureCount(elkan.getAddress()));
    }
    
    @Test
    public void testGetAddressSignatureCountNotFound() {
        assertEquals(-1, ledgerManager.getAddressSignatureCount("asijdaosijdasoijd"));
    }
    
    @Test
    public void testAdjustAddressSignatureCount() {
        Account elkan = accountBuilder.elkan().withSignatureCount(2).save();
        ledgerManager.adjustAddressSignatureCount(elkan.getAddress(), 2);
        assertEquals(4, ledgerManager.getAddressSignatureCount(elkan.getAddress()));
    }
    
    @Test
    public void testAdjustAddressSignatureCountNegativeSignatureCount() {
        Account elkan = accountBuilder.elkan().withSignatureCount(2).save();
        ledgerManager.adjustAddressSignatureCount(elkan.getAddress(), -3);
        assertEquals(elkan.getSignatureCount(), ledgerManager.getAddressSignatureCount(elkan.getAddress()));
    }
    
    @Test
    public void testGetAddressBalance() {
        Account elkan = accountBuilder.elkan().save();
        assertEquals(BigInteger.valueOf(9999L), ledgerManager.getAddressBalance(elkan.getAddress()));
    }
    
    @Test
    public void testAdjustAddressBalance() {
        Account elkan = accountBuilder.elkan().save();

        ledgerManager.adjustAddressBalance(elkan.getAddress(), BigInteger.valueOf(10L));

        assertEquals(BigInteger.valueOf(10009L), ledgerManager.getAddressBalance(elkan.getAddress()));
    }
    
    @Test
    public void testAdjustAddressBalanceNegativeBalance() {
        Account kelly = accountBuilder.kelly().save();

        ledgerManager.adjustAddressBalance(kelly.getAddress(), BigInteger.valueOf(-10L));

        assertEquals(BigInteger.ONE, ledgerManager.getAddressBalance(kelly.getAddress()));
    }
    
}
