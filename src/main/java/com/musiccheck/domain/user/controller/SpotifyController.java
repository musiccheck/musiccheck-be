package com.musiccheck.domain.user.service;

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

    @Value("${spring.security.oauth2.client.registration.spotify.client-id}")
    private String clientId;

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    /**
     * 스포티파이 연동 시작 엔드포인트
     * 이 엔드포인트를 호출하면 스포티파이 OAuth 인증 페이지로 리다이렉트됩니다.
     */
    @GetMapping("/api/spotify/connect")
    public ResponseEntity<Map<String, String>> connectSpotify(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        try {
            // 스포티파이 OAuth 인증 URL 생성
            String redirectUri = "http://" + serverAddress + ":" + serverPort + "/api/spotify/callback";
            String scope = "user-read-private user-read-email playlist-modify-public playlist-modify-private";
            String state = URLEncoder.encode(authentication.getName(), StandardCharsets.UTF_8.toString()); // 사용자 이메일을 state로 사용하여 콜백에서 식별

            String authUrl = "https://accounts.spotify.com/authorize" +
                    "?client_id=" + clientId +
                    "&response_type=code" +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()) +
                    "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()) +
                    "&state=" + state;

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
     * 스포티파이에서 인증 후 리다이렉트되는 엔드포인트입니다.
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
