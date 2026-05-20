package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User findById(int id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("User with id=" + id + " not found");
        }
        return users.get(id);
    }

    @Override
    public User create(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        findById(user.getId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public void delete(int id) {
        findById(id);
        users.remove(id);
    }

    private int getNextId() {
        return nextId++;
    }
}
