package com.musiccheck.domain.user.service;

import com.musiccheck.domain.user.entity.Role;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User saveOrUpdate(String email, String name, String profile) {
        return userRepository.findByEmail(email)
                .map(user -> user.update(name, profile))  // 업데이트
                .orElseGet(() ->
                        userRepository.save(new User(email, name, profile, Role.USER))
                );
    }

    public User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
    }
}