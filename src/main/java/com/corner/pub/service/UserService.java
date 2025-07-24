package com.corner.pub.service;

import com.corner.pub.exception.resourcenotfound.UserNotFoundException;
import com.corner.pub.model.User;
import com.corner.pub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("ID utente non trovato: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * Trova un utente per numero di telefono,
     * oppure lancia UserNotFoundException se non esiste.
     */
    public User findByPhoneOrThrow(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException(phone));
    }

    /**
     * Trova un utente per telefono o lo crea se non esiste.
     */
    public User findOrCreate(String name, String phone) {
        return userRepository.findByPhone(phone)
                .orElseGet(() -> {
                    User u = new User();
                    u.setName(name);
                    u.setPhone(phone);
                    return userRepository.save(u);
                });
    }

    // (eventualmente puoi tenere anche il solo findByPhone se ti serve altrove)
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }
}
