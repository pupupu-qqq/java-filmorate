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
    public List<User> findFriends(int id) {
        User user = findById(id);
        return users.values().stream()
                .filter(friend -> user.getFriends().contains(friend.getId()))
                .toList();
    }

    @Override
    public List<User> findCommonFriends(int id, int otherId) {
        User user = findById(id);
        User otherUser = findById(otherId);
        return users.values().stream()
                .filter(friend -> user.getFriends().contains(friend.getId()))
                .filter(friend -> otherUser.getFriends().contains(friend.getId()))
                .toList();
    }

    @Override
    public User findById(int id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("User with id=" + id + " not found");
        }
        return users.get(id);
    }

    @Override
    public boolean existsById(int id) {
        return users.containsKey(id);
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

    @Override
    public void addFriend(int id, int friendId) {
        findById(id).getFriends().add(friendId);
    }

    @Override
    public void deleteFriend(int id, int friendId) {
        findById(id).getFriends().remove(friendId);
    }

    private int getNextId() {
        return nextId++;
    }
}
