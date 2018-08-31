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

package io.smilo.commons;

import org.junit.After;
import org.junit.Before;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * Abstract spring test for running integrated tests
 * - Starts with profile LiveTests ( mvn test -P LiveTests)
 * - Uses profile "test", using application-test.properties
 * - Cleans up the states of classes and database before and after every test
 * - The main loop will in Smilo.java will not run to prevent tests from stalling
 * - Has mocked peers, initialized in MockedPeerInitializer, providing testable p2p requests/responses
 */
@SpringBootTest()
@ActiveProfiles({"test", "disableMainLoop"})
@Category(StableTests.class)
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractSpringTest {

    @Autowired
    private TestUtility testUtility;

    @Before
    public void regenerateFiles() {
        testUtility.initialize();
    }

    @After
    public void cleanUpFiles() {
        testUtility.cleanUp();
    }
}
