package io.smilo.commons.block;

import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.StableTests;
import io.smilo.commons.block.data.transaction.TransactionParser;
import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.ledger.AddressUtility;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

@Category({StableTests.class})
public class BlockSigningTest extends AbstractSpringTest {

    @Autowired
    private AddressUtility addressUtility;
    @Autowired
    private AddressManager addressManager;
    @Autowired
    private TransactionParser transactionParser;
    @Autowired
    private BlockParser blockParser;

    @Test
    public void testIsValidWithoutTransactions() {
        String address = addressManager.getDefaultAddress();
        Block block = new Block(1527514557052L, 0, "", address, "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        blockParser.hash(block);
        blockParser.sign(block, addressManager.getAddressPrivateKey(address), 0);
        Assert.assertTrue(blockParser.isValid(block));
    }

    @Test
    public void testIsNotValidWrongMerkleSignature() {
        Block block = new Block(1527514557052L, 0, "", "myRedeemAddress", "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        String privateKey = addressManager.getDefaultPrivateKey();
        blockParser.hash(block);
        blockParser.sign(block, privateKey, (int) block.getNodeSignatureIndex());
        block.setNodeSignature("wrongsignature,0");
        block.setNodeSignatureIndex(0);
        Assert.assertFalse(blockParser.isValid(block));
    }

    @Test
    public void testIsValidBlock() {
        String address = addressManager.getDefaultAddress();
        Block block = new Block(1527514557052L, 0, "", address, "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        String privateKey = addressManager.getAddressPrivateKey(address);
        blockParser.hash(block);
        blockParser.sign(block, privateKey, (int) block.getNodeSignatureIndex());
        Assert.assertTrue(blockParser.isValid(block));
    }

    @Test
    public void testSign() {
        String address = addressManager.getDefaultAddress();
        Block block = new Block(1527514557052L, 0, "", address, "myLedgerHash", new ArrayList<>(), "nodeSignature", 0);
        blockParser.hash(block);
        String privateKey = addressManager.getAddressPrivateKey(address);
        String before = block.getRawBlockDataWithHash();
        blockParser.sign(block, privateKey, (int) block.getNodeSignatureIndex());
        String after = block.getRawBlockDataWithHash();
        Assert.assertEquals(before, after);
        Assert.assertNotNull(block.getNodeSignature());
        Assert.assertNotEquals("", block.getNodeSignature());
        Assert.assertTrue(addressUtility.verifyMerkleSignature(after, block.getNodeSignature(), block.getRedeemAddress(), block.getNodeSignatureIndex()));
    }

}
