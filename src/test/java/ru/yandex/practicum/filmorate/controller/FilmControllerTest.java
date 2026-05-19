package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {
    private final FilmController controller = new FilmController();

    @Test
    void shouldCreateFilmWithValidData() {
        Film film = makeValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDescription("a".repeat(200));

        Film createdFilm = controller.create(film);

        assertEquals(1, createdFilm.getId());
    }

    @Test
    void shouldRejectFilmWithEmptyName() {
        Film film = makeValidFilm();
        film.setName("");

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldRejectFilmWithTooLongDescription() {
        Film film = makeValidFilm();
        film.setDescription("a".repeat(201));

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldRejectFilmReleasedBeforeCinemaBirthday() {
        Film film = makeValidFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldRejectFilmWithNotPositiveDuration() {
        Film film = makeValidFilm();
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldRejectEmptyFilmRequest() {
        assertThrows(ValidationException.class, () -> controller.create(null));
    }

    private Film makeValidFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        return film;
    }
}
