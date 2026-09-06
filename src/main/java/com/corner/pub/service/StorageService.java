package com.corner.pub.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.public-path:/uploads}")
    private String publicPath;

    private Path root;

    @PostConstruct
    public void init() throws IOException {
        root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Files.createDirectories(root.resolve("prodotti"));
        Files.createDirectories(root.resolve("eventi"));
        log.info("Upload directory: {}", root);
    }

    public Path getRoot() {
        return root;
    }

    public String store(String folder, String basename, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("File vuoto");
        }
        Path dir = safeResolve(folder);
        Files.createDirectories(dir);
        deleteByBasename(folder, basename);

        String ext = extension(file);
        String relative = folder + "/" + basename + "." + ext;
        Path dest = safeResolve(relative);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("Salvata foto {} ({} bytes)", relative, file.getSize());
        return relative;
    }

    /** Sostituisce una foto: cancella quella vecchia e scrive il nuovo file. */
    public String replace(String folder, String basename, String oldPublicUrl, MultipartFile file) throws IOException {
        delete(oldPublicUrl);
        return store(folder, basename, file);
    }

    /** Cancella dal disco sia l'URL pubblico sia eventuali file con lo stesso id. */
    public void deleteOwned(String publicUrl, String folder, String basename) {
        delete(publicUrl);
        if (folder != null && basename != null) {
            deleteByBasename(folder, basename);
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        int q = relativePath.indexOf('?');
        if (q >= 0) {
            relativePath = relativePath.substring(0, q);
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return;
        }
        String prefix = publicPath.endsWith("/") ? publicPath : publicPath + "/";
        if (relativePath.startsWith(prefix)) {
            relativePath = relativePath.substring(prefix.length());
        } else if (relativePath.startsWith("/uploads/")) {
            relativePath = relativePath.substring("/uploads/".length());
        }
        try {
            Path dest = safeResolve(relativePath);
            if (Files.deleteIfExists(dest)) {
                log.info("Eliminata foto {}", relativePath);
            }
        } catch (Exception e) {
            log.warn("Impossibile cancellare {}: {}", relativePath, e.getMessage());
        }
    }

    public void deleteByBasename(String folder, String basename) {
        Path dir = root.resolve(folder).normalize();
        if (!dir.startsWith(root) || !Files.isDirectory(dir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, basename + ".*")) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
            }
        } catch (IOException e) {
            log.warn("Cleanup {}/{}: {}", folder, basename, e.getMessage());
        }
    }

    public String publicUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")
                || relativePath.startsWith("/")) {
            return relativePath;
        }
        String prefix = publicPath.endsWith("/") ? publicPath : publicPath + "/";
        return prefix + relativePath;
    }

    private Path safeResolve(String relative) throws IOException {
        Path dest = root.resolve(relative).normalize();
        if (!dest.startsWith(root)) {
            throw new IOException("Percorso non valido");
        }
        return dest;
    }

    private String extension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (ext.matches("[a-z0-9]{1,8}")) {
                return ext;
            }
        }
        String ct = file.getContentType();
        if (ct == null) {
            return "bin";
        }
        return switch (ct) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/gif" -> "gif";
            case "image/svg+xml" -> "svg";
            default -> "bin";
        };
    }
}
