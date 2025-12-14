package com.musiccheck.common.oauth;

import com.musiccheck.common.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = extractEmail(oAuth2User);
        String token = jwtTokenProvider.createToken(email);

        // 구글 로그인인 경우 HTML 반환 (스포티파이처럼)
        String requestURI = request.getRequestURI();
        System.out.println("=== OAuth2SuccessHandler 디버깅 ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Email: " + email);
        System.out.println("Contains /oauth2/code/google: " + (requestURI != null && requestURI.contains("/oauth2/code/google")));
        System.out.println("Contains /code/google: " + (requestURI != null && requestURI.contains("/code/google")));
        System.out.println("===================================");
        
        if (requestURI != null && (requestURI.contains("/oauth2/code/google") || requestURI.contains("/code/google"))) {
            // HTML 응답 반환
            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            
            String html = generateSuccessHtml(token, email);
            response.getWriter().write(html);
            return;
        }

        // 카카오, 네이버는 기존대로 JSON 반환
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("token", token);
        result.put("email", email);
        result.put("message", "로그인 성공");

        objectMapper.writeValue(response.getWriter(), result);
    }

    private String extractEmail(OAuth2User oAuth2User) {
        String email = (String) oAuth2User.getAttributes().get("email");
        if (email == null && oAuth2User.getAttributes().containsKey("response")) {
            // 네이버의 경우
            Map<String, Object> response = (Map<String, Object>) oAuth2User.getAttributes().get("response");
            email = (String) response.get("email");
        } else if (email == null && oAuth2User.getAttributes().containsKey("kakao_account")) {
            // 카카오의 경우
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
            email = (String) kakaoAccount.get("email");
        }
        return email;
    }

    private String generateSuccessHtml(String token, String email) {
        try {
            // 토큰과 이메일을 URL 인코딩
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString());
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());
            
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
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>✅ 완료!</h2>
                        <p>구글 로그인이 완료되었습니다.</p>
                        <p style="font-size: 14px; color: #999; margin-top: 20px;">앱으로 돌아가주세요!</p>
                    </div>
                    <script>
                        console.log('구글 로그인 HTML 페이지 로드됨');
                        
                        // WebView에서 실행 중인지 확인 (카카오/네이버용)
                        if (window.ReactNativeWebView) {
                            console.log('WebView 메시지 전송 시도');
                            const message = JSON.stringify({
                                type: 'googleCallback',
                                success: true,
                                token: '%s',
                                email: '%s'
                            });
                            console.log('전송할 메시지:', message);
                            window.ReactNativeWebView.postMessage(message);
                            console.log('메시지 전송 완료');
                        } else {
                            // 외부 브라우저인 경우 Deep Link로 리다이렉트
                            console.log('외부 브라우저 감지 - Deep Link로 리다이렉트');
                            const deepLink = 'musiccheck://google-callback?success=true&token=%s&email=%s';
                            console.log('Deep Link:', deepLink);
                            
                            // 즉시 리다이렉트 시도
                            setTimeout(function() {
                                try {
                                    window.location.href = deepLink;
                                    console.log('Deep Link 리다이렉트 완료');
                                } catch (e) {
                                    console.error('Deep Link 리다이렉트 실패:', e);
                                    // 실패 시 사용자에게 안내
                                    alert('앱으로 돌아가주세요. 로그인이 완료되었습니다.');
                                }
                            }, 1500); // 1.5초 후 리다이렉트
                        }
                    </script>
                </body>
                </html>
                """, token, email, encodedToken, encodedEmail);
        } catch (Exception e) {
            System.err.println("❌ HTML 생성 오류: " + e.getMessage());
            e.printStackTrace();
            // 오류 발생 시 기본 HTML 반환
            return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>구글 로그인 완료</title>
                </head>
                <body>
                    <h2>✅ 완료!</h2>
                    <p>구글 로그인이 완료되었습니다.</p>
                    <p>앱으로 돌아가주세요!</p>
                    <script>
                        setTimeout(function() {
                            window.location.href = 'musiccheck://google-callback?success=true';
                        }, 1500);
                    </script>
                </body>
                </html>
                """);
        }
    }
}