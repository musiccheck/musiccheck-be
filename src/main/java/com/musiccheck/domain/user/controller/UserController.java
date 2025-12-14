package com.musiccheck.domain.user.controller;

import com.musiccheck.common.oauth.OAuth2SuccessHandler;
import com.musiccheck.domain.user.dto.UserDto;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import com.musiccheck.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;

    // 내 정보 조회 API
    @GetMapping("/api/user/me")
    public UserDto getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                    "사용자를 찾을 수 없습니다. OAuth 로그인을 다시 진행해주세요. (Email: " + email + ")"));

        return new UserDto(user);
    }

    // 회원탈퇴
    @DeleteMapping("/api/user")
    public ResponseEntity<Map<String, Object>> deleteAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 회원탈퇴 처리 (관련 데이터 모두 삭제)
        userService.deleteAccount(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "회원탈퇴가 완료되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 구글 로그인 임시 토큰 조회 API (이메일로)
     * 외부 브라우저에서 설정된 쿠키를 앱에서 읽을 수 없으므로, 이메일로 임시 토큰 조회
     * 구글 로그인 직후 5분간만 유효하며, 1회 조회 후 자동 삭제
     */
    @GetMapping("/api/user/google-token")
    public ResponseEntity<Map<String, Object>> getGoogleLoginToken(@RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();
        
        // 이메일이 제공된 경우
        if (email != null && !email.isEmpty()) {
            String token = OAuth2SuccessHandler.getGoogleLoginToken(email);
            
            if (token != null) {
                response.put("success", true);
                response.put("token", token);
                response.put("email", email);
                System.out.println("✅ [Google] 임시 토큰 조회 성공 (이메일: " + email + ")");
            } else {
                response.put("success", false);
                response.put("message", "토큰을 찾을 수 없습니다. 구글 로그인을 다시 진행해주세요.");
                System.out.println("⚠️ [Google] 임시 토큰 조회 실패 (이메일: " + email + ", 토큰 없음 또는 만료)");
            }
        } else {
            // 이메일이 없는 경우: 최근 로그인한 사용자의 토큰 조회
            Map<String, String> recentLogin = OAuth2SuccessHandler.getRecentGoogleLoginToken();
            
            if (recentLogin != null && recentLogin.containsKey("token")) {
                response.put("success", true);
                response.put("token", recentLogin.get("token"));
                response.put("email", recentLogin.get("email"));
                System.out.println("✅ [Google] 최근 로그인 사용자 토큰 조회 성공 (이메일: " + recentLogin.get("email") + ")");
            } else {
                response.put("success", false);
                response.put("message", "최근 로그인한 사용자를 찾을 수 없습니다. 구글 로그인을 다시 진행해주세요.");
                System.out.println("⚠️ [Google] 최근 로그인 사용자 토큰 조회 실패 (토큰 없음 또는 만료)");
            }
        }
        
        return ResponseEntity.ok(response);
    }

}