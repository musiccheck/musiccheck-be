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

        // 구글 로그인인지 확인 (URI에 /code/google이 포함되어 있는지)
        String requestUri = request.getRequestURI();
        boolean isGoogle = requestUri != null && requestUri.contains("/code/google");

        if (isGoogle) {
            // 구글 로그인: HTML 페이지 반환 (WebView용)
            String html = generateSuccessHtml(token, email);
            response.setContentType("text/html; charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(html);
        } else {
            // 네이버/카카오 로그인: JSON 응답 반환 (기존 방식)
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("token", token);
            result.put("email", email);
            result.put("message", "로그인 성공");

            objectMapper.writeValue(response.getWriter(), result);
        }
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

    /**
     * 구글 로그인 성공 시 HTML 페이지 생성 (WebView용)
     */
    private String generateSuccessHtml(String token, String email) {
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
}