/*
 * Copyright (c) 2018 Smilo Platform B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smilo.commons.block.data.message;

import io.smilo.commons.ledger.AddressManager;
import io.smilo.commons.ledger.LedgerManager;
import io.smilo.commons.pendingpool.PendingBlockDataPool;
import io.smilo.commons.ledger.AccountBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;

@Component
public class MessageBuilder {

    @Autowired
    private PendingBlockDataPool pendingBlockDataPool;

    @Autowired
    private AccountBuilder accountBuilder;

    @Autowired
    private AddressManager addressManager;

    @Autowired
    private MessageParser messageParser;

    @Autowired
    private LedgerManager ledgerManager;

    public MessageBuildCommand kelly_says_hi_to_robert_incorrect_hash() {
        return new MessageBuildCommand()
                .withTimestamp(System.currentTimeMillis())
                .withInputAddress(accountBuilder.kelly().save().getAddress())
                .withContent("Hi!")
                .withFee(BigInteger.ZERO)
                .addOutputAddress(accountBuilder.robert().save().getAddress())
                .withDataHash("invalidhash")
//                .withSignatureData(addressUtility.getMerkleSignature(message, privateKey, 0, address))
                .withSignatureIndex(0L);
    }

    public MessageBuildCommand elkan_greets_team() {
        // TODO: correct hash
        return new MessageBuildCommand()
                .withTimestamp(System.currentTimeMillis())
                .withInputAddress(accountBuilder.elkan().save().getAddress())
                .withContent("Hello!")
                .withFee(BigInteger.ZERO)
                .addOutputAddress(accountBuilder.robert().save().getAddress())
                .addOutputAddress(accountBuilder.kelly().save().getAddress())
                .hashMessageData()
                .signMessage();
    }

    public MessageBuildCommand empty() {
        return new MessageBuildCommand();
    }

    public class MessageBuildCommand {

        private final Message message;

        public MessageBuildCommand() {
            this.message = new Message();
        }

        public MessageBuildCommand withTimestamp(Long timestamp) {
            this.message.setTimestamp(timestamp);
            return this;
        }

        public MessageBuildCommand withInputAddress(String inputAddress) {
            this.message.setInputAddress(inputAddress);
            return this;
        }

        public MessageBuildCommand withContent(String content) {
            this.message.setContent(content);
            return this;
        }

        public MessageBuildCommand withFee(BigInteger fee) {
            this.message.setFee(fee);
            return this;
        }

        public MessageBuildCommand withDataHash(String dataHash) {
            this.message.setDataHash(dataHash);
            return this;
        }


        public MessageBuildCommand withSignatureData(String signatureData) {
            this.message.setSignatureData(signatureData);
            return this;
        }

        public MessageBuildCommand withSignatureIndex(Long signatureIndex) {
            this.message.setSignatureIndex(signatureIndex);
            return this;
        }

        public MessageBuildCommand addOutputAddress(String address) {
            this.message.getOutputAddresses().add(address);
            return this;
        }

        public MessageBuildCommand hashMessageData() {
            messageParser.hash(message);
            return this;
        }

        public MessageBuildCommand signMessage() {
            String defaultAddress = addressManager.getDefaultAddress();
            int signatureIndex = ledgerManager.getAddressSignatureCount(defaultAddress);
            String defaultPrivateKey = addressManager.getDefaultPrivateKey();

            //messageParser.signMessage(message, defaultPrivateKey, signatureIndex, message.getInputAddress());
            return this;
        }

        public Message construct() {
            return message;
        }

        public Message queue() {
            pendingBlockDataPool.addBlockData(message);
            return message;
        }
    }
}
