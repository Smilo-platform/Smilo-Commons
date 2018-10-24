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


import io.smilo.commons.block.Content;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

public abstract class BlockData extends Content {

    private String inputAddress = "";
    private BigInteger fee = BigInteger.ZERO;
    private Optional<String> extraData = Optional.of("");
    private Optional<String> signatureData = Optional.of("");
    private long signatureIndex;
    private Optional<String> dataHash = Optional.of("");

    protected BlockData() {}

    protected BlockData(Long timestamp, String inputAddress, BigInteger fee, String extraData, String signatureData, Long signatureIndex, String dataHash) {
        super(timestamp);
        this.inputAddress = inputAddress;
        this.fee = fee;
        this.extraData = Optional.ofNullable(extraData);
        this.signatureData = Optional.ofNullable(signatureData);
        this.signatureIndex = signatureIndex;
        this.dataHash = Optional.of(dataHash);
    }

    public String getInputAddress() {
        return inputAddress;
    }

    public BigInteger getFee() {
        return fee;
    }

    public String getSignatureData() {
        return signatureData.orElse("");
    }

    public Long getSignatureIndex() {
        return signatureIndex;
    }

    public String getExtraData() {
        return extraData.orElse("");
    }

    public String getDataHash() {
        return dataHash.orElse("");
    }

    public void setInputAddress(String inputAddress) {
        this.inputAddress = inputAddress;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }

    public void setExtraData(String extraData) {
        this.extraData = Optional.ofNullable(extraData);
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = Optional.ofNullable(signatureData);
    }

    public void setSignatureIndex(Long signatureIndex) {
        this.signatureIndex = signatureIndex;
    }

    public void setDataHash(String dataHash) {
        this.dataHash = Optional.of(dataHash);
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
        final BlockData other = (BlockData) obj;
        return Objects.equals(this.dataHash, other.dataHash);
    }

    @Override
    public int hashCode() {
        return dataHash.hashCode();
    }

}
