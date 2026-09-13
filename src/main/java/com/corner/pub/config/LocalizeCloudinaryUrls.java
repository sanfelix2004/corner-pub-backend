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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LocalizeCloudinaryUrls implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalizeCloudinaryUrls.class);

    private final MenuItemRepository menuItemRepository;
    private final EventRepository eventRepository;

    public LocalizeCloudinaryUrls(MenuItemRepository menuItemRepository, EventRepository eventRepository) {
        this.menuItemRepository = menuItemRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int dishes = 0;
        for (MenuItem item : menuItemRepository.findAll()) {
            String next = StorageService.toLocalUrl(item.getImageUrl());
            if (next != null && !next.equals(item.getImageUrl())) {
                item.setImageUrl(next);
                dishes++;
            }
        }

        int events = 0;
        for (Event event : eventRepository.findAll()) {
            String next = StorageService.toLocalUrl(event.getPosterUrl());
            if (next != null && !next.equals(event.getPosterUrl())) {
                event.setPosterUrl(next);
                if (next.startsWith("/uploads/")) {
                    event.setPosterPublicId(next.substring("/uploads/".length()));
                }
                events++;
            }
        }

        if (dishes > 0 || events > 0) {
            log.info("URL foto convertiti su Aruba: {} piatti, {} eventi", dishes, events);
        }
    }
}
