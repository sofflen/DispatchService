package com.study.dispatchservice.utils;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class WiremockUtils {

    public static void reset() {
        WireMock.reset();
        WireMock.resetAllRequests();
        WireMock.resetAllScenarios();
        WireMock.resetToDefault();
    }

    public static void stubWiremock(String url, int httpStatusResponse, String body) {
        stubWiremock(url, httpStatusResponse, body, null, null, null);
    }

    public static void stubWiremock(String url, int httpStatusResponse, String body, String scenario, String initialState, String nextState) {
        var responseDefinitionBuilder = aResponse()
                .withStatus(httpStatusResponse)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                .withBody(body);

        if (scenario != null) {
            stubFor(get(urlEqualTo(url))
                    .inScenario(scenario)
                    .whenScenarioStateIs(initialState)
                    .willReturn(responseDefinitionBuilder)
                    .willSetStateTo(nextState));
        } else {
            stubFor(get(urlEqualTo(url))
                    .willReturn(responseDefinitionBuilder));
        }
    }
}
