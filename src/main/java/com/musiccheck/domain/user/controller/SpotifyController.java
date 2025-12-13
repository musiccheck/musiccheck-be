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
    public ResponseEntity<String> spotifyCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {

        if (error != null) {
            String errorHtml = generateErrorHtml("스포티파이 연동이 취소되었습니다.");
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(errorHtml);
        }

        if (code == null || state == null) {
            String errorHtml = generateErrorHtml("필수 파라미터가 누락되었습니다.");
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(errorHtml);
        }

        Map<String, Object> result = spotifyService.handleCallback(code, state);
        
        // 성공 시 HTML 페이지 반환하여 앱으로 리다이렉트
        String successHtml = generateSuccessHtml(result);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(successHtml);
    }

    private String generateSuccessHtml(Map<String, Object> result) {
        String spotifyUserId = result.get("spotifyUserId") != null ? result.get("spotifyUserId").toString() : "";
        String encodedSpotifyUserId = URLEncoder.encode(spotifyUserId, StandardCharsets.UTF_8);
        
        // Deep Link URL 생성 (Expo: musiccheck:// 또는 exp://)
        String deepLinkUrl = String.format("musiccheck://spotify-callback?success=true&spotifyUserId=%s", encodedSpotifyUserId);
        
        // 간단한 HTML 페이지 반환
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Spotify 연동 완료</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 20px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    }
                    .container {
                        text-align: center;
                        padding: 40px;
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                        max-width: 400px;
                    }
                    h2 {
                        color: #1DB954;
                        margin-bottom: 20px;
                    }
                    .btn {
                        display: inline-block;
                        margin-top: 20px;
                        padding: 12px 30px;
                        background-color: #1DB954;
                        color: white;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>✅ 완료!</h2>
                    <p>스포티파이 연동이 완료되었습니다.</p>
                    <a href="%s" class="btn">앱으로 돌아가기</a>
                </div>
            </body>
            </html>
            """, deepLinkUrl);
    }

    private String generateErrorHtml(String message) {
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String deepLinkUrl = String.format("musiccheck://spotify-callback?success=false&message=%s", encodedMessage);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Spotify 연동 실패</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 20px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                    }
                    .container {
                        text-align: center;
                        padding: 40px;
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                        max-width: 400px;
                    }
                    h2 {
                        color: #e74c3c;
                        margin-bottom: 20px;
                    }
                    .btn {
                        display: inline-block;
                        margin-top: 20px;
                        padding: 12px 30px;
                        background-color: #e74c3c;
                        color: white;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>❌ Spotify 연동 실패</h2>
                    <p>%s</p>
                    <a href="%s" class="btn">앱으로 돌아가기</a>
                </div>
            </body>
            </html>
            """, message, deepLinkUrl);
    }
}
