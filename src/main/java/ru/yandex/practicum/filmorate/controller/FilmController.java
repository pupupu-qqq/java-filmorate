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
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate FIRST_FILM_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private int nextId = 1;

    @GetMapping
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        validate(film);
        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Created film: {}", film);
        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film film) {
        validate(film);
        if (!films.containsKey(film.getId())) {
            log.warn("Film with id={} not found", film.getId());
            throw new NotFoundException("Film with id=" + film.getId() + " not found");
        }
        films.put(film.getId(), film);
        log.info("Updated film: {}", film);
        return film;
    }

    private void validate(Film film) {
        if (film == null) {
            log.warn("Film is not passed");
            throw new ValidationException("Film is not passed");
        }
        if (film.getName() == null || film.getName().isBlank()) {
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

    private int getNextId() {
        return nextId++;
    }
}
