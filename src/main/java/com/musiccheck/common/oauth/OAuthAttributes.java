package com.musiccheck.common.oauth;

import com.musiccheck.domain.user.entity.Role;
import com.musiccheck.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String profile;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String profile) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.profile = profile;
    }

    // 구글, 네이버, 카카오 판단
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equals(registrationId)) {
            return ofNaver("id", attributes);
        }
        if ("kakao".equals(registrationId)) {
            return ofKakao("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    //구글
    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        String name = (String) attributes.get("name");
        String email = (String) attributes.get("email");
        return OAuthAttributes.builder()
                .name(name != null ? name : (email != null ? email.split("@")[0] : "사용자"))
                .email(email)
                .profile((String) attributes.get("profile"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    //네이버
    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        String name = (String) response.get("name");
        String email = (String) response.get("email");

        return OAuthAttributes.builder()
                .name(name != null ? name : (email != null ? email.split("@")[0] : "사용자"))
                .email(email)
                .profile((String) response.get("profile_image"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    //카카오
    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String name = profile != null ? (String) profile.get("nickname") : null;
        String email = (String) kakaoAccount.get("email");

        return OAuthAttributes.builder()
                .name(name != null ? name : (email != null ? email.split("@")[0] : "사용자"))
                .email(email)
                .profile(profile != null ? (String) profile.get("profile_image_url") : null)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity() {
        User user = new User(email);
        // name이 null이면 email의 @ 앞부분을 사용, 그것도 없으면 "사용자"
        String finalName = name != null ? name : (email != null ? email.split("@")[0] : "사용자");
        user.setOAuthAttributes(finalName, profile, Role.GUEST);
        return user;
    }
}