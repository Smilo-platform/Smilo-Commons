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

import java.math.BigInteger;
import java.util.List;

/**
 * Object used for serialization to the database and the network, ensuring a stable interface to the outside world
 */
public class MessageDTO {

    private Long timestamp;
    private String inputAddress;
    private String content;
    private List<String> outputAddresses;
    private BigInteger fee;
    private String dataHash;
    private String signatureData;
    private Long signatureIndex;

    public MessageDTO() {
        // Placing a comment makes SonarQube happy.. And I love happy computers..
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getInputAddress() {
        return inputAddress;
    }

    public void setInputAddress(String inputAddress) {
        this.inputAddress = inputAddress;
    }

    public BigInteger getFee() {
        return fee;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public Long getSignatureIndex() {
        return signatureIndex;
    }

    public void setSignatureIndex(Long signatureIndex) {
        this.signatureIndex = signatureIndex;
    }

    public String getDataHash() {
        return dataHash;
    }

    public void setDataHash(String dataHash) {
        this.dataHash = dataHash;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getOutputAddresses() {
        return outputAddresses;
    }

    public void setOutputAddresses(List<String> outputAddresses) {
        this.outputAddresses = outputAddresses;
    }

    public static MessageDTO toDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setTimestamp(message.getTimestamp());
        dto.setInputAddress(message.getInputAddress());
        dto.setFee(message.getFee());
        dto.setSignatureData(message.getSignatureData());
        dto.setSignatureIndex(message.getSignatureIndex());
        dto.setDataHash(message.getDataHash());
        dto.setContent(message.getContent());
        dto.setOutputAddresses(message.getOutputAddresses());
        return dto;
    }

    public static Message toMessage(MessageDTO messageDTO) {
        Message message = new Message();
        message.setTimestamp(messageDTO.getTimestamp());
        message.setInputAddress(messageDTO.getInputAddress());
        message.setFee(messageDTO.getFee());
        message.setSignatureData(messageDTO.getSignatureData());
        message.setSignatureIndex(messageDTO.getSignatureIndex());
        message.setDataHash(messageDTO.getDataHash());
        message.setContent(messageDTO.getContent());
        message.setOutputAddresses(messageDTO.getOutputAddresses());
        return message;
    }
}
