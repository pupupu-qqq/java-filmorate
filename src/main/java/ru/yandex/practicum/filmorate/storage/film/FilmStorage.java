package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    List<Film> findAll();

    List<Film> findPopular(int count);

    List<Film> findPopular(int count, Integer genreId, Integer year);

    List<Film> findCommon(int userId, int friendId);

    List<Film> findByDirector(int directorId, String sortBy);

    List<Film> search(String query, String by);

    List<Film> findRecommendations(int userId);

    Film findById(int id);

    Film create(Film film);

    Film update(Film film);

    void delete(int id);

    void addLike(int id, int userId);

    void deleteLike(int id, int userId);
}
