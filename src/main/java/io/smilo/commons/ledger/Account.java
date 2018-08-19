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

import java.math.BigInteger;

public class Account implements Comparable<Account> {

    private String address;
    private BigInteger balance;
    private int signatureCount;
    private byte[] code = new byte[0];
    private byte[] codeHash = new byte[0];
    private State state = new State();

    public Account() {
    }

    public Account(String address, BigInteger balance, int signatureCount) {
        this.address = address;
        this.balance = balance;
        this.signatureCount = signatureCount;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getAddress() {
        return address;
    }
    
    public BigInteger getBalance() {
        return balance;
    }

    public void incrementBalance(BigInteger increment) {
        this.balance = this.balance.add(increment);
    }
    
    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public int getSignatureCount() {
        return signatureCount;
    }

    public void setSignatureCount(int signatureCount) {
        this.signatureCount = signatureCount;
    }

    public byte[] getCode() {
        return code;
    }

    public void setCode(byte[] data) {
        code = data;
    }

    public byte[] getCodeHash() {
        return codeHash;
    }

    public void codeHash() {
        this.codeHash = HashUtility.digestSHA256(this.code);
    }

    public State getState() {
        return state;
    }

    public void setState(State data) {
        state = data;
    }

    @Override
    public int compareTo(Account o) {
        if (o != null) {
            return this.address.compareTo(o.address);
        } else {
            return 1;
        }
    }
}
