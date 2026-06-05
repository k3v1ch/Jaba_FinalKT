package com.example.auth.service;

import com.example.auth.dto.UserProfileDto;
import com.example.auth.entity.*;
import com.example.auth.exception.*;
import com.example.auth.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${app.uploads.profile-images-dir:./uploads/profile-images}")
    private String uploadDir;

    @Value("${app.uploads.max-images-per-user:5}")
    private int maxImages;

    private Path imagesPath;

    private final UserRepository userRepository;
    private final ProfileImageRepository profileImageRepository;

    @PostConstruct
    void init() throws IOException {
        imagesPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(imagesPath);
        log.info("Profile images dir: {}", imagesPath);
    }

    public UserProfileDto.ProfileResponse getProfile(String email) {
        User user = findUser(email);
        List<UserProfileDto.ImageInfo> images = profileImageRepository.findAllByUserId(user.getId()).stream()
                .map(i -> new UserProfileDto.ImageInfo(i.getId(), i.getFilename(), i.getUploadedAt()))
                .toList();
        return UserProfileDto.ProfileResponse.from(user, images);
    }

    public UserProfileDto.ProfileResponse updateProfile(String email, UserProfileDto.UpdateProfileRequest req) {
        User user = findUser(email);
        user.setName(req.getName().trim());
        user.setBio(req.getBio());
        userRepository.save(user);
        return getProfile(email);
    }

    public UserProfileDto.ImageInfo uploadImage(String email, MultipartFile file) {
        User user = findUser(email);

        if (profileImageRepository.countByUserId(user.getId()) >= maxImages)
            throw new AppException(ErrorCode.PROFILE_IMAGE_LIMIT,
                    "Лимит: " + maxImages + " изображений");

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType))
            throw new AppException(ErrorCode.PROFILE_IMAGE_INVALID, "type=" + contentType);

        String ext = contentType.substring(contentType.indexOf('/') + 1);
        String filename = "user_" + user.getId() + "_" + UUID.randomUUID() + "." + ext;
        Path target = imagesPath.resolve(filename);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AppException(ErrorCode.PROFILE_UPLOAD_FAILED, filename, e);
        }

        ProfileImage saved = profileImageRepository.save(ProfileImage.builder()
                .user(user).filename(filename)
                .contentType(contentType).fileSize(file.getSize()).build());

        return new UserProfileDto.ImageInfo(saved.getId(), saved.getFilename(), saved.getUploadedAt());
    }

    public Resource getImage(String filename) {
        Path path = imagesPath.resolve(filename).normalize();
        if (!path.startsWith(imagesPath) || !Files.exists(path))
            throw new AppException(ErrorCode.PROFILE_IMAGE_NOT_FOUND, filename);
        try {
            return new UrlResource(path.toUri());
        } catch (Exception e) {
            throw new AppException(ErrorCode.PROFILE_UPLOAD_FAILED, filename, e);
        }
    }

    public void deleteImage(String email, Long imageId) {
        User user = findUser(email);
        ProfileImage image = profileImageRepository.findByIdAndUserId(imageId, user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_IMAGE_NOT_FOUND, "id=" + imageId));
        try {
            Files.deleteIfExists(imagesPath.resolve(image.getFilename()));
        } catch (IOException e) {
            log.warn("Could not delete file {}: {}", image.getFilename(), e.getMessage());
        }
        profileImageRepository.delete(image);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_USER_NOT_FOUND, email));
    }
}
