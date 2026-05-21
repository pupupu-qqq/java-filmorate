package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component("userDbStorage")
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<User> userMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        user.setFriends(findFriendIdsByUserId(user.getId()));
        return user;
    };

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, email, login, name, birthday FROM users ORDER BY id";
        return jdbcTemplate.query(sql, userMapper);
    }

    @Override
    public List<User> findFriends(int id) {
        findById(id);
        String sql = """
                SELECT u.id, u.email, u.login, u.name, u.birthday
                FROM users AS u
                JOIN user_friends AS uf ON u.id = uf.friend_id
                WHERE uf.user_id = ?
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, userMapper, id);
    }

    @Override
    public List<User> findCommonFriends(int id, int otherId) {
        findById(id);
        findById(otherId);
        String sql = """
                SELECT u.id, u.email, u.login, u.name, u.birthday
                FROM users AS u
                JOIN user_friends AS uf ON u.id = uf.friend_id
                JOIN user_friends AS other_uf ON u.id = other_uf.friend_id
                WHERE uf.user_id = ? AND other_uf.user_id = ?
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, userMapper, id, otherId);
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, userMapper, id).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("User with id=" + id + " not found"));
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setDate(4, Date.valueOf(user.getBirthday()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new NotFoundException("User id was not generated");
        }

        user.setId(key.intValue());
        return findById(user.getId());
    }

    @Override
    public User update(User user) {
        findById(user.getId());
        String sql = """
                UPDATE users
                SET email = ?,
                    login = ?,
                    name = ?,
                    birthday = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(
                sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );
        return findById(user.getId());
    }

    @Override
    public void delete(int id) {
        findById(id);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public void addFriend(int id, int friendId) {
        findById(id);
        findById(friendId);
        String sql = "MERGE INTO user_friends (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, id, friendId);
    }

    @Override
    public void deleteFriend(int id, int friendId) {
        findById(id);
        findById(friendId);
        jdbcTemplate.update("DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?", id, friendId);
    }

    private Set<Integer> findFriendIdsByUserId(int id) {
        String sql = "SELECT friend_id FROM user_friends WHERE user_id = ? ORDER BY friend_id";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("friend_id"), id));
    }
}
