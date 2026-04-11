/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2021 Wipro Limited. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.so.adapters.oof.exceptions.OofAdapterException;
import org.onap.so.adapters.oof.model.OofRequest;
import org.onap.so.adapters.oof.utils.OofUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class OofAdapterExceptionTest {

    @MockBean
    OofUtils oofutils;

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    OofCallbackHandler oofCallbackHandler;

    @Autowired
    OofClient oofClient;

    @BeforeEach
    void prepareMocks() throws Exception {
        Mockito.when(oofutils.getCamundaHeaders()).thenReturn(new HttpHeaders());
        Mockito.when(oofutils.getCamundaMsgUrl(anyString(), anyString())).thenReturn("http://fake/message");
        Mockito.when(oofutils.getOofHttpHeaders()).thenReturn(new HttpHeaders());
        Mockito.when(oofutils.getOofurl(anyString())).thenReturn("http://fake/oof");
    }

    @Test
    public void processCallbackThrowsOofAdapterExceptionOnRestClientException() {
        Mockito.when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RestClientException("Connection refused"));
        assertThrows(OofAdapterException.class, () -> oofCallbackHandler.processCallback(
                "NSISelectionResponse", "d88da85c-d9e8-4f73-b837-3a72a431622a", "request-body"));
    }

    @Test
    public void callOofThrowsOofAdapterExceptionOnRestClientException() throws OofAdapterException {
        OofRequest request = new OofRequest();
        request.setApiPath("/api/oof/selection/nsi/v1");
        Mockito.when(restTemplate.postForEntity(anyString(), any(), any()))
                .thenThrow(new RestClientException("Connection refused"));
        assertThrows(OofAdapterException.class, () -> oofClient.callOof(request));
    }
}
