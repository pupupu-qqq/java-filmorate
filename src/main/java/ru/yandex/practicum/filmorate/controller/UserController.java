package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private int nextId = 1;

    @GetMapping
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validate(user);
        setDefaultName(user);
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Created user: {}", user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User user) {
        validate(user);
        if (!users.containsKey(user.getId())) {
            log.warn("User with id={} not found", user.getId());
            throw new NotFoundException("User with id=" + user.getId() + " not found");
        }
        setDefaultName(user);
        users.put(user.getId(), user);
        log.info("Updated user: {}", user);
        return user;
    }

    private void validate(User user) {
        if (user == null) {
            log.warn("User is not passed");
            throw new ValidationException("User is not passed");
        }
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Incorrect email");
            throw new ValidationException("Email cannot be empty and must contain @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().matches(".*\\s.*")) {
            log.warn("Incorrect user login");
            throw new ValidationException("Login cannot be empty or contain spaces");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("User birthday cannot be in the future");
            throw new ValidationException("Birthday cannot be in the future");
        }
    }

    private void setDefaultName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    private int getNextId() {
        return nextId++;
    }
}
