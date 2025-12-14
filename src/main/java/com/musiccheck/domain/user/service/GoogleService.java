package com.musiccheck.domain.user.service;

import com.musiccheck.common.jwt.JwtTokenProvider;
import com.musiccheck.common.oauth.OAuthAttributes;
import com.musiccheck.domain.user.entity.OAuthUser;
import com.musiccheck.domain.user.entity.Role;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.OAuthUserRepository;
import com.musiccheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleService {

    private final UserRepository userRepository;
    private final OAuthUserRepository oAuthUserRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    /**
     * 구글 인증 코드를 액세스 토큰으로 교환하고 사용자 정보 조회
     * CustomOAuth2UserService의 saveOrUpdate 로직 재사용
     */
    @Transactional
    public Map<String, Object> handleCallback(String code) {
        try {
            // 1. 인증 코드를 액세스 토큰으로 교환
            Map<String, String> tokens = exchangeCodeForTokens(code);
            String accessToken = tokens.get("access_token");

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("액세스 토큰이 없습니다.");
            }

            // 2. 액세스 토큰으로 사용자 정보 조회
            Map<String, Object> userInfo = getUserInfo(accessToken);

            // 3. OAuthAttributes 생성
            OAuthAttributes attributes = OAuthAttributes.of("google", "sub", userInfo);
            String providerUserId = String.valueOf(userInfo.get("sub"));

            // 4. 사용자 저장 또는 업데이트 (CustomOAuth2UserService 로직 재사용)
            User user = saveOrUpdate(attributes, "google", providerUserId);

            // 5. JWT 토큰 생성
            String jwtToken = jwtTokenProvider.createToken(user.getEmail());

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", jwtToken);
            result.put("email", user.getEmail());
            result.put("message", "구글 로그인 성공");

            return result;

        } catch (Exception e) {
            System.err.println("❌ Google Callback 오류 발생:");
            System.err.println("   오류 타입: " + e.getClass().getName());
            System.err.println("   오류 메시지: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "구글 로그인 중 오류 발생: " + e.getMessage());
            return result;
        }
    }

    /**
     * 인증 코드를 액세스 토큰으로 교환
     */
    private Map<String, String> exchangeCodeForTokens(String code) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> tokenResponse = response.getBody();
            String accessToken = (String) tokenResponse.get("access_token");

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("액세스 토큰을 받지 못했습니다.");
            }

            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", accessToken);
            return tokens;
        }

        throw new RuntimeException("토큰 교환 실패: " + response.getStatusCode());
    }

    /**
     * 액세스 토큰으로 구글 사용자 정보 가져오기
     */
    private Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        }
        throw new RuntimeException("사용자 정보 조회 실패");
    }

    /**
     * 사용자 저장 또는 업데이트 (CustomOAuth2UserService의 saveOrUpdate 로직 재사용)
     */
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
                    entity.setOAuthAttributes(name, attributes.getProfile(), Role.GUEST);
                    return entity;
                })
                .orElse(attributes.toEntity());

        User savedUser = userRepository.save(user);
        // 저장 후 OAuth attributes 설정
        savedUser.setOAuthAttributes(name, attributes.getProfile(), Role.GUEST);

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
