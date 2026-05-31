package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public List<Film> findPopular(int count) {
        return films.values().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .toList();
    }

    @Override
    public List<Film> findPopular(int count, Integer genreId, Integer year) {
        return findPopular(count);
    }

    @Override
    public List<Film> findCommon(int userId, int friendId) {
        return films.values().stream()
                .filter(film -> film.getLikes().contains(userId) && film.getLikes().contains(friendId))
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .toList();
    }

    @Override
    public List<Film> findByDirector(int directorId, String sortBy) {
        return films.values().stream()
                .filter(film -> film.getDirectors().stream().anyMatch(director -> director.getId() == directorId))
                .toList();
    }

    @Override
    public List<Film> search(String query, String by) {
        String lowerQuery = query.toLowerCase();
        return films.values().stream()
                .filter(film -> film.getName().toLowerCase().contains(lowerQuery)
                        || film.getDescription() != null && film.getDescription().toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .toList();
    }

    @Override
    public List<Film> findRecommendations(int userId) {
        return List.of();
    }

    @Override
    public Film findById(int id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Film with id=" + id + " not found");
        }
        return films.get(id);
    }

    @Override
    public Film create(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        findById(film.getId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public void delete(int id) {
        findById(id);
        films.remove(id);
    }

    @Override
    public void addLike(int id, int userId) {
        findById(id).getLikes().add(userId);
    }

    @Override
    public void deleteLike(int id, int userId) {
        findById(id).getLikes().remove(userId);
    }

    private int getNextId() {
        return nextId++;
    }
}
