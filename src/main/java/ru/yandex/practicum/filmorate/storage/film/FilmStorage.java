package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    List<Film> findAll();

    List<Film> findPopular(int count);

    Film findById(int id);

    Film create(Film film);

    Film update(Film film);

    void delete(int id);
}
