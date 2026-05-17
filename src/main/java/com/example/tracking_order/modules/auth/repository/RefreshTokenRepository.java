package com.example.tracking_order.modules.auth.repository;

import com.example.tracking_order.modules.auth.entity.RefreshToken;
import com.example.tracking_order.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(User user);
    
    @Modifying
    int deleteByToken(String token);
}
