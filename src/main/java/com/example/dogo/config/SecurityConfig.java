package com.example.dogo.config;

import com.example.dogo.service.user.CustomOAuth2UserService;
import com.example.dogo.security.CustomAuthenticationFailureHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
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

                // 비로그인 사용자도 공지사항 목록 및 상세조회가 가능하도록 /notice/** 경로 허용 추가
                .requestMatchers(HttpMethod.GET, "/inquiry").permitAll() // 목록 페이지는 누구나 접근 가능
                .requestMatchers("/inquiry/**").authenticated() // 상세조회, 등록 등은 로그인 필요
                .requestMatchers("/", "/login", "/join", "/find-account", "/api/user/**", "/api/mail/**", "/api/sms/**", "/api/place/**", "/missing-alerts", "/api/missing-alerts/**", "/css/**", "/js/**", "/images/**", "/oauth2/**", "/lost-items/**", "/found-items/**", "/animal-reports/**", "/missing-persons/**", "/lost-report", "/lost-report/**", "/areas/**", "/api/areas/**", "/api/police/**", "/api/korail/**", "/api/subway/**", "/api/lost-items/*/rematch", "/api/found-items/*/rematch", "/api/lost-items/*/stream", "/api/found-items/*/stream", "/api/animal-reports/*/stream", "/faq", "/faq/**", "/notice/**", "/guide", "/error", "/uploads/**").permitAll()
                .requestMatchers("/admin/api/emergency/status").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()

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
                .userDetailsService(userDetailsService)
            );

        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureHandler(customAuthenticationFailureHandler)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
            );
        }

        return http.build();
    }
}
