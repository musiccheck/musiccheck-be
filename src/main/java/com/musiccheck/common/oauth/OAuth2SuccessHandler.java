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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 구글 로그인 임시 토큰 저장소 (이메일 -> 토큰, 5분간 유효)
    private static final Map<String, String> googleLoginTokens = new ConcurrentHashMap<>();
    // 최근 로그인한 이메일 저장 (가장 최근 1개만, 5분간 유효)
    private static volatile String recentGoogleLoginEmail = null;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    static {
        // 5분마다 만료된 토큰 정리
        scheduler.scheduleAtFixedRate(() -> {
            // 토큰이 너무 많아지면 전체 삭제 (5분 후 자동 만료)
            if (googleLoginTokens.size() > 100) {
                googleLoginTokens.clear();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

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
            // JWT 토큰을 쿠키에 설정 (프론트엔드에서 credentials: 'include'로 사용)
            jakarta.servlet.http.Cookie tokenCookie = new jakarta.servlet.http.Cookie("jwt_token", token);
            tokenCookie.setHttpOnly(true);
            tokenCookie.setSecure(true); // HTTPS에서만 전송 (프로덕션 환경)
            tokenCookie.setPath("/");
            tokenCookie.setMaxAge(60 * 60 * 24 * 7); // 7일
            // SameSite 설정: None으로 설정하여 크로스 사이트 요청에서도 쿠키 전송 가능
            // (외부 브라우저에서 설정된 쿠키가 앱의 API 호출에서도 사용되도록)
            try {
                // Java 11+ 에서는 reflection을 사용하여 SameSite 설정
                java.lang.reflect.Method setSameSiteMethod = tokenCookie.getClass().getMethod("setAttribute", String.class, String.class);
                setSameSiteMethod.invoke(tokenCookie, "SameSite", "None");
            } catch (Exception e) {
                // SameSite 설정 실패 시 로그만 출력 (Java 버전에 따라 지원되지 않을 수 있음)
                System.out.println("⚠️ [Google] SameSite 설정 실패 (무시 가능): " + e.getMessage());
            }
            response.addCookie(tokenCookie);
            
            // 임시 토큰 저장소에 저장 (5분간 유효, 프론트엔드가 이메일로 조회 가능)
            googleLoginTokens.put(email, token);
            // 최근 로그인한 이메일 저장
            recentGoogleLoginEmail = email;
            // 5분 후 자동 삭제
            scheduler.schedule(() -> {
                googleLoginTokens.remove(email);
                if (recentGoogleLoginEmail != null && recentGoogleLoginEmail.equals(email)) {
                    recentGoogleLoginEmail = null;
                }
            }, 5, TimeUnit.MINUTES);
            
            System.out.println("✅ [Google] JWT 토큰 쿠키 설정 완료 (프론트엔드가 credentials: 'include'로 사용 가능)");
            System.out.println("✅ [Google] 임시 토큰 저장소에 저장 완료 (이메일: " + email + ", 5분간 유효)");
            
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
        // 스포티파이처럼 단순히 완료 메시지만 표시 (Deep Link 리다이렉트 없음)
        // 프론트엔드에서 브라우저가 닫힌 후 폴링으로 로그인 상태 확인
        // 이메일을 메타 태그와 숨겨진 필드에 포함시켜서 프론트엔드가 읽을 수 있게 함
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="google-login-email" content="%s">
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
                <!-- 이메일 정보를 숨겨진 필드에 저장 (프론트엔드가 읽을 수 있도록) -->
                <input type="hidden" id="google-login-email" value="%s">
                <input type="hidden" id="google-login-token" value="%s">
                <script>
                    // WebView에서 실행 중인지 확인 (카카오/네이버용)
                    if (window.ReactNativeWebView) {
                        // WebView에 성공 메시지 전송
                        window.ReactNativeWebView.postMessage(JSON.stringify({
                            type: 'googleCallback',
                            success: true,
                            token: '%s',
                            email: '%s'
                        }));
                    }
                    
                    // 외부 브라우저에서도 이메일을 읽을 수 있도록 전역 변수에 저장
                    window.googleLoginEmail = '%s';
                    window.googleLoginToken = '%s';
                    
                    // URL에 이메일을 포함시켜서 프론트엔드가 읽을 수 있게 함 (선택적)
                    // 현재 URL에 이미 이메일이 없을 때만 추가
                    if (!window.location.search.includes('email=')) {
                        const newUrl = window.location.href.split('?')[0] + '?email=' + encodeURIComponent('%s');
                        // history.replaceState를 사용하여 URL만 변경 (페이지 리로드 없음)
                        window.history.replaceState({}, '', newUrl);
                    }
                </script>
            </body>
            </html>
            """, email, token, email, token, email, email, email);
    }
    
    /**
     * 구글 로그인 임시 토큰 조회 (프론트엔드용)
     * 외부 브라우저에서 설정된 쿠키를 앱에서 읽을 수 없으므로, 이메일로 임시 토큰 조회
     */
    public static String getGoogleLoginToken(String email) {
        return googleLoginTokens.remove(email); // 조회 후 즉시 삭제 (1회용)
    }
    
    /**
     * 최근 로그인한 구글 이메일 조회 (프론트엔드용)
     * 이메일을 모를 때 최근 로그인한 사용자의 이메일을 조회
     */
    public static String getRecentGoogleLoginEmail() {
        return recentGoogleLoginEmail;
    }
    
    /**
     * 최근 로그인한 구글 사용자의 토큰 조회 (이메일 없이)
     * 프론트엔드가 이메일을 모를 때 사용
     */
    public static Map<String, String> getRecentGoogleLoginToken() {
        if (recentGoogleLoginEmail != null) {
            String token = googleLoginTokens.remove(recentGoogleLoginEmail);
            if (token != null) {
                Map<String, String> result = new HashMap<>();
                result.put("email", recentGoogleLoginEmail);
                result.put("token", token);
                recentGoogleLoginEmail = null; // 조회 후 삭제
                return result;
            }
        }
        return null;
    }
}