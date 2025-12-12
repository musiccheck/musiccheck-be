package com.musiccheck.domain.user.service;

import com.musiccheck.domain.book.repository.UserHistoryRepository;
import com.musiccheck.domain.music.repository.UserFeedbackRepository;
import com.musiccheck.domain.user.entity.Role;
import com.musiccheck.domain.user.entity.User;
import com.musiccheck.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserFeedbackRepository userFeedbackRepository;
    private final UserHistoryRepository userHistoryRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

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

    // 회원탈퇴 (관련 데이터 모두 삭제)
    @Transactional
    public void deleteAccount(Long userId) {
        // 1) user_feedback 삭제
        userFeedbackRepository.deleteByUserId(userId);
        
        // 2) user_history 삭제
        userHistoryRepository.deleteByUserId(userId);
        
        // 3) oauth_user 삭제 (Native Query)
        entityManager.createNativeQuery("DELETE FROM oauth_user WHERE user_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        
        // 4) playlist_generation_log 삭제 (Native Query - 나중에 엔티티 생기면 추가)
        entityManager.createNativeQuery("DELETE FROM playlist_generation_log WHERE user_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();
        
        // 5) user 삭제 (마지막에 삭제)
        userRepository.deleteById(userId);
    }
}