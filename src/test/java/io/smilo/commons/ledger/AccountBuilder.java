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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
public class AccountBuilder {

    // Public address of Elkan, with private key "BOdbzfoE9Za9a4cGQTpExBYw7mQNFo2B"
    public static final String ELKAN = "S1RQ3ZVRQ2K42FTXDONQVFVX73Q37JHIDCSFAR";

    // Public address of Kelly, with private key "Kelly"
    public static final String KELLY = "S1HY2JKBM44VJWCBZROTMOM5B3BYZWD7FVDVFS";

    // Public address of Robert, with private key "Robert"
    public static final String ROBERT = "S17LXTYN7HC4VHVTYZDENSTZOAD47HKV5FBEHR";

    @Autowired
    private LedgerStore ledgerStore;
    
    public AccountBuildCommand blank() {
        return new AccountBuildCommand();
    }
    
    public AccountBuildCommand elkan() {
        return new AccountBuildCommand()
                .withAddress(ELKAN)
                .withBalance(BigInteger.valueOf(9999L))
                .withSignatureCount(0);
    }
    
    public AccountBuildCommand robert() {
        return new AccountBuildCommand()
                .withAddress(ROBERT)
                .withBalance(BigInteger.valueOf(10L))
                .withSignatureCount(0);
    }
    
    public AccountBuildCommand kelly() {
        return new AccountBuildCommand()
                .withAddress(KELLY)
                .withBalance(BigInteger.valueOf(1L))
                .withSignatureCount(0);
    }
    
    public class AccountBuildCommand {
        
        private final Account account;
        
        public AccountBuildCommand() {
            this.account = new Account();
        }
        
        public AccountBuildCommand withAddress(String address) {
            account.setAddress(address);
            return this;
        }
        
        public AccountBuildCommand withBalance(BigInteger balance) {
            account.setBalance(balance);
            return this;
        }
        
        public AccountBuildCommand withSignatureCount(int signatureCount) {
            account.setSignatureCount(signatureCount);
            return this;
        }
        
        public Account save() {
            ledgerStore.writeToDB(account);
            return account;
        }
        
        public Account construct() {
            return account;
        }
        
    }
}
