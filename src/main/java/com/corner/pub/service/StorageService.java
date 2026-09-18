package com.corner.pub.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

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
        Files.createDirectories(root.resolve("thumbs/prodotti"));
        Files.createDirectories(root.resolve("thumbs/eventi"));
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
        String relative = folder + "/" + basename + "-" + System.currentTimeMillis() + "." + ext;
        Path dest = safeResolve(relative);
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        optimizeImage(dest);
        writeThumb(relative);
        log.info("Salvata foto {} ({} bytes)", relative, Files.size(dest));
        return relative;
    }

    /** Sostituisce una foto: cancella quella vecchia dal disco e scrive il nuovo file. */
    public String replace(String folder, String basename, String oldPublicUrl, MultipartFile file) throws IOException {
        deleteOwned(oldPublicUrl, folder, basename);
        return store(folder, basename, file);
    }

    /** Cancella dal disco sia l'URL pubblico sia eventuali file con lo stesso id. */
    public void deleteOwned(String publicUrl, String folder, String basename) {
        delete(publicUrl);
        if (folder != null && basename != null) {
            deleteByBasename(folder, basename);
            deleteByBasename("thumbs/" + folder, basename);
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        relativePath = toLocalUrl(relativePath);
        if (relativePath == null || relativePath.isBlank()) {
            return;
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
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (ownedFile(p.getFileName().toString(), basename) && Files.deleteIfExists(p)) {
                    log.info("Eliminata foto {}/{}", folder, p.getFileName());
                }
            }
        } catch (IOException e) {
            log.warn("Cleanup {}/{}: {}", folder, basename, e.getMessage());
        }
    }

    /** Accetta sia 18.png sia 18-1712345678.jpg, senza toccare 180.png. */
    private static boolean ownedFile(String filename, String basename) {
        if (filename.equals(basename)) {
            return true;
        }
        String exact = basename + ".";
        if (filename.startsWith(exact)) {
            return true;
        }
        String versioned = basename + "-";
        if (!filename.startsWith(versioned)) {
            return false;
        }
        String rest = filename.substring(versioned.length());
        int dot = rest.lastIndexOf('.');
        String stamp = dot >= 0 ? rest.substring(0, dot) : rest;
        return !stamp.isEmpty() && stamp.chars().allMatch(Character::isDigit);
    }

    public String publicUrl(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String localized = toLocalUrl(relativePath);
        if (localized.startsWith("http://") || localized.startsWith("https://")
                || localized.startsWith("/")) {
            return localized;
        }
        String prefix = publicPath.endsWith("/") ? publicPath : publicPath + "/";
        return prefix + localized;
    }

    /** Miniatura quadrata JPEG per il menu: pesa poco e riempie il riquadro sul telefono. */
    public String publicThumbUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }
        try {
            String thumbRel = ensureThumb(imageUrl);
            if (thumbRel != null) {
                return publicUrl(thumbRel) + "?v=3";
            }
        } catch (Exception e) {
            log.warn("Thumb skip {}: {}", imageUrl, e.getMessage());
        }
        String fallback = publicUrl(imageUrl);
        return fallback == null ? null : fallback + "?v=3";
    }

    public String ensureThumb(String imageUrl) throws IOException {
        String localized = toLocalUrl(imageUrl);
        if (localized == null || localized.isBlank()
                || localized.startsWith("http://") || localized.startsWith("https://")) {
            return null;
        }
        String relative = localized;
        String prefix = publicPath.endsWith("/") ? publicPath : publicPath + "/";
        if (relative.startsWith(prefix)) {
            relative = relative.substring(prefix.length());
        } else if (relative.startsWith("/uploads/")) {
            relative = relative.substring("/uploads/".length());
        }
        if (relative.startsWith("thumbs/")) {
            return relative;
        }
        Path original = safeResolve(relative);
        if (!Files.isRegularFile(original)) {
            return null;
        }
        String fileName = original.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        Path parent = original.getParent();
        String folder = parent != null && parent.startsWith(root)
                ? root.relativize(parent).toString().replace('\\', '/')
                : "prodotti";
        String thumbRel = "thumbs/" + folder + "/" + base + ".jpg";
        Path thumb = safeResolve(thumbRel);
        if (Files.isRegularFile(thumb)
                && Files.getLastModifiedTime(thumb).compareTo(Files.getLastModifiedTime(original)) >= 0
                && Files.size(thumb) > 2000) {
            return thumbRel;
        }
        writeThumbFrom(original, thumb);
        return Files.isRegularFile(thumb) ? thumbRel : null;
    }

    public void writeThumb(String relativeOriginal) {
        try {
            ensureThumb(relativeOriginal);
        } catch (Exception e) {
            log.warn("Thumb write {}: {}", relativeOriginal, e.getMessage());
        }
    }

    /**
     * Converte un URL Cloudinary (o un path relativo) nell'URL pubblico Aruba:
     * {@code /uploads/prodotti/18.png}.
     */
    public static String toLocalUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String value = url.trim();
        int q = value.indexOf('?');
        if (q >= 0) {
            value = value.substring(0, q);
        }
        int hash = value.indexOf('#');
        if (hash >= 0) {
            value = value.substring(0, hash);
        }

        String lower = value.toLowerCase(Locale.ROOT);
        int cloudIdx = lower.indexOf("res.cloudinary.com/");
        if (cloudIdx >= 0) {
            int uploadIdx = lower.indexOf("/image/upload/");
            String path;
            if (uploadIdx >= 0) {
                path = value.substring(uploadIdx + "/image/upload/".length());
            } else {
                int slash = value.indexOf('/', cloudIdx + "res.cloudinary.com/".length());
                path = slash >= 0 ? value.substring(slash + 1) : "";
            }
            path = stripCloudinaryTransforms(path);
            if (path.startsWith("event/")) {
                path = "eventi/" + path.substring("event/".length());
            }
            return "/uploads/" + path;
        }
        return value;
    }

    private static String stripCloudinaryTransforms(String path) {
        String[] parts = path.split("/");
        int i = 0;
        while (i < parts.length) {
            String p = parts[i];
            if (p.isEmpty() || p.matches("v\\d+") || p.indexOf(',') >= 0
                    || p.startsWith("w_") || p.startsWith("h_") || p.startsWith("c_")
                    || p.startsWith("q_") || p.startsWith("f_") || p.startsWith("dpr_")
                    || p.startsWith("e_")) {
                i++;
                continue;
            }
            break;
        }
        if (i >= parts.length) {
            return path;
        }
        StringBuilder sb = new StringBuilder();
        for (int j = i; j < parts.length; j++) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(parts[j]);
        }
        return sb.toString();
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

    private void optimizeImage(Path dest) {
        String name = dest.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))) {
            return;
        }
        try {
            BufferedImage src = ImageIO.read(dest.toFile());
            if (src == null) {
                return;
            }
            int max = 480;
            int w = src.getWidth();
            int h = src.getHeight();
            long size = Files.size(dest);
            if (w <= max && h <= max && size < 80_000) {
                return;
            }
            double scale = Math.min(1d, Math.min(max / (double) w, max / (double) h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            boolean jpeg = name.endsWith(".jpg") || name.endsWith(".jpeg");
            BufferedImage out = new BufferedImage(nw, nh, jpeg ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (jpeg) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, nw, nh);
            }
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();
            if (jpeg) {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) {
                    ImageIO.write(out, "jpg", dest.toFile());
                    return;
                }
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.72f);
                try (ImageOutputStream ios = ImageIO.createImageOutputStream(dest.toFile())) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(out, null, null), param);
                } finally {
                    writer.dispose();
                }
            } else {
                ImageIO.write(out, name.endsWith(".webp") ? "webp" : "png", dest.toFile());
            }
            log.info("Foto ottimizzata {} -> {} bytes", dest.getFileName(), Files.size(dest));
        } catch (Exception e) {
            log.warn("Optimize skip {}: {}", dest.getFileName(), e.getMessage());
        }
    }

    private void writeThumbFrom(Path original, Path thumb) throws IOException {
        BufferedImage src = ImageIO.read(original.toFile());
        if (src == null) {
            return;
        }
        Files.createDirectories(thumb.getParent());
        int target = 400;
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.max(target / (double) w, target / (double) h);
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        int x = (target - nw) / 2;
        int y = (target - nh) / 2;
        BufferedImage out = new BufferedImage(target, target, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(new Color(0x1c1c1c));
        g.fillRect(0, 0, target, target);
        g.drawImage(src, x, y, nw, nh, null);
        g.dispose();
        writeJpeg(out, thumb, 0.72f);
        log.info("Thumb {} -> {} bytes", thumb.getFileName(), Files.size(thumb));
    }

    private void writeJpeg(BufferedImage image, Path dest, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ImageIO.write(image, "jpg", dest.toFile());
            return;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(dest.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
