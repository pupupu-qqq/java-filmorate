package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(int id) {
        return userStorage.findById(id);
    }

    public User create(User user) {
        validate(user);
        setDefaultName(user);
        User createdUser = userStorage.create(user);
        log.info("Created user: {}", createdUser);
        return createdUser;
    }

    public User update(User user) {
        validate(user);
        setDefaultName(user);
        User savedUser = userStorage.findById(user.getId());
        user.setFriends(savedUser.getFriends());
        User updatedUser = userStorage.update(user);
        log.info("Updated user: {}", updatedUser);
        return updatedUser;
    }

    public void addFriend(int id, int friendId) {
        User user = userStorage.findById(id);
        User friend = userStorage.findById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(id);
        userStorage.update(user);
        userStorage.update(friend);
        log.info("Users with id={} and id={} are friends", id, friendId);
    }

    public void deleteFriend(int id, int friendId) {
        User user = userStorage.findById(id);
        User friend = userStorage.findById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
        userStorage.update(user);
        userStorage.update(friend);
        log.info("Users with id={} and id={} are not friends anymore", id, friendId);
    }

    public List<User> findFriends(int id) {
        return userStorage.findFriends(id);
    }

    public List<User> findCommonFriends(int id, int otherId) {
        return userStorage.findCommonFriends(id, otherId);
    }

    private void validate(User user) {
        if (user == null) {
            log.warn("User is not passed");
            throw new ValidationException("User is not passed");
        }
        if (!StringUtils.hasText(user.getEmail()) || !user.getEmail().contains("@")) {
            log.warn("Incorrect email");
            throw new ValidationException("Email cannot be empty and must contain @");
        }
        if (!StringUtils.hasText(user.getLogin()) || user.getLogin().matches(".*\\s.*")) {
            log.warn("Incorrect user login");
            throw new ValidationException("Login cannot be empty or contain spaces");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("User birthday cannot be in the future");
            throw new ValidationException("Birthday cannot be in the future");
        }
    }

    private void setDefaultName(User user) {
        if (!StringUtils.hasText(user.getName())) {
            user.setName(user.getLogin());
        }
    }
}
