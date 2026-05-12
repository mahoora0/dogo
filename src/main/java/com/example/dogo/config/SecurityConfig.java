package com.example.dogo.config;

import com.example.dogo.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 로컬 테스트를 위해 CSRF 비활성화 (필요시 활성화)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/join", "/css/**", "/js/**", "/images/**", "/oauth2/**", "/lost-items/**", "/areas/**", "/error", "/uploads/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/loginProcess")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository))
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );
        }

        return http.build();
    }

    private OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(customizer -> {
            customizer.attributes(attributes -> {
                String registrationId = (String) attributes.get(OAuth2ParameterNames.REGISTRATION_ID);
                if ("google".equals(registrationId)) {
                    customizer.additionalParameters(params -> params.put("prompt", "select_account"));
                } else if ("kakao".equals(registrationId)) {
                    customizer.additionalParameters(params -> params.put("prompt", "login"));
                } else if ("naver".equals(registrationId)) {
                    customizer.additionalParameters(params -> params.put("auth_type", "reauthenticate"));
                }
            });
        });

        return resolver;
    }
}
