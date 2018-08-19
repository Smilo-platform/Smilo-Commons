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

package io.smilo.commons.pendingpool;

import java.math.BigInteger;

/**
 * I'm not a huge fan of Java's built-in options for pairing stuff together. So I wrote this. This class is primarily used by PendingTransactionContainer.
 */
public class StringBigIntegerPair {

    private String stringToHold;
    private BigInteger bigIntegerToHold;

    /**
     * A StringBigIntegerPair is... well... a pair containing a String and a BigInteger.
     *
     * @param stringToHold String to hold
     * @param bigIntegerToHold BigInteger to hold
     */
    public StringBigIntegerPair(String stringToHold, BigInteger bigIntegerToHold) {
        this.stringToHold = stringToHold;
        this.bigIntegerToHold = bigIntegerToHold;
    }

    public String getStringToHold(){
        return stringToHold;
    }

    public BigInteger getBigIntegerToHold(){
        return bigIntegerToHold;
    }

    public void addBigIntegerToHold(BigInteger bigIntegerToHold) {
        this.bigIntegerToHold = this.bigIntegerToHold.add(bigIntegerToHold);
    }
}
