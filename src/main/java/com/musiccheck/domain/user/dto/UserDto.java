package com.musiccheck.domain.user.dto;

import com.musiccheck.domain.user.entity.User;
import lombok.Getter;

@Getter
public class UserDto {
    private String name;
    private String email;
    private String profile;
    private String role;

    public UserDto(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.profile = user.getProfile();
        this.role = user.getRoleKey();
    }
}