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
        // 인증 확인
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "로그인이 필요합니다.");
            return ResponseEntity.status(401).body(error);
        }

        // 전체 사용자 조회
        List<User> users = userRepository.findAll();
        
        // DTO로 변환
        List<AdminUserDto> userDtos = users.stream()
                .map(AdminUserDto::new)
                .collect(Collectors.toList());

        // 응답 생성
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalCount", users.size());  // 전체 사용자 수
        response.put("users", userDtos);

        return ResponseEntity.ok(response);
    }
}
