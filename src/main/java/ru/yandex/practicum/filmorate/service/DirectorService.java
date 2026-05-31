package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Service
public class DirectorService {
    private final DirectorStorage directorStorage;

    @Autowired
    public DirectorService(DirectorStorage directorStorage) {
        this.directorStorage = directorStorage;
    }

    public List<Director> findAll() {
        return directorStorage.findAll();
    }

    public Director findById(int id) {
        return directorStorage.findById(id);
    }

    public Director create(Director director) {
        validate(director);
        return directorStorage.create(director);
    }

    public Director update(Director director) {
        validate(director);
        return directorStorage.update(director);
    }

    public void delete(int id) {
        directorStorage.delete(id);
    }

    private void validate(Director director) {
        if (director == null || !StringUtils.hasText(director.getName())) {
            throw new ValidationException("Director name cannot be empty");
        }
    }
}
