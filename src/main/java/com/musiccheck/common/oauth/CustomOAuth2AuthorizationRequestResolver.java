package com.musiccheck.common.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * 구글 로그인 시 항상 계정 선택 화면을 보여주기 위한 커스텀 리졸버
 */
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    
    private final OAuth2AuthorizationRequestResolver defaultResolver;
    
    public CustomOAuth2AuthorizationRequestResolver(OAuth2AuthorizationRequestResolver defaultResolver) {
        this.defaultResolver = defaultResolver;
    }
    
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        
        // 구글 로그인인 경우 prompt=select_account 추가
        // registration_id는 authorizationRequest의 clientId나 다른 방법으로 확인
        if (authorizationRequest != null) {
            String clientId = authorizationRequest.getClientId();
            // 구글 클라이언트 ID로 확인 (설정 파일의 client-id와 비교)
            if (clientId != null && clientId.contains("googleusercontent.com")) {
                return customizeAuthorizationRequest(authorizationRequest);
            }
        }
        
        return authorizationRequest;
    }
    
    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        
        // 구글 로그인인 경우 prompt=select_account 추가
        if (authorizationRequest != null && "google".equals(clientRegistrationId)) {
            return customizeAuthorizationRequest(authorizationRequest);
        }
        
        return authorizationRequest;
    }
    
    private OAuth2AuthorizationRequest customizeAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest) {
        Map<String, Object> additionalParameters = new HashMap<>(authorizationRequest.getAdditionalParameters());
        // 항상 계정 선택 화면을 보여주도록 설정
        additionalParameters.put("prompt", "select_account");
        
        Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters);
        
        return builder.build();
    }
}
