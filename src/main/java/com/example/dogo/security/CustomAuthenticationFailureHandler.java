package com.example.dogo.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        String errorMessage;
        String exceptionType;

        if (exception instanceof UsernameNotFoundException) {
            errorMessage = "존재하지 않는 아이디입니다.";
            exceptionType = "userNotFound";
        } else if (exception instanceof BadCredentialsException) {
            errorMessage = "비밀번호가 일치하지 않습니다.";
            exceptionType = "badCredentials";
        } else if (exception instanceof org.springframework.security.authentication.DisabledException) {
            errorMessage = "이용 정지되었거나 탈퇴 처리된 계정입니다. 고객센터에 문의해 주세요.";
            exceptionType = "disabled";
        } else if (exception instanceof org.springframework.security.oauth2.core.OAuth2AuthenticationException) {
            errorMessage = exception.getMessage();
            exceptionType = "oauth2Error";
        } else {
            errorMessage = "알 수 없는 이유로 로그인에 실패하였습니다.";
            exceptionType = "unknown";
        }

        // URL 인코딩하여 한글 깨짐 방지
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        setDefaultFailureUrl("/login?error=true&exception=" + exceptionType + "&message=" + encodedMessage);

        super.onAuthenticationFailure(request, response, exception);
    }
}
