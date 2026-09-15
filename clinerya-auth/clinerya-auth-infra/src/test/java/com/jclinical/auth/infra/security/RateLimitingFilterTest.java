package com.jclinical.auth.infra.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Regresion del bypass del rate limit: la implementacion anterior tomaba el PRIMER valor
 * de X-Forwarded-For, que lo escribe el cliente. Bastaba con mandar una IP inventada
 * distinta en cada intento para estrenar bucket y anular el limite de login.
 */
class RateLimitingFilterTest {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String REAL_CLIENT = "203.0.113.7";

    private MockHttpServletRequest loginRequest(String forwardedFor, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN);
        request.setRequestURI(LOGIN);
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void takesTheHopAppendedByTheTrustedProxyNotTheClientSuppliedOne() {
        RateLimitingFilter filter = new RateLimitingFilter(1);

        // nginx usa $proxy_add_x_forwarded_for: conserva lo que mando el cliente y anade el peer real.
        String ip = filter.clientIp(loginRequest("1.1.1.1, 2.2.2.2, " + REAL_CLIENT, "10.0.0.1"));

        assertEquals(REAL_CLIENT, ip);
    }

    @Test
    void takesTheSecondToLastHopWithTwoProxiesInFront() {
        RateLimitingFilter filter = new RateLimitingFilter(2);

        String ip = filter.clientIp(loginRequest("1.1.1.1, " + REAL_CLIENT + ", 172.16.0.9", "10.0.0.1"));

        assertEquals(REAL_CLIENT, ip);
    }

    @Test
    void ignoresTheHeaderEntirelyWhenThereIsNoProxyInFront() {
        RateLimitingFilter filter = new RateLimitingFilter(0);

        String ip = filter.clientIp(loginRequest("1.1.1.1", "10.0.0.1"));

        assertEquals("10.0.0.1", ip);
    }

    @Test
    void fallsBackToTheDirectPeerWhenThereAreFewerHopsThanExpected() {
        RateLimitingFilter filter = new RateLimitingFilter(2);

        // Solo un salto: alguien llego sin pasar por los dos proxies esperados.
        String ip = filter.clientIp(loginRequest("1.1.1.1", "10.0.0.1"));

        assertEquals("10.0.0.1", ip);
    }

    @Test
    void spoofedLeadingEntriesDoNotEarnAFreshBucket() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(1);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse lastResponse = null;

        // El limite de login es 10/min. Un atacante varia la parte que controla en cada intento.
        for (int attempt = 0; attempt < 11; attempt++) {
            MockHttpServletRequest request =
                    loginRequest("10.9.9." + attempt + ", " + REAL_CLIENT, "10.0.0.1");
            lastResponse = new MockHttpServletResponse();
            filter.doFilter(request, lastResponse, chain);
        }

        assertEquals(429, lastResponse.getStatus());
    }

    @Test
    void letsDistinctRealClientsThroughIndependently() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(1);
        FilterChain chain = mock(FilterChain.class);

        for (int attempt = 0; attempt < 11; attempt++) {
            filter.doFilter(loginRequest(REAL_CLIENT, "10.0.0.1"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse otherClient = new MockHttpServletResponse();
        filter.doFilter(loginRequest("198.51.100.4", "10.0.0.1"), otherClient, chain);

        assertEquals(200, otherClient.getStatus());
    }

    @Test
    void doesNotRateLimitPathsOutsideTheList() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(1);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/clinics");
        request.setRequestURI("/api/v1/clinics");
        request.setRemoteAddr(REAL_CLIENT);

        for (int attempt = 0; attempt < 50; attempt++) {
            response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
        }

        assertEquals(200, response.getStatus());
    }
}
