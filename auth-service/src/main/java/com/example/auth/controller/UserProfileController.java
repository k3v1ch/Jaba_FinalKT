package com.example.auth.controller;

import com.example.auth.dto.UserProfileDto;
import com.example.auth.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public UserProfileDto.ProfileResponse getProfile(Authentication auth) {
        return userProfileService.getProfile(auth.getName());
    }

    @PutMapping("/me")
    public UserProfileDto.ProfileResponse updateProfile(Authentication auth,
                                                        @Valid @RequestBody UserProfileDto.UpdateProfileRequest req) {
        return userProfileService.updateProfile(auth.getName(), req);
    }

    @PostMapping(value = "/me/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileDto.ImageInfo> uploadImage(Authentication auth,
                                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileService.uploadImage(auth.getName(), file));
    }

    @DeleteMapping("/me/images/{imageId}")
    public ResponseEntity<Void> deleteImage(Authentication auth, @PathVariable Long imageId) {
        userProfileService.deleteImage(auth.getName(), imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = userProfileService.getImage(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }
}
