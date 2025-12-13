package com.musiccheck.domain.user.controller;

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

}