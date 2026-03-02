package com.volcengine.imagegen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service for handling file storage
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    /**
     * Store an uploaded file
     *
     * @param file The file to store
     * @return The URL/path of the stored file
     * @throws IOException if file storage fails
     */
    public String storeFile(MultipartFile file) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // Store file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Stored file: {} -> {}", originalFilename, filename);

        // Return file URL (for local storage, return relative path)
        return "/uploads/" + filename;
    }

    /**
     * Store an uploaded file and return the absolute path
     *
     * @param file The file to store
     * @return The absolute path of the stored file
     * @throws IOException if file storage fails
     */
    public String storeFileAbsolutePath(MultipartFile file) throws IOException {
        String relativePath = storeFile(file);
        return Paths.get(uploadDir).resolve(relativePath.substring("/uploads/".length())).toAbsolutePath().toString();
    }

    /**
     * Convert a stored file path to URL
     *
     * @param filePath The file path
     * @return The URL for accessing the file
     */
    public String getFileUrl(String filePath) {
        // For local development, return localhost URL
        // In production, this should return the actual CDN/storage URL
        return "http://localhost:8080" + filePath;
    }

    /**
     * Delete a file
     *
     * @param filePath The file path to delete
     * @throws IOException if deletion fails
     */
    public void deleteFile(String filePath) throws IOException {
        if (filePath.startsWith("/uploads/")) {
            String filename = filePath.substring("/uploads/".length());
            Path fullPath = Paths.get(uploadDir).resolve(filename);
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
                log.info("Deleted file: {}", filename);
            }
        }
    }
}
