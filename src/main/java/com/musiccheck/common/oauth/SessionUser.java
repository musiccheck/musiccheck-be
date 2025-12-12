package com.musiccheck.common.oauth;

import com.musiccheck.domain.user.entity.User;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class SessionUser implements Serializable {
    private String name;
    private String email;
    private String profile;

    public SessionUser(User user) {
        this.name = user.getName();
        this.email = user.getEmail();
        this.profile = user.getProfile();

    }
}