package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventOperation;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final EventStorage eventStorage;

    @Autowired
    public UserService(
            @Qualifier("userDbStorage") UserStorage userStorage,
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            EventStorage eventStorage
    ) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
        this.eventStorage = eventStorage;
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

    public void delete(int id) {
        userStorage.delete(id);
        log.info("Deleted user with id={}", id);
    }

    public void addFriend(int id, int friendId) {
        userStorage.addFriend(id, friendId);
        addEvent(id, EventType.FRIEND, EventOperation.ADD, friendId);
        log.info("User with id={} added user with id={} to friends", id, friendId);
    }

    public void deleteFriend(int id, int friendId) {
        userStorage.deleteFriend(id, friendId);
        addEvent(id, EventType.FRIEND, EventOperation.REMOVE, friendId);
        log.info("User with id={} deleted user with id={} from friends", id, friendId);
    }

    public List<User> findFriends(int id) {
        return userStorage.findFriends(id);
    }

    public List<User> findCommonFriends(int id, int otherId) {
        return userStorage.findCommonFriends(id, otherId);
    }

    public List<Film> findRecommendations(int id) {
        checkUserExists(id);
        return filmStorage.findRecommendations(id);
    }

    public List<Event> findFeed(int id) {
        checkUserExists(id);
        return eventStorage.findByUserId(id);
    }

    private void addEvent(int userId, EventType eventType, EventOperation operation, int entityId) {
        eventStorage.addEvent(userId, eventType.name(), operation.name(), entityId);
    }

    private void checkUserExists(int userId) {
        if (!userStorage.existsById(userId)) {
            log.warn("User with id={} not found", userId);
            throw new NotFoundException("User with id=" + userId + " not found");
        }
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
