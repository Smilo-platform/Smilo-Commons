package io.smilo.commons.ledger;

import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertEquals;

public class AddressHelperTest {

    @Test
    public void testAddressEncoding() {
//      # Normal
//      0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed
        assertEquals("5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed", AddressHelper.formatAddress(AddressHelper.AddressType.S5, Hex.decode("aAeb6053F3E94C9b9A09f33669435E7Ef1BeAedadadadada")));
//      0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359
        assertEquals("fB6916095ca1df60bB79Ce92cE3Ea74c37c5d359", AddressHelper.formatAddress(AddressHelper.AddressType.PUBLIC_CONTRACT, Hex.decode("B6916095ca1df60bB79Ce92cE3Ea74c37c5d359adadadada")));
//# All caps
//      0x52908400098527886E0F7030069857D2E4169EE7
        assertEquals("52908400098527886E0F7030069857D2E4169EE7", AddressHelper.formatAddress(AddressHelper.AddressType.S5, Hex.decode("2908400098527886E0F7030069857D2E4169EE7a")));
//# All Lower
//      0x27b1fdb04752bbc536007a920d24acb045561c26
        assertEquals("27b1fdb04752bbc536007a920d24acb045561c26", AddressHelper.formatAddress(AddressHelper.AddressType.S2, Hex.decode("7b1fdb04752bbc536007a920d24acb045561c26a")));
    }
}
