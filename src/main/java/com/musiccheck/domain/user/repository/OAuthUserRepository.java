package com.musiccheck.domain.user.repository;

import com.musiccheck.domain.user.entity.OAuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthUserRepository extends JpaRepository<OAuthUser, Long> {
    Optional<OAuthUser> findByProviderAndProviderUserId(String provider, String providerUserId);
    Optional<OAuthUser> findByUserIdAndProvider(Long userId, String provider);
}

