// src/main/java/com/corner/pub/service/InEvidenzaService.java
package com.corner.pub.service;

import com.corner.pub.dto.request.InEvidenzaRequest;
import com.corner.pub.dto.response.InEvidenzaResponse;
import com.corner.pub.exception.resourcenotfound.MenuItemNotFoundException;
import com.corner.pub.model.InEvidenza;
import com.corner.pub.model.MenuItem;
import com.corner.pub.repository.InEvidenzaRepository;
import com.corner.pub.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InEvidenzaService {
    private final InEvidenzaRepository repo;
    private final MenuItemRepository menuRepo;

    public InEvidenzaService(InEvidenzaRepository repo, MenuItemRepository menuRepo) {
        this.repo = repo;
        this.menuRepo = menuRepo;
    }

    @Transactional(readOnly = true)
    public List<InEvidenzaResponse> listAll() {
        return repo.findAll().stream()
                .filter(e -> e.getProdotto() != null) // ⛑️ sicurezza extra
                .map(e -> {
                    InEvidenzaResponse r = new InEvidenzaResponse();
                    r.setId(e.getId());
                    r.setCategoria(e.getCategoria());

                    if (e.getProdotto() != null) {
                        r.setItemId(e.getProdotto().getId());
                        r.setTitolo(e.getProdotto().getTitolo());
                    } else {
                        r.setItemId(null);
                        r.setTitolo("PRODOTTO MANCANTE");
                    }

                    r.setCreatedAt(e.getCreatedAt());
                    return r;
                }).collect(Collectors.toList());
    }


    @Transactional
    public void add(InEvidenzaRequest req) {
        MenuItem mi = menuRepo.findById(req.getItemId())
                .orElseThrow(() -> new MenuItemNotFoundException(req.getItemId()));
        InEvidenza e = new InEvidenza();
        e.setCategoria(req.getCategoria());
        e.setProdotto(mi);
        repo.save(e);
    }

    @Transactional
    public void remove(Long id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("Highlight non trovato");
        repo.deleteById(id);
    }
}