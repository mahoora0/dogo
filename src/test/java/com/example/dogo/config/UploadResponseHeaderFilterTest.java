package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UploadResponseHeaderFilterTest {

    @Test
    void addsDefensiveHeadersToUploadedResources() throws Exception {
        UploadResponseHeaderFilter filter = new UploadResponseHeaderFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/inquiries/legacy.svg");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("default-src 'none'; sandbox", response.getHeader("Content-Security-Policy"));
    }
}
