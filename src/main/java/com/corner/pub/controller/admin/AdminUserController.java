package com.corner.pub.controller.admin;

import com.corner.pub.dto.request.UserRequest;
import com.corner.pub.dto.response.UserResponse;
import com.corner.pub.model.User;
import com.corner.pub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller per la gestione degli utenti (lead, prenotazioni).
 */
@RestController
@RequestMapping("/api/users")
public class AdminUserController {

    private final UserService userService;

    @Autowired
    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Recupera un utente per numero di telefono.
     * @param phone numero di telefono
     * @return UserResponse con id, name e phone
     */
    @GetMapping("/phone/{phone}")
    public ResponseEntity<UserResponse> getByPhone(@PathVariable String phone) {
        User user = userService.findByPhoneOrThrow(phone);
        UserResponse resp = toResponse(user);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Crea un nuovo utente, o lo restituisce se esiste già.
     * @param req UserRequest con name e phone
     * @return UserResponse
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest req) {
        User u = userService.findOrCreate(req.getName(), req.getPhone());
        UserResponse dto = toResponse(u);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Restituisce tutti gli utenti presenti nel sistema.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(users);
    }


    // Utility di mapping entity → DTO
    private UserResponse toResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setName(user.getName());
        r.setPhone(user.getPhone());
        return r;
    }
}
