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

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Extension of AbstractSpringTest, providing a mocked web client.
 * This client can be used for mocking and testing requests to the RPC layer.
 */
public abstract class AbstractWebSpringTest extends AbstractSpringTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    protected MockMvc webClient;

    @Before
    public void initWebClient() {
        DefaultMockMvcBuilder webClientBuilder = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .defaultRequest(get("/").contentType(APPLICATION_JSON));

        prepareWebClient(webClientBuilder);
        this.webClient = webClientBuilder.build();
    }

    protected void prepareWebClient(DefaultMockMvcBuilder webClientBuilder) {

    }

}
