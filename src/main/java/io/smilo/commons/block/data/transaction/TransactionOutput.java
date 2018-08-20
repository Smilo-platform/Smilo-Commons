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

import java.math.BigInteger;

public class TransactionOutput {

    private String outputAddress;
    private BigInteger outputAmount = BigInteger.ZERO;

    public TransactionOutput() {};

    public TransactionOutput(String outputAddress, BigInteger outputAmount) {
        this.outputAddress = outputAddress;
        this.outputAmount = outputAmount;
    }

    public void setOutputAddress(String outputAddress) {
        this.outputAddress = outputAddress;
    }

    public void setOutputAmount(BigInteger outputAmount) {
        this.outputAmount = outputAmount;
    }

    public String getOutputAddress() {
        return outputAddress;
    }

    public BigInteger getOutputAmount() {
        return outputAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransactionOutput output = (TransactionOutput) o;

        if (outputAddress != null ? !outputAddress.equals(output.outputAddress) : output.outputAddress != null)
            return false;
        return outputAmount != null ? outputAmount.equals(output.outputAmount) : output.outputAmount == null;
    }

    @Override
    public int hashCode() {
        int result = outputAddress != null ? outputAddress.hashCode() : 0;
        result = 31 * result + (outputAmount != null ? outputAmount.hashCode() : 0);
        return result;
    }
}
