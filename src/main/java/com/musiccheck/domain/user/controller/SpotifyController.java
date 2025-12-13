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
        
        // Deep Link URL 생성 (프론트엔드에서 처리할 형식)
        String deepLinkUrl = String.format("musiccheck://spotify-callback?success=true&spotifyUserId=%s", encodedSpotifyUserId);
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Spotify 연동 완료</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                    }
                    .container {
                        text-align: center;
                        padding: 40px 30px;
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                        max-width: 400px;
                        margin: 20px;
                    }
                    h2 {
                        color: #1DB954;
                        margin-bottom: 20px;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    p {
                        color: #666;
                        margin-bottom: 10px;
                        font-size: 16px;
                        line-height: 1.6;
                    }
                    .spinner {
                        border: 3px solid #f3f3f3;
                        border-top: 3px solid #1DB954;
                        border-radius: 50%%;
                        width: 40px;
                        height: 40px;
                        animation: spin 1s linear infinite;
                        margin: 20px auto;
                    }
                    @keyframes spin {
                        0%% { transform: rotate(0deg); }
                        100%% { transform: rotate(360deg); }
                    }
                    .message {
                        font-size: 14px;
                        color: #999;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>✅ Spotify 연동 완료!</h2>
                    <p>잠시 후 앱으로 돌아갑니다...</p>
                    <div class="spinner"></div>
                    <p class="message">앱이 자동으로 열리지 않으면<br>앱을 직접 열어주세요.</p>
                </div>
                <script>
                    // Deep Link로 앱 열기 시도
                    const deepLinkUrl = '%s';
                    
                    // 즉시 Deep Link 시도
                    try {
                        window.location.href = deepLinkUrl;
                    } catch (e) {
                        console.error('Deep Link 오류:', e);
                    }
                    
                    // 3초 후에도 앱이 열리지 않으면 메시지 업데이트
                    setTimeout(() => {
                        const container = document.querySelector('.container');
                        if (container) {
                            container.innerHTML = 
                                '<h2>✅ 연동 완료</h2>' +
                                '<p style="color: #1DB954; font-size: 18px; margin-bottom: 15px;">스포티파이 연동이 완료되었습니다!</p>' +
                                '<p class="message">앱을 다시 열어주세요.<br>연동 상태가 자동으로 업데이트됩니다.</p>';
                        }
                    }, 3000);
                </script>
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
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: linear-gradient(135deg, #f093fb 0%%, #f5576c 100%%);
                    }
                    .container {
                        text-align: center;
                        padding: 40px 30px;
                        background: white;
                        border-radius: 20px;
                        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
                        max-width: 400px;
                        margin: 20px;
                    }
                    h2 {
                        color: #e74c3c;
                        margin-bottom: 20px;
                        font-size: 24px;
                        font-weight: 600;
                    }
                    p {
                        color: #666;
                        margin-bottom: 10px;
                        font-size: 16px;
                        line-height: 1.6;
                    }
                    .message {
                        font-size: 14px;
                        color: #999;
                        margin-top: 20px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>❌ Spotify 연동 실패</h2>
                    <p>%s</p>
                    <p class="message">앱으로 돌아가주세요.</p>
                </div>
                <script>
                    // Deep Link로 앱 열기 시도 (에러 정보 전달)
                    const deepLinkUrl = '%s';
                    
                    try {
                        window.location.href = deepLinkUrl;
                    } catch (e) {
                        console.error('Deep Link 오류:', e);
                    }
                    
                    // 3초 후에도 앱이 열리지 않으면 메시지 업데이트
                    setTimeout(() => {
                        const container = document.querySelector('.container');
                        if (container) {
                            container.innerHTML += 
                                '<p class="message" style="margin-top: 15px;">앱을 다시 열어주세요.</p>';
                        }
                    }, 3000);
                </script>
            </body>
            </html>
            """, message, deepLinkUrl);
    }
}
