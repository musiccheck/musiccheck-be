package com.musiccheck.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "\"user\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "nickname", nullable = false)
    private String name;

    @Column(name = "profile")
    private String profile;

    @Column(name = "spotify_connected")
    private Boolean spotifyConnected = false;

    @Transient
    private Role role;

    @Builder
    public User(String email) {
        this.email = email;
    }

    public User(String email, String name, String profile, Role role) {
        this.email = email;
        this.name = name;
        this.profile = profile;
        this.role = role;
    }

    public void setOAuthAttributes(String name, String profile, Role role) {
        this.name = name;
        this.profile = profile;
        this.role = role;
    }

    public User update(String name, String profile) {
        this.name = name;
        this.profile = profile;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    public void setSpotifyConnected(Boolean connected) {
        this.spotifyConnected = connected;
        this.updatedAt = LocalDateTime.now();
    }

    public String getRoleKey() {
        return this.role != null ? this.role.getKey() : Role.GUEST.getKey();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}