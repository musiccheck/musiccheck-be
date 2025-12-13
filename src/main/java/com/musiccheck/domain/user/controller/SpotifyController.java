package com.musiccheck.domain.user.controller;

import com.musiccheck.domain.user.service.SpotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;

    // ⭐ Spring Security OAuth 경로 X — 우리가 만든 경로로 수정
    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.redirect.uri}")
    private String spotifyRedirectUri;

    /**
     * 스포티파이 연동 시작 엔드포인트
     */
    @GetMapping("/api/spotify/connect")
    public ResponseEntity<Map<String, String>> connectSpotify(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        try {
            String redirectUri = spotifyRedirectUri;
            
            // 디버깅: 실제 사용하는 redirect URI 로그 출력
            System.out.println("=== Spotify Redirect URI 디버깅 ===");
            System.out.println("설정 파일에서 읽은 URI: " + spotifyRedirectUri);
            System.out.println("인코딩 전 URI: " + redirectUri);
            System.out.println("인코딩 후 URI: " + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()));

            String scope = "user-read-private user-read-email playlist-modify-public playlist-modify-private";
            String state = URLEncoder.encode(authentication.getName(), StandardCharsets.UTF_8.toString());

            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());
            String authUrl = "https://accounts.spotify.com/authorize" +
                    "?client_id=" + clientId +
                    "&response_type=code" +
                    "&redirect_uri=" + encodedRedirectUri +
                    "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()) +
                    "&state=" + state;
            
            System.out.println("최종 authUrl: " + authUrl);
            System.out.println("===================================");

            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            response.put("message", "스포티파이 연동을 시작합니다.");

            return ResponseEntity.ok(response);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 인코딩 오류", e);
        }
    }

    /**
     * 스포티파이 OAuth 콜백 엔드포인트
     */
    @GetMapping("/api/spotify/callback")
    public ResponseEntity<Map<String, Object>> spotifyCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        if (error != null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "스포티파이 연동이 취소되었습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (code == null || state == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "필수 파라미터가 누락되었습니다.");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> result = spotifyService.handleCallback(code, state);
        return ResponseEntity.ok(result);
    }
}
