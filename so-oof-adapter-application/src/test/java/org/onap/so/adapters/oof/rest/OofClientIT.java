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
import org.onap.so.adapters.oof.model.OofRequest;
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
class OofClientIT {

    private static final WireMockServer oofServer =
            new WireMockServer(wireMockConfig().dynamicPort());

    @DynamicPropertySource
    static void configureOofEndpoint(DynamicPropertyRegistry registry) {
        oofServer.start();
        registry.add("mso.oof.endpoint", oofServer::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        oofServer.stop();
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void resetWireMock() {
        oofServer.resetAll();
    }

    @Test
    void callOof_forwardsRequestToOofAndReturnsOofResponse() throws IOException {
        final String oofPath = "/api/oof/selection/nsi/v1";
        final String oofResponseBody = "{\"requestId\":\"abc\",\"requestStatus\":\"completed\"}";

        oofServer.stubFor(post(urlEqualTo(oofPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(oofResponseBody)));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/so/adapters/oof/v1", buildHttpEntity(), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(oofResponseBody, response.getBody());

        oofServer.verify(postRequestedFor(urlEqualTo(oofPath))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void callOof_whenOofReturns202_returnsAccepted() throws IOException {
        final String oofPath = "/api/oof/selection/nsi/v1";

        oofServer.stubFor(post(urlEqualTo(oofPath))
                .willReturn(aResponse().withStatus(202)));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/so/adapters/oof/v1", buildHttpEntity(), String.class);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        oofServer.verify(postRequestedFor(urlEqualTo(oofPath)));
    }

    @Test
    void callOof_whenOofReturns500_adapterRespondsWithServerError() throws IOException {
        oofServer.stubFor(post(urlEqualTo("/api/oof/selection/nsi/v1"))
                .willReturn(aResponse().withStatus(500).withBody("OOF internal error")));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/so/adapters/oof/v1", buildHttpEntity(), String.class);

        assertTrue(response.getStatusCode().is5xxServerError(),
                "Expected a 5xx status but got " + response.getStatusCode());
    }

    @Test
    void callOof_whenOofIsUnreachable_adapterRespondsWithServerError() {
        OofRequest request = new OofRequest();
        request.setApiPath("/api/oof/selection/nsi/v1");
        request.setRequestDetails("{}");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OofRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/so/adapters/oof/v1", entity, String.class);

        assertTrue(!response.getStatusCode().is2xxSuccessful(),
                "Expected non-2xx status but got " + response.getStatusCode());
    }

    private HttpEntity<OofRequest> buildHttpEntity() throws IOException {
        File file = new ClassPathResource("testInputs/NsiSelectionOofRequest.json").getFile();
        OofRequest oofRequest = new ObjectMapper().readValue(file, OofRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(oofRequest, headers);
    }
}
