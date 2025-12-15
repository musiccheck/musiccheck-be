package com.musiccheck.domain.user.controller;

import com.musiccheck.common.jwt.JwtTokenProvider;
import com.musiccheck.domain.user.dto.AdminUserDto;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    
    // 관리자 계정 정보 (하드코딩, 나중에 DB로 변경 가능)
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin1234";
    private static final String ADMIN_EMAIL = "admin@musiccheck.store"; // 관리자 이메일

    /**
     * 관리자 로그인 API
     * username/password로 인증 후 JWT 토큰 발급
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Map<String, String> credentials) {
        System.out.println("🔍 [Admin] 로그인 요청 받음");
        
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        System.out.println("🔍 [Admin] 입력된 아이디: " + username);
        System.out.println("🔍 [Admin] 입력된 비밀번호: " + (password != null ? "***" : "null"));
        
        // 입력값 검증
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            System.out.println("⚠️ [Admin] 입력값 누락");
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "아이디와 비밀번호를 입력해주세요.");
            return ResponseEntity.status(400).body(error);
        }
        
        // 관리자 인증
        String trimmedUsername = username.trim();
        String trimmedPassword = password.trim();
        
        if (!ADMIN_USERNAME.equals(trimmedUsername) || !ADMIN_PASSWORD.equals(trimmedPassword)) {
            System.out.println("⚠️ [Admin] 인증 실패: 아이디 또는 비밀번호 불일치");
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return ResponseEntity.status(401).body(error);
        }
        
        // JWT 토큰 발급
        String token = jwtTokenProvider.createToken(ADMIN_EMAIL);
        System.out.println("✅ [Admin] 인증 성공, JWT 토큰 발급 완료");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("email", ADMIN_EMAIL);
        response.put("message", "관리자 로그인 성공");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 사용자 목록 조회 (관리자용)
     * 응답에 totalCount 필드 포함
     */
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers(Authentication authentication) {
        System.out.println("🔍 [Admin] /api/admin/users 요청 받음");
        System.out.println("🔍 [Admin] Authentication: " + (authentication != null ? authentication.getName() : "null"));
        System.out.println("🔍 [Admin] isAuthenticated: " + (authentication != null ? authentication.isAuthenticated() : "false"));
        
        // 인증 확인
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("⚠️ [Admin] 인증 실패: 로그인이 필요합니다.");
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        // 일반 사용자 수 조회 (SELECT COUNT(*) FROM user)
        long totalCount = userRepository.count();
        System.out.println("✅ [Admin] 일반 사용자 수 조회: " + totalCount + "명");
        
        // 전체 사용자 목록 조회
        List<User> users = userRepository.findAll();
        System.out.println("✅ [Admin] 전체 사용자 목록 조회: " + users.size() + "명");
        
        // DTO로 변환
        List<AdminUserDto> userDtos = users.stream()
                .map(AdminUserDto::new)
                .collect(Collectors.toList());

        // 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalCount", totalCount);  // COUNT(*) 쿼리로 조회한 일반 사용자 수
        response.put("users", userDtos);

        System.out.println("✅ [Admin] 응답 전송: success=true, totalCount=" + totalCount + ", users.size()=" + userDtos.size());
        System.out.println("✅ [Admin] 응답 JSON: " + response.toString());
        return ResponseEntity.ok(response);
    }
}
