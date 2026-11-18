package com.corner.pub.service;

import com.corner.pub.dto.response.RunClubSponsorResponse;
import com.corner.pub.exception.CornerPubException;
import com.corner.pub.model.RunClubSponsor;
import com.corner.pub.repository.RunClubSponsorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RunClubSponsorService {

    private static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final RunClubSponsorRepository repository;
    private final StorageService storageService;

    public RunClubSponsorService(RunClubSponsorRepository repository, StorageService storageService) {
        this.repository = repository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public List<RunClubSponsorResponse> list() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(RunClubSponsorResponse::new)
                .toList();
    }

    @Transactional
    public RunClubSponsorResponse create(String name, String socialUrl, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CornerPubException("Carica l'immagine dello sponsor.");
        }
        RunClubSponsor sponsor = new RunClubSponsor();
        sponsor.setName(cleanName(name));
        sponsor.setSocialUrl(normalizeSocial(socialUrl));
        sponsor.setSortOrder(repository.findAll().size() + 1);
        sponsor.setImagePath("pending");
        sponsor = repository.save(sponsor);
        sponsor.setImagePath(storeImage(sponsor.getId(), image));
        return new RunClubSponsorResponse(repository.save(sponsor));
    }

    @Transactional
    public RunClubSponsorResponse update(Long id, String name, String socialUrl, MultipartFile image) {
        RunClubSponsor sponsor = require(id);
        sponsor.setName(cleanName(name));
        sponsor.setSocialUrl(normalizeSocial(socialUrl));
        if (image != null && !image.isEmpty()) {
            String previous = sponsor.getImagePath();
            sponsor.setImagePath(storeImage(sponsor.getId(), image));
            storageService.delete(previous);
        }
        return new RunClubSponsorResponse(repository.save(sponsor));
    }

    @Transactional
    public void delete(Long id) {
        RunClubSponsor sponsor = require(id);
        storageService.deleteOwned(sponsor.getImagePath(), "sponsor", String.valueOf(sponsor.getId()));
        repository.delete(sponsor);
    }

    private RunClubSponsor require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CornerPubException("Sponsor non trovato."));
    }

    private String storeImage(Long id, MultipartFile image) {
        String type = image.getContentType() == null ? "" : image.getContentType().toLowerCase(Locale.ROOT);
        String name = image.getOriginalFilename() == null ? "" : image.getOriginalFilename().toLowerCase(Locale.ROOT);
        boolean knownType = type.equals("image/jpeg") || type.equals("image/png")
                || type.equals("image/webp") || type.equals("image/gif");
        boolean knownName = IMAGES.stream().anyMatch(ext -> name.endsWith("." + ext));
        if (type.contains("svg") || name.endsWith(".svg") || (!knownType && !knownName)) {
            throw new CornerPubException("L'immagine deve essere jpg, png, webp o gif.");
        }
        try {
            return storageService.publicUrl(storageService.store("sponsor", String.valueOf(id), image));
        } catch (IOException e) {
            throw new CornerPubException("Non riesco a salvare l'immagine dello sponsor.");
        }
    }

    private String cleanName(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty() || value.length() > 80) {
            throw new CornerPubException("Inserisci il nome dello sponsor, al massimo 80 caratteri.");
        }
        return value;
    }

    private String normalizeSocial(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new CornerPubException("Inserisci il link del profilo social.");
        }
        if (value.startsWith("@")) {
            String handle = value.substring(1).replaceAll("[^A-Za-z0-9._]", "");
            if (handle.isBlank()) {
                throw new CornerPubException("Il profilo social deve essere un link valido.");
            }
            value = "https://instagram.com/" + handle;
        } else if (!value.regionMatches(true, 0, "http://", 0, 7)
                && !value.regionMatches(true, 0, "https://", 0, 8)) {
            value = "https://" + value.replaceFirst("^/+", "");
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || value.contains(" ") || value.length() > 300
                || !lower.matches("https?://[^\\s]+\\.[^\\s]+")) {
            throw new CornerPubException("Il profilo social deve essere un link valido.");
        }
        return value;
    }
}
