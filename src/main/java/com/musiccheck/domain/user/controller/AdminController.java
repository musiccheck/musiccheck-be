package com.musiccheck.domain.user.controller;

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
