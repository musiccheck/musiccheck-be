package com.musiccheck.domain.user.service;

import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.spotify.client-secret}")
    private String clientSecret;

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 스포티파이 인증 코드를 액세스 토큰으로 교환
     */
    @Transactional
    public Map<String, Object> handleCallback(String code, String state) {
        try {
            // 1. 인증 코드를 액세스 토큰으로 교환
            String accessToken = exchangeCodeForToken(code);

            // 2. 액세스 토큰으로 사용자 정보 가져오기
            Map<String, Object> userInfo = getUserInfo(accessToken);

            // 3. 사용자 찾기 및 스포티파이 연동 상태 업데이트
            User user = userRepository.findByEmail(state)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            user.setSpotifyConnected(true);
            userRepository.save(user);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "스포티파이 연동이 완료되었습니다.");
            result.put("spotifyUserId", userInfo.get("id"));

            return result;
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "스포티파이 연동 중 오류가 발생했습니다: " + e.getMessage());
            return result;
        }
    }

    /**
     * 인증 코드를 액세스 토큰으로 교환
     */
    private String exchangeCodeForToken(String code) {
        String tokenUrl = "https://accounts.spotify.com/api/token";
        String redirectUri = "http://" + serverAddress + ":" + serverPort + "/api/spotify/callback";

        RestTemplate restTemplate = new RestTemplate();

        // Basic Authentication 헤더 생성
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("토큰 교환 실패");
        }
    }

    /**
     * 액세스 토큰으로 스포티파이 사용자 정보 가져오기
     */
    private Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = "https://api.spotify.com/v1/me";

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
        } else {
            throw new RuntimeException("사용자 정보 조회 실패");
        }
    }
}