/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2026 Deutsche Telekom AG.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.so.adapters.oof.rest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OofCallbackHandlerIT {

    private static final String MSO_KEY = "07a7159d3bf51a0e53be7a8f89699be7";

    private static final WireMockServer camundaServer =
            new WireMockServer(wireMockConfig().dynamicPort());

    @DynamicPropertySource
    static void configureCamundaEndpoint(DynamicPropertyRegistry registry) {
        camundaServer.start();
        registry.add("mso.camundaURL", camundaServer::baseUrl);
        registry.add("mso.camundaAuth", () -> "testuser:testpass");
        registry.add("mso.msoKey", () -> MSO_KEY);
        registry.add("mso.workflow.message.endpoint",
                () -> camundaServer.baseUrl() + "/mso/WorkflowMessage");
    }

    @AfterAll
    static void stopWireMock() {
        camundaServer.stop();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void resetWireMock() {
        camundaServer.resetAll();
    }

    @Test
    void processCallback_forwardsBodyToCamundaAndReturnsResponse() throws IOException {
        final String messageEventName = "NSISelectionResponse";
        final String correlator = "d88da85c-d9e8-4f73-b837-3a72a431622a";
        final String camundaPath =
                "/mso/WorkflowMessage/" + messageEventName + "/" + correlator;
        final String camundaResponse = "{\"status\":\"received\"}";

        camundaServer.stubFor(post(urlEqualTo(camundaPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(camundaResponse)));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/so/adapters/oof/callback/v1/" + messageEventName + "/" + correlator,
                buildCallbackEntity(), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(camundaResponse, response.getBody());

        camundaServer.verify(postRequestedFor(urlEqualTo(camundaPath))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void processCallback_usesMessageEventNameAndCorrelatorInCamundaUrl() throws IOException {
        final String messageEventName = "NSSIAllocationResponse";
        final String correlator = "test-correlator-42";
        final String expectedPath =
                "/mso/WorkflowMessage/" + messageEventName + "/" + correlator;

        camundaServer.stubFor(post(urlEqualTo(expectedPath))
                .willReturn(aResponse().withStatus(200)));

        restTemplate.postForEntity(
                "/so/adapters/oof/callback/v1/" + messageEventName + "/" + correlator,
                buildCallbackEntity(), String.class);

        camundaServer.verify(1, postRequestedFor(urlEqualTo(expectedPath)));
    }

    @Test
    void processCallback_whenCamundaReturns500_adapterRespondsWithServerError()
            throws IOException {
        final String messageEventName = "NSISelectionResponse";
        final String correlator = "d88da85c-d9e8-4f73-b837-3a72a431622a";

        camundaServer.stubFor(post(urlEqualTo(
                "/mso/WorkflowMessage/" + messageEventName + "/" + correlator))
                .willReturn(aResponse().withStatus(500).withBody("Camunda error")));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/so/adapters/oof/callback/v1/" + messageEventName + "/" + correlator,
                buildCallbackEntity(), String.class);

        assertTrue(response.getStatusCode().is5xxServerError(),
                "Expected 5xx but got " + response.getStatusCode());
    }

    private HttpEntity<Object> buildCallbackEntity() throws IOException {
        File file = new ClassPathResource("testInputs/NsiSelectionResponse.json").getFile();
        Object body = new ObjectMapper().readValue(file, Object.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
