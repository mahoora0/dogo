package com.example.dogo.service;

import com.example.dogo.entity.User;
import com.example.dogo.entity.UserSocialAccount;
import com.example.dogo.repository.UserRepository;
import com.example.dogo.repository.UserSocialAccountRepository;
import com.example.dogo.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerUserId = null;
        String email = null;
        String nickname = null;
        String profileImageUrl = null;

        if ("google".equals(registrationId)) {
            providerUserId = (String) attributes.get("sub");
            email = (String) attributes.get("email");
            nickname = (String) attributes.get("name");
            profileImageUrl = (String) attributes.get("picture");
        } else if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            providerUserId = (String) response.get("id");
            email = (String) response.get("email");
            nickname = (String) response.get("name");
            profileImageUrl = (String) response.get("profile_image");
        } else if ("kakao".equals(registrationId)) {
            providerUserId = String.valueOf(attributes.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            
            email = (String) kakaoAccount.get("email");
            if (profile != null) {
                nickname = (String) profile.get("nickname");
                profileImageUrl = (String) profile.get("profile_image_url");
            }
        }

        // 소셜 계정 조회
        Optional<UserSocialAccount> socialAccountOpt = userSocialAccountRepository.findByProviderAndProviderUserId(registrationId, providerUserId);

        User user;
        if (socialAccountOpt.isPresent()) {
            // 이미 가입된 경우 정보 갱신
            UserSocialAccount socialAccount = socialAccountOpt.get();
            socialAccount.updateProfile(nickname, profileImageUrl);
            user = socialAccount.getUser();
            
            // 기존 가입된 유저의 프로필 이미지가 null인 경우 (또는 소셜 프로필로 덮어쓰고 싶은 경우)
            if (profileImageUrl != null) {
                user.updateProfileImage(profileImageUrl);
            }
        } else {
            // 신규 가입
            // 이메일로 기존 일반 유저가 있는지 확인
            User finalUser = null;
            if (email != null) {
                Optional<User> existingUserOpt = userRepository.findByEmail(email);
                if (existingUserOpt.isPresent()) {
                    finalUser = existingUserOpt.get();
                }
            }

            if (finalUser == null) {
                // 완전히 새로운 유저 (loginId, password, phone은 null)
                finalUser = new com.example.dogo.entity.User(null, null, email, nickname, null, profileImageUrl);
                userRepository.save(finalUser);
            }
            user = finalUser;

            // 소셜 계정 정보 저장
            UserSocialAccount newSocialAccount = new UserSocialAccount(
                    user, registrationId, providerUserId, email, nickname, profileImageUrl
            );
            userSocialAccountRepository.save(newSocialAccount);
        }

        return new CustomUserDetails(user, attributes);
    }
}
