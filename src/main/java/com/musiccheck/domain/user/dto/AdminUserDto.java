package com.musiccheck.domain.user.dto;

import com.musiccheck.domain.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminUserDto {
    private Long userId;
    private String email;
    private String name;
    private LocalDateTime createdAt;

    public AdminUserDto(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.createdAt = user.getCreatedAt();
    }
}
