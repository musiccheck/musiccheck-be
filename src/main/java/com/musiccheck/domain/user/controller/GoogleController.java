package com.musiccheck.domain.user.controller;

import com.musiccheck.domain.user.service.GoogleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GoogleController {

    private final GoogleService googleService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    /**
     * 구글 로그인 시작 엔드포인트
     */
    @GetMapping("/api/google/connect")
    public ResponseEntity<Map<String, String>> connectGoogle() {
        try {
            String scope = "openid email profile";
            String responseType = "code";
            String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString());

            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                    "?client_id=" + clientId +
                    "&response_type=" + responseType +
                    "&redirect_uri=" + encodedRedirectUri +
                    "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8.toString()) +
                    "&access_type=offline" +
                    "&prompt=consent";

            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            response.put("message", "구글 로그인을 시작합니다.");

            return ResponseEntity.ok(response);

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 인코딩 오류", e);
        }
    }

    /**
     * 구글 OAuth 콜백 엔드포인트
     */
    @GetMapping("/api/google/callback")
    public ResponseEntity<String> googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error) {

        if (error != null) {
            String errorHtml = generateErrorHtml("구글 로그인이 취소되었습니다.");
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(errorHtml);
        }

        if (code == null) {
            String errorHtml = generateErrorHtml("필수 파라미터가 누락되었습니다.");
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(errorHtml);
        }

        Map<String, Object> result = googleService.handleCallback(code);

        // 성공 시 HTML 페이지 반환하여 앱으로 리다이렉트
        String html = result.get("success").equals(true) 
                ? generateSuccessHtml(result) 
                : generateErrorHtml((String) result.get("message"));
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    private String generateSuccessHtml(Map<String, Object> result) {
        String token = result.get("token") != null ? result.get("token").toString() : "";
        String email = result.get("email") != null ? result.get("email").toString() : "";

        // WebView용 HTML 페이지 반환
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>구글 로그인 완료</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 20px;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        background: linear-gradient(135deg, #4285f4 0%%, #34a853 100%%);
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
                        color: #4285f4;
                        margin-bottom: 20px;
                    }
                    .btn {
                        display: inline-block;
                        margin-top: 20px;
                        padding: 12px 30px;
                        background-color: #4285f4;
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
                    <p>구글 로그인이 완료되었습니다.</p>
                    <p style="font-size: 14px; color: #999; margin-top: 20px;">앱으로 돌아가주세요!</p>
                </div>
                <script>
                    // WebView에서 실행 중인지 확인
                    if (window.ReactNativeWebView) {
                        // WebView에 성공 메시지 전송
                        window.ReactNativeWebView.postMessage(JSON.stringify({
                            type: 'googleCallback',
                            success: true,
                            token: '%s',
                            email: '%s'
                        }));
                    }
                </script>
            </body>
            </html>
            """, token, email);
    }

    private String generateErrorHtml(String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>구글 로그인 실패</title>
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
                    <h2>❌ 구글 로그인 실패</h2>
                    <p>%s</p>
                    <p style="font-size: 14px; color: #999; margin-top: 20px;">앱으로 돌아가주세요!</p>
                </div>
                <script>
                    // WebView에서 실행 중인지 확인
                    if (window.ReactNativeWebView) {
                        // WebView에 실패 메시지 전송
                        window.ReactNativeWebView.postMessage(JSON.stringify({
                            type: 'googleCallback',
                            success: false,
                            message: '%s'
                        }));
                    }
                </script>
            </body>
            </html>
            """, message, message);
    }
}
