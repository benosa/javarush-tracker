package com.javarush.jira.bugtracking.attachment;

import com.javarush.jira.common.error.IllegalRequestDataException;
import com.javarush.jira.common.error.NotFoundException;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class FileUtil {
    private static final String ATTACHMENT_PATH = "./attachments/%s/";

    public static void upload(MultipartFile multipartFile, String directoryPath, String fileName) {
        if (multipartFile.isEmpty()) {
            throw new IllegalRequestDataException("Select a file to upload.");
        }

        Path directory = Path.of(directoryPath).toAbsolutePath().normalize();
        Path target = directory.resolve(fileName).normalize();
        if (fileName.isBlank() || !directory.equals(target.getParent())
                || !Path.of(fileName).getFileName().toString().equals(fileName)) {
            throw new IllegalRequestDataException("Invalid attachment name");
        }
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            temporary = Files.createTempFile(directory, ".upload-", ".tmp");
            try (InputStream input = multipartFile.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            // Replace only after the entire stream has been written.
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalRequestDataException("Failed to upload file " + multipartFile.getOriginalFilename());
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // A failed cleanup must not hide the upload error.
                }
            }
        }
    }

    public static Resource download(String fileLink) {
        Path path = Paths.get(fileLink);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalRequestDataException("Failed to download file " + resource.getFilename());
            }
        } catch (MalformedURLException ex) {
            throw new NotFoundException("File" + fileLink + " not found");
        }
    }

    public static void delete(String fileLink) {
        Path path = Paths.get(fileLink);
        try {
            Files.delete(path);
        } catch (IOException ex) {
            throw new IllegalRequestDataException("File" + fileLink + " deletion failed.");
        }
    }

    public static String getPath(String titleType) {
        return String.format(ATTACHMENT_PATH, titleType.toLowerCase());
    }
}
