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

import io.smilo.commons.AbstractSpringTest;
import io.smilo.commons.StableTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({StableTests.class})
public class AddressUtilityTest extends AbstractSpringTest {

    @Autowired
    private AddressUtility addressUtility;
    
    @Test
    public void testValidateValidS1Address() {
        assertTrue(addressUtility.isAddressFormattedCorrectly(AccountBuilder.ELKAN));
    }

    @Test
    public void testValidateValidS2Address() {
        assertTrue(addressUtility.isAddressFormattedCorrectly(AccountBuilder.TEST_S2));
    }

    @Test
    public void testValidateValidS3Address() {
        assertTrue(addressUtility.isAddressFormattedCorrectly(AccountBuilder.TEST_S3));
    }

    @Test
    public void testValidateValidS4Address() {
        assertTrue(addressUtility.isAddressFormattedCorrectly(AccountBuilder.TEST_S4));
    }

    @Test
    public void testValidateValidS5Address() {
        assertTrue(addressUtility.isAddressFormattedCorrectly(AccountBuilder.TEST_S5));
    }

    @Test
    public void testValidateValidPublicContractAddress() {
        assertTrue(addressUtility.isAddressFormattedCorrectly(AccountBuilder.PUBLIC_CONTRACT));
    }

    @Test
    public void testValidateInvalidS1Address() {
        assertFalse(addressUtility.isAddressFormattedCorrectly(AccountBuilder.ELKAN.toUpperCase()));
    }

    @Test
    public void testValidateInvalidPrefixAddress() {
        assertFalse(addressUtility.isAddressFormattedCorrectly("9"+ AccountBuilder.ELKAN.substring(1)));
    }

    @Test
    public void testValidateInvalidCharactersetAddress() {
        assertFalse(addressUtility.isAddressFormattedCorrectly(AccountBuilder.ELKAN.substring(0,19)+"G"));
    }

}
