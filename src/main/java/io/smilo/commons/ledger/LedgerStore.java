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

import io.smilo.commons.db.Store;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

@Component
public class LedgerStore {

    private AccountParser accountParser;

    private static final Logger LOGGER = Logger.getLogger(LedgerStore.class);

    private static final String COLLECTION_NAME = "account";
    private final Store store;

    public LedgerStore(Store store) {
        this.store = store;
        store.initializeCollection(COLLECTION_NAME);
        this.accountParser = new AccountParser();
    }

    Collection<Account> getAccounts() {
        Collection<Account> ret = new LinkedList<>();
        for(Map.Entry<byte[], byte[]> entry : store.getAll(COLLECTION_NAME).entrySet()){
            ret.add(accountParser.deserialize(entry.getValue()));
        }
        return ret;
    }

    public void clearAccounts() {
        store.clear(COLLECTION_NAME);
    }

    /*
     * Looks up an account in the ledger, if account exists return it otherwise create
     * new account with 0 balance.
     */
    public Account findOrCreate(String address) {
        return getByAddress(address).orElseGet(() -> {
            Account account = new Account(address, BigInteger.ZERO, -1);
            writeToDB(account);
            return account;
        });
    }

    /**
     * Writes ledger to DB.
     * @param account account to save
     */
    public void writeToDB(Account account) {
        store.put(COLLECTION_NAME, account.getAddress().getBytes(StandardCharsets.UTF_8), accountParser.serialize(account));
    }

    public Optional<Account> getByAddress(String address) {
        Optional<Account> ret = Optional.empty();
        if(address != null) {
            try {
                byte[] acc = store.get(COLLECTION_NAME, address.getBytes(StandardCharsets.UTF_8));
                ret = Optional.of(accountParser.deserialize(acc));
            } catch (NullPointerException | IndexOutOfBoundsException ex) {
                LOGGER.debug("ADDRESS "+ address +" UNKNOWN BALANCE $0 ADDED.");
            }
        }
        return ret;
    }

    public void remove(String address) {
        store.remove(COLLECTION_NAME, address.getBytes(StandardCharsets.UTF_8));
    }
}
