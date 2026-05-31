package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate FIRST_FILM_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final EventStorage eventStorage;
    private final DirectorStorage directorStorage;

    @Autowired
    public FilmService(
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            EventStorage eventStorage,
            DirectorStorage directorStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.eventStorage = eventStorage;
        this.directorStorage = directorStorage;
    }

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.eventStorage = null;
        this.directorStorage = null;
    }

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(int id) {
        return filmStorage.findById(id);
    }

    public Film create(Film film) {
        validate(film);
        Film createdFilm = filmStorage.create(film);
        log.info("Created film: {}", createdFilm);
        return createdFilm;
    }

    public Film update(Film film) {
        validate(film);
        Film savedFilm = filmStorage.findById(film.getId());
        film.setLikes(savedFilm.getLikes());
        Film updatedFilm = filmStorage.update(film);
        log.info("Updated film: {}", updatedFilm);
        return updatedFilm;
    }

    public void delete(int id) {
        filmStorage.delete(id);
        log.info("Deleted film with id={}", id);
    }

    public void addLike(int id, int userId) {
        filmStorage.findById(id);
        checkUserExists(userId);
        filmStorage.addLike(id, userId);
        addEvent(userId, "LIKE", "ADD", id);
        log.info("User with id={} liked film with id={}", userId, id);
    }

    public void deleteLike(int id, int userId) {
        filmStorage.findById(id);
        checkUserExists(userId);
        filmStorage.deleteLike(id, userId);
        addEvent(userId, "LIKE", "REMOVE", id);
        log.info("User with id={} deleted like from film with id={}", userId, id);
    }

    public List<Film> findPopular(int count) {
        if (count < 0) {
            log.warn("Popular films count cannot be negative");
            throw new ValidationException("Popular films count cannot be negative");
        }
        return filmStorage.findPopular(count);
    }

    public List<Film> findPopular(int count, Integer genreId, Integer year) {
        if (count < 0) {
            log.warn("Popular films count cannot be negative");
            throw new ValidationException("Popular films count cannot be negative");
        }
        if (genreId == null && year == null) {
            return filmStorage.findPopular(count);
        }
        return filmStorage.findPopular(count, genreId, year);
    }

    public List<Film> findCommon(int userId, int friendId) {
        checkUserExists(userId);
        checkUserExists(friendId);
        return filmStorage.findCommon(userId, friendId);
    }

    public List<Film> findByDirector(int directorId, String sortBy) {
        if (directorStorage != null && !directorStorage.existsById(directorId)) {
            throw new NotFoundException("Director with id=" + directorId + " not found");
        }
        return filmStorage.findByDirector(directorId, sortBy);
    }

    public List<Film> search(String query, String by) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return filmStorage.search(query, by);
    }

    private void addEvent(int userId, String eventType, String operation, int entityId) {
        if (eventStorage != null) {
            eventStorage.addEvent(userId, eventType, operation, entityId);
        }
    }

    private void checkUserExists(int userId) {
        if (!userStorage.existsById(userId)) {
            log.warn("User with id={} not found", userId);
            throw new NotFoundException("User with id=" + userId + " not found");
        }
    }

    private void validate(Film film) {
        if (film == null) {
            log.warn("Film is not passed");
            throw new ValidationException("Film is not passed");
        }
        if (!StringUtils.hasText(film.getName())) {
            log.warn("Film name cannot be empty");
            throw new ValidationException("Film name cannot be empty");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Film description is longer than {} characters", MAX_DESCRIPTION_LENGTH);
            throw new ValidationException("Film description cannot be longer than 200 characters");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FIRST_FILM_RELEASE_DATE)) {
            log.warn("Film release date is before {}", FIRST_FILM_RELEASE_DATE);
            throw new ValidationException("Film release date cannot be before 1895-12-28");
        }
        if (film.getDuration() <= 0) {
            log.warn("Film duration must be positive");
            throw new ValidationException("Film duration must be positive");
        }
    }
}
