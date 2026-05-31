package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        FilmDbStorage.class,
        UserDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class,
        ReviewDbStorage.class,
        EventDbStorage.class,
        DirectorDbStorage.class
})
class DbStorageTest {
    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;
    private final ReviewDbStorage reviewStorage;
    private final EventDbStorage eventStorage;
    private final DirectorDbStorage directorStorage;

    @Autowired
    DbStorageTest(
            FilmDbStorage filmStorage,
            UserDbStorage userStorage,
            GenreDbStorage genreStorage,
            MpaDbStorage mpaStorage,
            ReviewDbStorage reviewStorage,
            EventDbStorage eventStorage,
            DirectorDbStorage directorStorage
    ) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
        this.reviewStorage = reviewStorage;
        this.eventStorage = eventStorage;
        this.directorStorage = directorStorage;
    }

    @Test
    void shouldWorkWithUserStorage() {
        User firstUser = userStorage.create(makeUser("first@example.com", "first"));
        User secondUser = userStorage.create(makeUser("second@example.com", "second"));
        User commonFriend = userStorage.create(makeUser("friend@example.com", "friend"));

        assertThat(userStorage.findById(firstUser.getId()).getLogin()).isEqualTo("first");
        assertThat(userStorage.findAll()).hasSize(3);
        assertThat(userStorage.existsById(secondUser.getId())).isTrue();

        firstUser.setName("Updated");
        User updatedUser = userStorage.update(firstUser);
        assertThat(updatedUser.getName()).isEqualTo("Updated");

        userStorage.addFriend(firstUser.getId(), commonFriend.getId());
        userStorage.addFriend(secondUser.getId(), commonFriend.getId());
        assertThat(userStorage.findFriends(firstUser.getId()))
                .extracting(User::getId)
                .containsExactly(commonFriend.getId());
        assertThat(userStorage.findCommonFriends(firstUser.getId(), secondUser.getId()))
                .extracting(User::getId)
                .containsExactly(commonFriend.getId());

        userStorage.deleteFriend(firstUser.getId(), commonFriend.getId());
        assertThat(userStorage.findFriends(firstUser.getId())).isEmpty();

        userStorage.delete(secondUser.getId());
        assertThat(userStorage.existsById(secondUser.getId())).isFalse();
        assertThatThrownBy(() -> userStorage.findById(secondUser.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldWorkWithFilmStorage() {
        User user = userStorage.create(makeUser("user@example.com", "user"));
        User similarUser = userStorage.create(makeUser("similar@example.com", "similar"));
        Film firstFilm = filmStorage.create(makeFilm("Film", 1, Set.of(1, 2)));
        Film secondFilm = filmStorage.create(makeFilm("Popular", 2, Set.of(3)));
        Film thirdFilm = filmStorage.create(makeFilm("Searchable", 2, Set.of(3)));
        thirdFilm.setDescription("magic keyword");
        thirdFilm = filmStorage.update(thirdFilm);

        Film savedFilm = filmStorage.findById(firstFilm.getId());
        assertThat(savedFilm.getMpa().getName()).isEqualTo("G");
        assertThat(savedFilm.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Драма");
        assertThat(filmStorage.findAll()).hasSize(3);

        filmStorage.addLike(secondFilm.getId(), user.getId());
        assertThat(filmStorage.findPopular(1).getFirst().getId()).isEqualTo(secondFilm.getId());
        assertThat(filmStorage.search("magic", "description")).extracting(Film::getId).contains(thirdFilm.getId());

        filmStorage.addLike(firstFilm.getId(), user.getId());
        filmStorage.addLike(firstFilm.getId(), similarUser.getId());
        filmStorage.addLike(thirdFilm.getId(), similarUser.getId());
        assertThat(filmStorage.findRecommendations(user.getId())).extracting(Film::getId).contains(thirdFilm.getId());

        firstFilm.setName("Updated Film");
        firstFilm.setGenres(new LinkedHashSet<>(Set.of(makeGenre(6))));
        Film updatedFilm = filmStorage.update(firstFilm);
        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getGenres()).extracting(Genre::getId).containsExactly(6);

        filmStorage.deleteLike(secondFilm.getId(), user.getId());
        assertThat(filmStorage.findById(secondFilm.getId()).getLikes()).isEmpty();

        filmStorage.delete(firstFilm.getId());
        assertThatThrownBy(() -> filmStorage.findById(firstFilm.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldWorkWithReviewsAndEvents() {
        User user = userStorage.create(makeUser("reviewer@example.com", "reviewer"));
        User otherUser = userStorage.create(makeUser("other@example.com", "other"));
        Film film = filmStorage.create(makeFilm("Review film", 1, Set.of(1)));

        Review review = new Review();
        review.setContent("Good film");
        review.setIsPositive(true);
        review.setUserId(user.getId());
        review.setFilmId(film.getId());
        Review createdReview = reviewStorage.create(review);

        reviewStorage.addReaction(createdReview.getReviewId(), otherUser.getId(), 1);
        assertThat(reviewStorage.findById(createdReview.getReviewId()).getUseful()).isEqualTo(1);

        createdReview.setContent("Very good film");
        Review updatedReview = reviewStorage.update(createdReview);
        assertThat(updatedReview.getContent()).isEqualTo("Very good film");

        eventStorage.addEvent(user.getId(), "REVIEW", "ADD", createdReview.getReviewId());
        assertThat(eventStorage.findByUserId(user.getId()))
                .extracting(event -> event.getEventType() + event.getOperation())
                .containsExactly("REVIEWADD");

        reviewStorage.delete(createdReview.getReviewId());
        assertThatThrownBy(() -> reviewStorage.findById(createdReview.getReviewId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldWorkWithDirectorsAndCommonFilms() {
        User firstUser = userStorage.create(makeUser("common-first@example.com", "common-first"));
        User secondUser = userStorage.create(makeUser("common-second@example.com", "common-second"));
        Director director = directorStorage.create(makeDirector("Director"));
        Film film = makeFilm("Directed film", 1, Set.of(1));
        film.setDirectors(new LinkedHashSet<>(Set.of(director)));
        Film savedFilm = filmStorage.create(film);

        filmStorage.addLike(savedFilm.getId(), firstUser.getId());
        filmStorage.addLike(savedFilm.getId(), secondUser.getId());

        assertThat(filmStorage.findCommon(firstUser.getId(), secondUser.getId()))
                .extracting(Film::getId)
                .containsExactly(savedFilm.getId());
        assertThat(filmStorage.findPopular(10, 1, 2000))
                .extracting(Film::getId)
                .contains(savedFilm.getId());
        assertThat(filmStorage.findByDirector(director.getId(), "year"))
                .extracting(Film::getId)
                .containsExactly(savedFilm.getId());

        director.setName("Updated director");
        assertThat(directorStorage.update(director).getName()).isEqualTo("Updated director");
        directorStorage.delete(director.getId());
        assertThatThrownBy(() -> directorStorage.findById(director.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldWorkWithDictionaryStorages() {
        assertThat(mpaStorage.findAll()).hasSize(5);
        assertThat(mpaStorage.findById(3).getName()).isEqualTo("PG-13");
        assertThat(mpaStorage.existsById(5)).isTrue();
        assertThatThrownBy(() -> mpaStorage.findById(99)).isInstanceOf(NotFoundException.class);

        assertThat(genreStorage.findAll()).hasSize(6);
        assertThat(genreStorage.findById(1).getName()).isEqualTo("Комедия");
        assertThat(genreStorage.existsById(6)).isTrue();
        assertThatThrownBy(() -> genreStorage.findById(99)).isInstanceOf(NotFoundException.class);
    }

    private User makeUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private Film makeFilm(String name, int mpaId, Set<Integer> genreIds) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        Mpa mpa = new Mpa();
        mpa.setId(mpaId);
        film.setMpa(mpa);

        Set<Genre> genres = new LinkedHashSet<>();
        for (Integer genreId : genreIds) {
            genres.add(makeGenre(genreId));
        }
        film.setGenres(genres);
        return film;
    }

    private Genre makeGenre(int id) {
        Genre genre = new Genre();
        genre.setId(id);
        return genre;
    }

    private Director makeDirector(String name) {
        Director director = new Director();
        director.setName(name);
        return director;
    }
}
