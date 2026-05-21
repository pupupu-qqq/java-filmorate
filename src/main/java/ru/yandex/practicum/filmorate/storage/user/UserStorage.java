package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {
    List<User> findAll();

    List<User> findFriends(int id);

    List<User> findCommonFriends(int id, int otherId);

    User findById(int id);

    boolean existsById(int id);

    User create(User user);

    User update(User user);

    void delete(int id);

    void addFriend(int id, int friendId);

    void deleteFriend(int id, int friendId);
}
