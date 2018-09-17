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


import io.smilo.commons.HashHelper;
import org.apache.log4j.Logger;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.StandardCharsets;

public class AddressHelper {

    private static final Logger LOGGER = Logger.getLogger(AddressHelper.class);

    public static String formatAddress(AddressType type, byte[] shaValue) {
        StringBuffer hexAddress = new StringBuffer();
        hexAddress.append(type.getPrefix());
        hexAddress.append(Hex.toHexString(shaValue).toLowerCase());
        formatAddressCase(hexAddress);
        return hexAddress.toString();
    }

    public static boolean checkAddress(String address) {
        return address.equals(getAddressWithCase(address));
    }

    public static String getAddressWithCase(String address) {
        StringBuffer hexAddress = new StringBuffer();
        hexAddress.append(address.toLowerCase());
        formatAddressCase(hexAddress);
        return hexAddress.toString();
    }

    private static void formatAddressCase(StringBuffer hexAddress) {
        hexAddress.setLength(40);
        byte[] test = hexAddress.toString().toLowerCase().getBytes();
        byte[] shaOfHex = HashHelper.keccak256(hexAddress.toString().toLowerCase().getBytes());
        for (int i = 0; i < 20; i++) {
            if ((shaOfHex[i] & 0x08) > 0) hexAddress.setCharAt(2*i+1, Character.toUpperCase(hexAddress.charAt(2*i+1)));
            if ((shaOfHex[i] & 0x80) > 0) hexAddress.setCharAt(2*i, Character.toUpperCase(hexAddress.charAt(2*i)));
        }
    }

    public static AddressType getType(int numLayers) {
        AddressType ret;
        switch(numLayers) {
            case 14: ret = AddressType.S1;
                     break;
            case 15: ret = AddressType.S2;
                     break;
            case 16: ret = AddressType.S3;
                break;
            case 17: ret = AddressType.S4;
                break;
            case 18: ret = AddressType.S5;
                break;
            default: ret = AddressType.UNSUPPORTED;
                break;
        }
        return ret;
    }
    public enum AddressType {
        S1('1'),
        S2('2'),
        S3('3'),
        S4('4'),
        S5('5'),
        PRIVATE_CONTRACT('e'),
        PUBLIC_CONTRACT('f'),
        RESERVED('0'),
        UNSUPPORTED('d');

        private char prefix;
        AddressType(char prefix) {
            this.prefix = prefix;
        }

        public char getPrefix() {
            return this.prefix;
        }
    }

}

