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

import io.smilo.commons.block.data.BlockData;
import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

//        TRANSACTION timestamp;assetID;inputAddress;inputAmount;outputAddress1;outputAmount1;outputAddress2;outputAmount2;...;...;txFee;extraData;txHash;signatureData;signatureIndex
public class Transaction extends BlockData {

    private String assetId = "";
    private BigInteger inputAmount = BigInteger.ZERO;
    private List<TransactionOutput> transactionOutputs;

    public Transaction() {
        super();
        this.transactionOutputs = new ArrayList<>();
    }

    public Transaction(Long timestamp,
                       String assetId,
                       String inputAddress,
                       BigInteger inputAmount,
                       BigInteger fee,
                       List<TransactionOutput> transactionOutputs,
                       String extraData,
                       String dataHash,
                       String signatureData,
                       Long signatureIndex) {
        super(timestamp, inputAddress, fee, extraData, signatureData, signatureIndex, dataHash);
        this.assetId = assetId;
        this.inputAmount = inputAmount;
        this.transactionOutputs = transactionOutputs;
    }

    /**
     * ID of Asset (0x00000....0 for Smilo, 0x00000...1 for SmiloPay)
     * @return 
     */
    public String getAssetId() {
        return assetId;
    }

    public BigInteger getInputAmount() {
        return inputAmount;
    }

    public List<TransactionOutput> getTransactionOutputs() {
        return transactionOutputs;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public void setInputAmount(BigInteger inputAmount) {
        this.inputAmount = inputAmount;
    }

    public void setTransactionOutputs(List<TransactionOutput> transactionOutputs) {
        this.transactionOutputs = transactionOutputs;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(getRawTransaction());
    }
    
    public String getRawTransaction() {
        return getRawTransactionDataWithHash() + ";" + getSignatureData() + ";" + getSignatureIndex();
    }

    private String replaceNullByEmptyString(String input) {
        return input == null ? "" : input;
    }

    /**
     * Returns the transaction as string without signature data and index
     * @return 
     */
    public String getTransactionBody() {
        String[] tx = getRawTransaction().split(";");
        return Stream.of(tx).limit(tx.length - 2).collect(joining(";"));
    }

    /**
     * checks if the transaction has content
     * 
     * TODO: how can we improve this?
     * @return true if the content string is longer than 10 characters
     */
    public boolean hasContent() {
        return getRawTransaction().length() > 10;
    }

    /**
     * Sums the output amount total of all transactions
     * @return summation of the output amounts.
     */
    public BigInteger getOutputTotal() {
        return transactionOutputs.stream().map(TransactionOutput::getOutputAmount).reduce(BigInteger.ZERO,BigInteger::add);
    }
    
    public boolean containsAddress(String address) {
        if (getInputAddress().equals(address)) {
            return true;
        }
        
        return transactionOutputs.stream().anyMatch(txOutput -> txOutput.getOutputAddress().equals(address));
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.getTimestamp());
        hash = 97 * hash + Objects.hashCode(this.assetId);
        hash = 97 * hash + Objects.hashCode(this.getInputAddress());
        hash = 97 * hash + Objects.hashCode(this.inputAmount);
        hash = 97 * hash + Objects.hashCode(this.getFee());
        hash = 97 * hash + Objects.hashCode(this.getExtraData());
        hash = 97 * hash + Objects.hashCode(this.transactionOutputs);
        hash = 97 * hash + Objects.hashCode(this.getDataHash());
        hash = 97 * hash + Objects.hashCode(this.getSignatureData());
        hash = 97 * hash + Objects.hashCode(this.getSignatureIndex());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Transaction other = (Transaction) obj;
        if (!Objects.equals(this.assetId, other.assetId)) {
            return false;
        }
        if (!Objects.equals(this.getInputAddress(), other.getInputAddress())) {
            return false;
        }
        if (!Objects.equals(this.getExtraData(), other.getExtraData())) {
            return false;
        }
        if (!Objects.equals(this.getDataHash(), other.getDataHash())) {
            return false;
        }
        if (!Objects.equals(this.getSignatureData(), other.getSignatureData())) {
            return false;
        }
        if (!Objects.equals(this.getTimestamp(), other.getTimestamp())) {
            return false;
        }
        if (!Objects.equals(this.inputAmount, other.inputAmount)) {
            return false;
        }
        if (!Objects.equals(this.getFee(), other.getFee())) {
            return false;
        }
        if (!Objects.equals(this.transactionOutputs, other.transactionOutputs)) {
            return false;
        }
        if (!Objects.equals(this.getSignatureIndex(), other.getSignatureIndex())) {
            return false;
        }
        return true;
    }
    public String getRawTransactionDataWithHash() {
        return getRawTransactionData() + ";" + getDataHash();
    }

    public String getRawTransactionData() {
        String data = getTimestamp() + ";" + getAssetId() + ";" + getInputAddress() + ";" + getInputAmount() + ";";

        for (TransactionOutput txOutput : transactionOutputs) {
            data += ";" + txOutput.getOutputAddress() + ";" + txOutput.getOutputAmount();
        }

        return data + ";" + getFee() + ";" + getExtraData();
    }
}
