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

package io.smilo.commons.block.data.message;


import io.smilo.commons.block.data.BlockData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Message extends BlockData {

    private String content;
    private List<String> outputAddresses;

    public Message() {
        this.outputAddresses = new ArrayList<>();
    }

    public Message(Long timestamp,
                   String inputAddress,
                   List<String> outputAddresses,
                   String content,
                   BigInteger fee,
                   String dataHash,
                   String signatureData,
                   Long signatureIndex) {
        super(timestamp, inputAddress, fee, signatureData, signatureIndex, dataHash);
        this.outputAddresses = outputAddresses;
        this.content = content;
    }

    public String getHashableData() {
        String data = getTimestamp() + ":" + getInputAddress() + ":" + getFee() + ":" + getContent();

        for (String outputAddress : outputAddresses) {
            data += ":" + outputAddress;
        }
        return data;
    }

    public String getContent() {
        return content;
    }

    public List<String> getOutputAddresses() {
        return outputAddresses;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setOutputAddresses(List<String> outputAddresses) {
        this.outputAddresses = outputAddresses;
    }
}
