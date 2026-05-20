package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class FilmService {
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate FIRST_FILM_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
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

    public void addLike(int id, int userId) {
        Film film = filmStorage.findById(id);
        userStorage.findById(userId);
        film.getLikes().add(userId);
        filmStorage.update(film);
        log.info("User with id={} liked film with id={}", userId, id);
    }

    public void deleteLike(int id, int userId) {
        Film film = filmStorage.findById(id);
        userStorage.findById(userId);
        film.getLikes().remove(userId);
        filmStorage.update(film);
        log.info("User with id={} deleted like from film with id={}", userId, id);
    }

    public List<Film> findPopular(int count) {
        if (count < 0) {
            log.warn("Popular films count cannot be negative");
            throw new ValidationException("Popular films count cannot be negative");
        }
        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .toList();
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
