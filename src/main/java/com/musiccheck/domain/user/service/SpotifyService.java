package com.musiccheck.domain.user.service;

import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SpotifyService {

    private final UserRepository userRepository;

    // ✅ 새 경로로 수정
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    @Value("${spotify.redirect.uri}")
    private String spotifyRedirectUri;

    /**
     * 스포티파이 인증 코드를 액세스 토큰으로 교환
     */
    @Transactional
    public Map<String, Object> handleCallback(String code, String state) {
        try {
            // state 디코딩 (URL 인코딩된 이메일)
            String decodedEmail = URLDecoder.decode(state, StandardCharsets.UTF_8.toString());
            
            System.out.println("=== Spotify Callback 디버깅 ===");
            System.out.println("원본 state: " + state);
            System.out.println("디코딩된 이메일: " + decodedEmail);
            
            Map<String, String> tokens = exchangeCodeForTokens(code);
            String accessToken = tokens.get("access_token");
            String refreshToken = tokens.get("refresh_token");
            
            Map<String, Object> userInfo = getUserInfo(accessToken);

            System.out.println("사용자 찾기 시도: " + decodedEmail);
            User user = userRepository.findByEmail(decodedEmail)
                    .orElseThrow(() -> {
                        System.err.println("❌ 사용자를 찾을 수 없습니다. 이메일: " + decodedEmail);
                        return new IllegalArgumentException("사용자를 찾을 수 없습니다. 이메일: " + decodedEmail);
                    });

            System.out.println("✅ 사용자 찾음: " + user.getEmail());
            System.out.println("현재 spotify_connected 값: " + user.getSpotifyConnected());
            
            // 토큰 저장
            user.setSpotifyConnected(true);
            user.setSpotifyAccessToken(accessToken);
            user.setSpotifyRefreshToken(refreshToken);
            userRepository.save(user);
            
            // 저장 후 확인
            User savedUser = userRepository.findByEmail(decodedEmail).orElse(null);
            System.out.println("저장 후 spotify_connected 값: " + (savedUser != null ? savedUser.getSpotifyConnected() : "null"));
            System.out.println("===================================");

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "스포티파이 연동이 완료되었습니다.");
            result.put("spotifyUserId", userInfo.get("id"));

            return result;

        } catch (Exception e) {
            System.err.println("❌ Spotify Callback 오류 발생:");
            System.err.println("   오류 타입: " + e.getClass().getName());
            System.err.println("   오류 메시지: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "스포티파이 연동 중 오류 발생: " + e.getMessage());
            return result;
        }
    }

    /**
     * 인증 코드를 액세스 토큰과 리프레시 토큰으로 교환
     */
    private Map<String, String> exchangeCodeForTokens(String code) {
        String tokenUrl = "https://accounts.spotify.com/api/token";
        String redirectUri = spotifyRedirectUri;

        RestTemplate restTemplate = new RestTemplate();

        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials =
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedCredentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> tokenResponse = response.getBody();
            String accessToken = (String) tokenResponse.get("access_token");
            String refreshToken = (String) tokenResponse.get("refresh_token");
            
            Map<String, String> tokens = new HashMap<>();
            tokens.put("access_token", accessToken);
            tokens.put("refresh_token", refreshToken);
            return tokens;
        }
        throw new RuntimeException("토큰 교환 실패");
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
        }
        throw new RuntimeException("사용자 정보 조회 실패");
    }

    /**
     * 스포티파이 플레이리스트 생성
     */
    @Transactional
    public String createPlaylist(String email, String playlistName, List<String> trackUris) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!"yes".equals(user.getSpotifyConnected())) {
            throw new IllegalStateException("스포티파이가 연동되지 않았습니다.");
        }

        String accessToken = user.getSpotifyAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IllegalStateException("스포티파이 액세스 토큰이 없습니다.");
        }

        // 1. 사용자 정보 가져오기 (플레이리스트 생성에 필요한 user_id)
        Map<String, Object> userInfo = getUserInfo(accessToken);
        String userId = (String) userInfo.get("id");

        // 2. 플레이리스트 생성
        String createPlaylistUrl = "https://api.spotify.com/v1/users/" + userId + "/playlists";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> playlistData = new HashMap<>();
        playlistData.put("name", playlistName);
        playlistData.put("description", "Music Check에서 추천한 음악");
        playlistData.put("public", true);

        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(playlistData, headers);
        ResponseEntity<Map> createResponse = restTemplate.postForEntity(
                createPlaylistUrl, createRequest, Map.class);

        if (createResponse.getStatusCode() != HttpStatus.CREATED || createResponse.getBody() == null) {
            throw new RuntimeException("플레이리스트 생성 실패");
        }

        Map<String, Object> playlist = createResponse.getBody();
        String playlistId = (String) playlist.get("id");
        String playlistUrl = (String) playlist.get("external_urls");

        // 3. 트랙 추가
        if (trackUris != null && !trackUris.isEmpty()) {
            String addTracksUrl = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

            Map<String, Object> tracksData = new HashMap<>();
            tracksData.put("uris", trackUris);

            HttpEntity<Map<String, Object>> addRequest = new HttpEntity<>(tracksData, headers);
            ResponseEntity<Map> addResponse = restTemplate.postForEntity(
                    addTracksUrl, addRequest, Map.class);

            if (addResponse.getStatusCode() != HttpStatus.CREATED) {
                System.err.println("⚠️ 트랙 추가 실패 (플레이리스트는 생성됨)");
            }
        }

        // 플레이리스트 URL 반환
        if (playlistUrl != null) {
            Map<String, String> externalUrls = (Map<String, String>) playlist.get("external_urls");
            if (externalUrls != null) {
                return externalUrls.get("spotify");
            }
        }

        // URL이 없으면 기본 URL 생성
        return "https://open.spotify.com/playlist/" + playlistId;
    }
}
