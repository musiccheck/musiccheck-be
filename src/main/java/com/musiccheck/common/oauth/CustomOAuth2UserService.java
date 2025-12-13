package com.musiccheck.common.oauth;

import com.musiccheck.domain.user.entity.OAuthUser;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.OAuthUserRepository;
import com.musiccheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;
    private final OAuthUserRepository oAuthUserRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // provider_user_id 추출
        String providerUserId = extractProviderUserId(registrationId, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes, registrationId, providerUserId);

        return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    private String extractProviderUserId(String registrationId, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return response != null ? String.valueOf(response.get("id")) : null;
        } else if ("kakao".equals(registrationId)) {
            return String.valueOf(attributes.get("id"));
        } else {
            // Google
            return String.valueOf(attributes.get("sub"));
        }
    }

    @Transactional
    private User saveOrUpdate(OAuthAttributes attributes, String provider, String providerUserId) {
        // name이 null이면 email의 @ 앞부분을 사용, 그것도 없으면 "사용자"
        String nameFromAttributes = attributes.getName();
        final String name;
        if (nameFromAttributes == null || nameFromAttributes.isEmpty()) {
            String email = attributes.getEmail();
            name = email != null ? email.split("@")[0] : "사용자";
        } else {
            name = nameFromAttributes;
        }

        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> {
                    entity.update(name, attributes.getProfile());
                    entity.setOAuthAttributes(name, attributes.getProfile(), com.musiccheck.domain.user.entity.Role.GUEST);
                    return entity;
                })
                .orElse(attributes.toEntity());

        User savedUser = userRepository.save(user);
        // 저장 후 OAuth attributes 설정
        savedUser.setOAuthAttributes(name, attributes.getProfile(), com.musiccheck.domain.user.entity.Role.GUEST);

        // oauth_user 테이블에 저장 또는 업데이트
        saveOrUpdateOAuthUser(savedUser.getId(), provider, providerUserId, attributes.getEmail());

        return savedUser;
    }

    private void saveOrUpdateOAuthUser(Long userId, String provider, String providerUserId, String email) {
        oAuthUserRepository.findByUserIdAndProvider(userId, provider)
                .ifPresentOrElse(
                        existingOAuthUser -> {
                            // 이미 존재하면 업데이트 (provider_user_id나 email이 변경될 수 있음)
                            // 필요시 업데이트 로직 추가
                        },
                        () -> {
                            // 존재하지 않으면 새로 생성
                            OAuthUser oAuthUser = new OAuthUser(userId, provider, providerUserId, email);
                            oAuthUserRepository.save(oAuthUser);
                        }
                );
    }
}