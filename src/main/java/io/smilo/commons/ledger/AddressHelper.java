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
import org.apache.log4j.Logger;
import org.spongycastle.util.encoders.Hex;

public class AddressHelper {

    private static final Logger LOGGER = Logger.getLogger(AddressHelper.class);

    public static String formatAddress(AddressType type, byte[] shaValue) {
        StringBuffer hexAddress = new StringBuffer();
        hexAddress.append(type.getPrefix());
        hexAddress.append(Hex.toHexString(shaValue).toLowerCase());
        hexAddress.setLength(40);
        byte[] test = hexAddress.toString().toLowerCase().getBytes();
        byte[] shaOfHex = HashUtility.keccak256(hexAddress.toString().toLowerCase().getBytes());
        for (int i = 0; i < 20; i++) {
//            if ((shaOfHex[shaOfHex.length-1-i] & 0x08) > 0) hexAddress.setCharAt(2*i, Character.toUpperCase(hexAddress.charAt(2*i)));
//            if ((shaOfHex[shaOfHex.length-1-i] & 0x80) > 0) hexAddress.setCharAt(2*i+1, Character.toUpperCase(hexAddress.charAt(2*i+1)));
            if ((shaOfHex[i] & 0x08) > 0) hexAddress.setCharAt(2*i+1, Character.toUpperCase(hexAddress.charAt(2*i+1)));
            if ((shaOfHex[i] & 0x80) > 0) hexAddress.setCharAt(2*i, Character.toUpperCase(hexAddress.charAt(2*i)));
        }
        return hexAddress.toString();
    }

    public enum AddressType {
        S1('1'),
        S2('2'),
        S3('3'),
        S4('4'),
        S5('5'),
        PRIVATE_CONTRACT('e'),
        PUBLIC_CONTRACT('f'),
        RESERVED('0');

        private char prefix;
        AddressType(char prefix) {
            this.prefix = prefix;
        }

        public char getPrefix() {
            return this.prefix;
        }
    }

}

