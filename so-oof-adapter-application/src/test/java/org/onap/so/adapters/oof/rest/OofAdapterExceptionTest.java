/*-
 * ============LICENSE_START=======================================================
 * ONAP - SO
 * ================================================================================
 * Copyright (C) 2021 Wipro Limited. All rights reserved.
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

import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
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

	@Mock
	RestTemplate mockrestTemplate;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Autowired
	OofCallbackHandler oofCallbackHandler;

	@Autowired
	OofClient oofClient;

	@Before
	void prepareMocks() throws Exception {
		Mockito.when(oofutils.getCamundaHeaders()).thenReturn(new HttpHeaders());
		Mockito.when(oofutils.getCamundaMsgUrl(Mockito.anyString(), Mockito.anyString())).thenReturn("oofurl");
		Mockito.when(oofutils.getOofHttpHeaders()).thenReturn(new HttpHeaders());
		Mockito.when(oofutils.getOofurl(Mockito.anyString())).thenReturn("oofurl");
	}

	@Test
	public void processCallbackTestException() throws Exception {
		Mockito.when(mockrestTemplate.postForEntity(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenThrow(new RestClientException("Connection refused"));
		exception.expect(OofAdapterException.class);
		try {
			oofCallbackHandler.processCallback("NSISelectionResponse", "d88da85c-d9e8-4f73-b837-3a72a431622a",
					"request");
		} catch (OofAdapterException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void callOofTestException() throws Exception {
		OofRequest request = new OofRequest();
		Mockito.when(mockrestTemplate.postForEntity(Mockito.anyString(), Mockito.any(), Mockito.any()))
				.thenThrow(new RestClientException("Connection refused"));
		exception.expect(OofAdapterException.class);
		try {
			oofClient.callOof(request);
		} catch (OofAdapterException e) {
			e.printStackTrace();
		}
	}
}

