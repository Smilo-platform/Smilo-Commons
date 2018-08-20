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

import io.smilo.commons.HashUtility;
import io.smilo.commons.block.data.transaction.Transaction;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.spongycastle.util.encoders.Hex;

import static java.util.stream.Collectors.joining;

@Component
public class LedgerManager {

    private static final Logger LOGGER = Logger.getLogger(LedgerManager.class);
    private final AddressUtility addressUtility;
    private final LedgerStore ledgerStore;
    private final AccountParser accountParser;

    private Set<Transaction> pendingTransactions = new HashSet<>();

    public LedgerManager(AddressUtility addressUtility, LedgerStore ledgerStore, AccountParser accountParser) {
        this.addressUtility = addressUtility;
        this.ledgerStore = ledgerStore;
        this.accountParser = accountParser;
    }

    /**
     * Hashes the entire ledger, to compare against blocks.
     *
     * @return HEX SHA256 hash of the ledger
     */
    public String getLedgerHash() {
        String fulllLedgerAsString = ledgerStore.getAccounts().stream().sorted(Comparator.naturalOrder()).map(t -> Hex.toHexString(accountParser.serialize(t))).collect(joining("\n"));
        return HashUtility.digestSHA256ToHEX(fulllLedgerAsString);
    }

    /**
     * This method executes a given transaction String of the format InputAddress;InputAmount;OutputAddress1;OutputAmount1;OutputAddress2;OutputAmount2...;SignatureData;SignatureIndex
     *
     * @param transaction transaction to execute
     * @return boolean Whether execution of the transaction was successful
     */
    // TODO: throw exception instead of boolean for validation
    // TODO: validations might not be required here if we isValid during parsing
    public boolean executeTransaction(Transaction transaction) {
        if (!transaction.hasContent()) {
            return false;
        }

        try {
            boolean valid = checkValidity(transaction);
            if (!valid) {
                return false;
            }

            //Looks like everything is correct--transaction should be executed correctly
            Account inputAccount = ledgerStore.findOrCreate(transaction.getInputAddress());
            inputAccount.incrementBalance(transaction.getInputAmount().negate());
            inputAccount.setSignatureCount(inputAccount.getSignatureCount()+1);

            transaction.getTransactionOutputs().forEach(txOutput -> {
                Account outputAccount = ledgerStore.findOrCreate(txOutput.getOutputAddress());
                outputAccount.incrementBalance(txOutput.getOutputAmount());

                // Quick fix, update the output addresses
                ledgerStore.writeToDB(outputAccount);
            });
            ledgerStore.writeToDB(inputAccount);
            return true;
        } catch (Exception e) {
            LOGGER.warn("Exception when executeTransaction", e);
            return false;
        }
    }

    /**
     * This method reverse-executes a given transaction String of the format InputAddress;InputAmount;OutputAddress1;OutputAmount1;OutputAddress2;OutputAmount2...;SignatureData;SignatureIndex Used
     * primarily when a blockchain fork is resolved, and transactions have to be reversed that existed in the now-forked block(s).
     *
     * @param transaction String-formatted transaction to execute
     * @return boolean Whether execution of the transaction was successful
     */
    public boolean reverseTransaction(Transaction transaction) {
        try {
            boolean valid = checkValidity(transaction);
            if (!valid) {
                return false;
            }
            //Looks like everything is correct--transaction should be reversed correctly
            Account inputAccount = ledgerStore.findOrCreate(transaction.getInputAddress());
            inputAccount.incrementBalance(transaction.getInputAmount());

            transaction.getTransactionOutputs().forEach(txOutput -> {
                Account outputAccount = ledgerStore.findOrCreate(txOutput.getOutputAddress());
                outputAccount.incrementBalance(txOutput.getOutputAmount().negate());
            });
            adjustAddressSignatureCount(transaction.getInputAddress(), -1);
            return true;
        } catch (Exception e) {
            LOGGER.debug("Failed to reverseTransaction", e);
            return false;
        }
    }

    /**
     * Returns the last-used signature index of an address.
     *
     * @param address Account to retrieve the latest index for
     * @return int Last signature index used by address
     */
    public int getAddressSignatureCount(String address) {
        return ledgerStore.getByAddress(address).map(Account::getSignatureCount).orElse(-1);
    }

    /**
     * Adjusts an address's signature count.
     *
     * @param address    Account to adjust
     * @param adjustment Amount to adjust address's signature count by. This can be negative.
     * @return boolean Whether the adjustment was successful
     */
    public boolean adjustAddressSignatureCount(String address, int adjustment) {
        int oldCount = getAddressSignatureCount(address);
        if (oldCount + adjustment < 0) //Adjustment is negative with an absolute value larger than oldBalance
        {
            return false;
        }
        return updateAddressSignatureCount(address, oldCount + adjustment);
    }

    /**
     * Updates an address's signature count.
     *
     * @param address  Account to update
     * @param newCount New signature index to use
     * @return boolean Whether the adjustment was successful
     */
    private boolean updateAddressSignatureCount(String address, int newCount) {
        try {
            Account account = ledgerStore.findOrCreate(address);
            account.setSignatureCount(newCount);
            ledgerStore.writeToDB(account);
        } catch (Exception e) {
            LOGGER.error("Unable to run updateAddressSignatureCount", e);
            return false;
        }
        return true;
    }

    /**
     * Returns the address balance for a given address.
     *
     * @param address Account to check balance of
     * @return long Balance of address
     */
    public BigInteger getAddressBalance(String address) {
        return ledgerStore.getByAddress(address).map(Account::getBalance).orElse(BigInteger.ZERO);
    }

    /**
     * Adjusts the balance of an address by a given adjustment, which can be positive or negative.
     *
     * @param address    Account to adjust the balance of
     * @param adjustment Amount to adjust account balance by
     * @return boolean Whether the adjustment was successful
     */
    public boolean adjustAddressBalance(String address, BigInteger adjustment) {
        BigInteger oldBalance = getAddressBalance(address);
        if (oldBalance.add(adjustment).compareTo(BigInteger.ZERO) < 0) //Adjustment is negative with an absolute value larger than oldBalance
        {
            return false;
        }
        return updateAddressBalance(address, oldBalance.add(adjustment));
    }

    /**
     * Updates the balance of an address to a new amount
     *
     * @param address   Account to set the balance of
     * @param newAmount New amount to set as the balance of address
     * @return boolean Whether setting the new balance was successful
     */
    public boolean updateAddressBalance(String address, BigInteger newAmount) {
        try {
            Account account = ledgerStore.findOrCreate(address);
            account.setBalance(newAmount);
            ledgerStore.writeToDB(account);
        } catch (Exception e) {
            LOGGER.error("Exception when updateAddressBalance", e);
            return false;
        }
        return true;
    }

    private boolean checkValidity(Transaction transaction) {
        String transactionMessage = transaction.getRawTransactionDataWithHash();
        String inputAddress = transaction.getInputAddress();
        String signatureData = transaction.getSignatureData();
        long signatureIndex = transaction.getSignatureIndex();
        if (transactionMessage == null || inputAddress == null || signatureData == null) {
            return false;
        }
        if (!addressUtility.verifyMerkleSignature(transactionMessage, signatureData, inputAddress, signatureIndex)) {
            return false; //Signature does not sign transaction message!
        }

//        // Todo: We can not send from secondary addresses since everything uses the default SignatureIndex! Should be added when correct!
//        if (getAddressSignatureCount(inputAddress) + 1 != signatureIndex) {
//            return false; //The signature is valid, however it isn't using the expected signatureIndex. Blocked to ensure a compromised Lamport key from a previous transaction can't be used.
//        }
        if (!addressUtility.isAddressFormattedCorrectly(inputAddress)) {
            return false; //Incorrect sending address
        }
        BigInteger inputAmount = transaction.getInputAmount();
        if (getAddressBalance(inputAddress).compareTo(inputAmount) < 0) //inputAddress has an insufficient balance
        {
            return false; //Insufficient balance
        }

        boolean addressesAreValid = transaction.getTransactionOutputs().stream()
                .allMatch(txOutput -> addressUtility.isAddressFormattedCorrectly(txOutput.getOutputAddress()));
        if (!addressesAreValid) {
            return false;
        }

        BigInteger outputTotal = transaction.getOutputTotal();
        if (inputAmount.compareTo(outputTotal) < 0) {
            return false;
        }
        return true;
    }

    public void addPendingTransaction(Transaction transaction) {
        pendingTransactions.add(transaction);
    }

    public Set<Transaction> getPendingTransactions() {
        return pendingTransactions;
    }

    public void removePendingTransaction(Transaction transaction) {
        pendingTransactions.remove(transaction);
    }

}