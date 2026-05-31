package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilmControllerTest {
    private final UserStorage userStorage = new InMemoryUserStorage();
    private final FilmController controller = new FilmController(
            new FilmService(new InMemoryFilmStorage(), userStorage, makeEventStorage(), makeDirectorStorage())
    );

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

    @Test
    void shouldReturnPopularFilmsByLikes() {
        User user = userStorage.create(makeValidUser());
        Film firstFilm = controller.create(makeValidFilm());
        Film secondFilm = makeValidFilm();
        secondFilm.setName("Popular film");
        secondFilm = controller.create(secondFilm);

        controller.addLike(secondFilm.getId(), user.getId());

        assertEquals(secondFilm.getId(), controller.findPopular(10).getFirst().getId());
    }

    private Film makeValidFilm() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        return film;
    }

    private User makeValidUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setName("User");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private EventStorage makeEventStorage() {
        return new EventStorage() {
            @Override
            public void addEvent(int userId, String eventType, String operation, int entityId) {
            }

            @Override
            public List<Event> findByUserId(int userId) {
                return List.of();
            }
        };
    }

    private DirectorStorage makeDirectorStorage() {
        return new DirectorStorage() {
            @Override
            public List<Director> findAll() {
                return List.of();
            }

            @Override
            public Director findById(int id) {
                Director director = new Director();
                director.setId(id);
                return director;
            }

            @Override
            public boolean existsById(int id) {
                return true;
            }

            @Override
            public Director create(Director director) {
                return director;
            }

            @Override
            public Director update(Director director) {
                return director;
            }

            @Override
            public void delete(int id) {
            }
        };
    }
}
