package com.example.auth.repository;

import com.example.auth.entity.ProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProfileImageRepository extends JpaRepository<ProfileImage, Long> {
    List<ProfileImage> findAllByUserId(Long userId);
    long countByUserId(Long userId);
    Optional<ProfileImage> findByIdAndUserId(Long id, Long userId);
}
