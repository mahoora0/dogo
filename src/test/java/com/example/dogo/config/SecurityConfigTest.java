package com.example.dogo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {

    @Test
    void resolveRememberMeKeyUsesConfiguredValue() {
        String key = SecurityConfig.resolveRememberMeKey(" configured-secret ", () -> "fallback-secret");

        assertThat(key).isEqualTo("configured-secret");
    }

    @Test
    void resolveRememberMeKeyFallsBackWithoutUsingFixedDefault() {
        String key = SecurityConfig.resolveRememberMeKey(" ", () -> "generated-secret");

        assertThat(key).isEqualTo("generated-secret");
        assertThat(key).isNotEqualTo("uniqueAndSecret");
    }

    @Test
    void parseAllowedOriginsAcceptsExplicitOrigins() {
        assertThat(WebSocketConfig.parseAllowedOrigins("https://dogo.example, http://localhost:8080"))
                .containsExactly("https://dogo.example", "http://localhost:8080");
    }

    @Test
    void parseAllowedOriginsRejectsWildcard() {
        assertThatThrownBy(() -> WebSocketConfig.parseAllowedOrigins("https://dogo.example,*"))
                .isInstanceOf(IllegalStateException.class);
    }
}
