package com.corner.pub.config;

import com.corner.pub.model.Event;
import com.corner.pub.model.MenuItem;
import com.corner.pub.repository.EventRepository;
import com.corner.pub.repository.MenuItemRepository;
import com.corner.pub.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class GenerateMenuThumbs implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GenerateMenuThumbs.class);

    private final MenuItemRepository menuItemRepository;
    private final EventRepository eventRepository;
    private final StorageService storageService;

    public GenerateMenuThumbs(MenuItemRepository menuItemRepository,
                              EventRepository eventRepository,
                              StorageService storageService) {
        this.menuItemRepository = menuItemRepository;
        this.eventRepository = eventRepository;
        this.storageService = storageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int ok = 0;
        int missing = 0;
        for (MenuItem item : menuItemRepository.findAll()) {
            try {
                if (storageService.ensureThumb(item.getImageUrl()) != null) {
                    ok++;
                } else if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) {
                    missing++;
                }
            } catch (Exception e) {
                missing++;
                log.warn("Thumb piatto {}: {}", item.getId(), e.getMessage());
            }
        }
        for (Event event : eventRepository.findAll()) {
            try {
                storageService.ensureThumb(event.getPosterUrl());
            } catch (Exception e) {
                log.warn("Thumb evento {}: {}", event.getId(), e.getMessage());
            }
        }
        log.info("Miniature menu pronte: {} ok, {} file originali assenti", ok, missing);
    }
}
