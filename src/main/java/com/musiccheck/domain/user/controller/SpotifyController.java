package com.musiccheck.domain.user.controller;

import com.musiccheck.domain.user.service.SpotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        
        // WebView용 HTML 페이지 반환
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
                        border: none;
                        cursor: pointer;
                        font-size: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>✅ 완료!</h2>
                    <p>스포티파이 연동이 완료되었습니다.</p>
                    <p style="font-size: 14px; color: #999; margin-top: 20px;">앱으로 돌아가주세요!.</p>
                </div>
                <script>
                    // WebView에서 실행 중인지 확인
                    if (window.ReactNativeWebView) {
                        // WebView에 성공 메시지 전송
                        window.ReactNativeWebView.postMessage(JSON.stringify({
                            type: 'spotifyCallback',
                            success: true,
                            spotifyUserId: '%s'
                        }));
                    }
                </script>
            </body>
            </html>
            """, spotifyUserId);
    }

    private String generateErrorHtml(String message) {
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
                    <p style="font-size: 14px; color: #999; margin-top: 20px;">앱으로 돌아가주세요!.</p>
                </div>
                <script>
                    // WebView에서 실행 중인지 확인
                    if (window.ReactNativeWebView) {
                        // WebView에 실패 메시지 전송
                        window.ReactNativeWebView.postMessage(JSON.stringify({
                            type: 'spotifyCallback',
                            success: false,
                            message: '%s'
                        }));
                    }
                </script>
            </body>
            </html>
            """, message, message);
    }

    /**
     * 스포티파이 플레이리스트 생성 엔드포인트
     */
    @PostMapping("/api/spotify/create-playlist")
    public ResponseEntity<Map<String, Object>> createPlaylist(
            @RequestBody Map<String, Object> requestBody,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        try {
            String email = authentication.getName();
            String bookTitle = (String) requestBody.get("bookTitle");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> songs = (List<Map<String, Object>>) requestBody.get("songs");

            if (bookTitle == null || songs == null || songs.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "필수 파라미터가 누락되었습니다.");
                return ResponseEntity.badRequest().body(error);
            }

            // 플레이리스트 이름 생성
            String playlistName = bookTitle + " - Music Check 추천";

            // 노래 URI 리스트 생성 (spotify:track:xxx 형식)
            List<String> trackUris = songs.stream()
                    .map(song -> {
                        String trackId = (String) song.get("trackId");
                        if (trackId == null) {
                            trackId = (String) song.get("id");
                        }
                        if (trackId != null && !trackId.startsWith("spotify:track:")) {
                            return "spotify:track:" + trackId;
                        }
                        return trackId;
                    })
                    .filter(uri -> uri != null && uri.startsWith("spotify:track:"))
                    .toList();

            if (trackUris.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "유효한 스포티파이 트랙 ID가 없습니다.");
                return ResponseEntity.badRequest().body(error);
            }

            // 플레이리스트 생성
            String spotifyUrl = spotifyService.createPlaylist(email, playlistName, trackUris);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("spotifyUrl", spotifyUrl);
            result.put("message", "플레이리스트가 생성되었습니다.");

            return ResponseEntity.ok(result);

        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            System.err.println("❌ 플레이리스트 생성 오류: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", "플레이리스트 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
