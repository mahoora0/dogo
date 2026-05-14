package com.example.dogo.config;

import com.example.dogo.service.user.CustomOAuth2UserService;
import com.example.dogo.service.CustomUserDetailsService;
import com.example.dogo.security.CustomAuthenticationFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final UserDetailsService userDetailsService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false); // 아이디가 없을 때 UsernameNotFoundException을 던지도록 함
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // 로컬 테스트를 위해 CSRF 비활성화 (필요시 활성화)
            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/", "/login", "/join", "/api/user/**", "/api/mail/**", "/api/sms/**", "/css/**", "/js/**", "/images/**", "/oauth2/**", "/lost-items/**", "/found-items/**", "/areas/**", "/api/police/**", "/api/korail/**", "/faq", "/inquiry/**", "/notice", "/guide", "/error", "/uploads/**")
                .permitAll().anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/loginProcess")
                .usernameParameter("loginId")
                .failureHandler(customAuthenticationFailureHandler)
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("uniqueAndSecret")
                .tokenValiditySeconds(86400 * 30) // 30일 유지
                .userDetailsService(clientRegistrationRepositoryProvider.getIfAvailable() == null ? null : null) // Will be auto-injected if bean exists
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
                    customizer.additionalParameters(params -> params.put("prompt", "select_account consent"));
                } else if ("kakao".equals(registrationId)) {
                    customizer.additionalParameters(params -> params.put("prompt", "login"));
                } else if ("naver".equals(registrationId)) {
                    customizer.additionalParameters(params -> params.put("auth_type", "reprompt"));
                }
            });
        });

        return resolver;
    }
}
