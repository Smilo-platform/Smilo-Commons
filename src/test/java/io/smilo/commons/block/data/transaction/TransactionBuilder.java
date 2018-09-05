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

package io.smilo.commons.block.data.transaction;

import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.pendingpool.PendingBlockDataPool;
import io.smilo.commons.ledger.AccountBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Component
public class TransactionBuilder {

    @Autowired
    private PendingBlockDataPool pendingBlockDataPool;

    @Autowired
    private AccountBuilder accountBuilder;
    
    @Autowired
    private TransactionParser transactionParser;
    
    @Autowired
    private AddressManager addressManager;
    
    @Autowired
    private LedgerManager ledgerManager;

    public TransactionBuildCommand kelly_funds_robert_incorrect_hash() {
        return new TransactionBuildCommand()
                .withTimestamp(System.currentTimeMillis())
                .withInputAddress(accountBuilder.kelly().save().getAddress())
                .withAssetId("000x00123")
                .withFee(BigInteger.ZERO)
                .withInputAmount(BigInteger.ONE)
                .addTransactionOutput(accountBuilder.robert().save().getAddress(), BigInteger.ONE)
                .withDataHash("invalidhash")
                .withSignatureIndex(0L);
    }

    public TransactionBuildCommand elkan_shares_wealth() {
        return new TransactionBuildCommand()
                .withTimestamp(System.currentTimeMillis())
                .withInputAddress(accountBuilder.elkan().save().getAddress())
                .withAssetId("000x00123")
                .withFee(BigInteger.ZERO)
                .withInputAmount(BigInteger.valueOf(200L))
                .addTransactionOutput(accountBuilder.kelly().save().getAddress(), BigInteger.valueOf(100L))
                .addTransactionOutput(accountBuilder.robert().save().getAddress(), BigInteger.valueOf(100L))
                .hashTransactionData()
                .signTransaction();
    }

    public TransactionBuildCommand robert_shares_wealth_with_kelly() {
        return new TransactionBuildCommand()
                .withTimestamp(System.currentTimeMillis())
                .withInputAddress(AccountBuilder.ROBERT)
                .withAssetId("000x00123")
                .withFee(BigInteger.ZERO)
                .withInputAmount(BigInteger.valueOf(50L))
                .addTransactionOutput(AccountBuilder.KELLY, BigInteger.valueOf(50L))
                .hashTransactionData()
                .signTransaction();
    }

    public TransactionBuildCommand empty() {
        return new TransactionBuildCommand();
    }

    public class TransactionBuildCommand {

        private final Transaction transaction;

        public TransactionBuildCommand() {
            this.transaction = new Transaction();
        }

        public TransactionBuildCommand withTimestamp(Long timestamp) {
            this.transaction.setTimestamp(timestamp);
            return this;
        }

        public TransactionBuildCommand withInputAddress(String inputAddress) {
            this.transaction.setInputAddress(inputAddress);
            return this;
        }

        public TransactionBuildCommand withFee(BigInteger fee) {
            this.transaction.setFee(fee);
            return this;
        }

        public TransactionBuildCommand withSignatureData(String signatureData) {
            this.transaction.setSignatureData(signatureData);
            return this;
        }

        public TransactionBuildCommand withSignatureIndex(Long signatureIndex) {
            this.transaction.setSignatureIndex(signatureIndex);
            return this;
        }

        public TransactionBuildCommand withDataHash(String dataHash) {
            this.transaction.setDataHash(dataHash);
            return this;
        }

        public TransactionBuildCommand withAssetId(String assetId) {
            this.transaction.setAssetId(assetId);
            return this;
        }

        public TransactionBuildCommand withInputAmount(BigInteger inputAmount) {
            this.transaction.setInputAmount(inputAmount);
            return this;
        }

        public TransactionBuildCommand withTransactionOutputs(List<TransactionOutput> transactionOutputs) {
            this.transaction.setTransactionOutputs(transactionOutputs);
            return this;
        }

        public TransactionBuildCommand addTransactionOutput(String address, BigInteger amount) {
            TransactionOutput output = new TransactionOutput(address, amount);
            this.transaction.getTransactionOutputs().add(output);
            return this;
        }
        
        public TransactionBuildCommand hashTransactionData() {
            transactionParser.hash(transaction);
            return this;
        }
        
        public TransactionBuildCommand signTransaction() {
            //String defaultAddress = addressManager.getDefaultAddress();
            int signatureIndex = ledgerManager.getAddressSignatureCount(transaction.getInputAddress());
            String defaultPrivateKey = addressManager.getAddressPrivateKey(transaction.getInputAddress());
            transactionParser.sign(transaction, defaultPrivateKey, signatureIndex+1);
            return this;
        }

        public Transaction construct() {
            return transaction;
        }

        public Transaction queue() {
            pendingBlockDataPool.addBlockData(transaction);
            return transaction;
        }
    }

}
