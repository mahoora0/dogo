package com.example.dogo.service;

import com.example.dogo.entity.User;
import com.example.dogo.entity.UserSocialAccount;
import com.example.dogo.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    public void unlink(User user) {
        Optional<UserSocialAccount> socialAccountOpt = userSocialAccountRepository.findAll().stream()
                .filter(sa -> sa.getUser().getUserNo().equals(user.getUserNo()))
                .findFirst();

        if (socialAccountOpt.isEmpty()) return;

        UserSocialAccount socialAccount = socialAccountOpt.get();
        String provider = socialAccount.getProvider();
        
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(provider, user.getEmail());
        if (client == null) return;

        String accessToken = client.getAccessToken().getTokenValue();

        if ("kakao".equals(provider)) {
            unlinkKakao(accessToken);
        } else if ("naver".equals(provider)) {
            unlinkNaver(accessToken, provider);
        }
    }

    private void unlinkKakao(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange("https://kapi.kakao.com/v1/user/unlink", HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            System.err.println("Kakao unlink failed: " + e.getMessage());
        }
    }

    private void unlinkNaver(String accessToken, String provider) {
        ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(provider);
        String clientId = registration.getClientId();
        String clientSecret = registration.getClientSecret();

        RestTemplate restTemplate = new RestTemplate();
        String url = String.format("https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=%s&client_secret=%s&access_token=%s&service_provider=NAVER",
                clientId, clientSecret, accessToken);
        
        try {
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            System.err.println("Naver unlink failed: " + e.getMessage());
        }
    }
}
