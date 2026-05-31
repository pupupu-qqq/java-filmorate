package ru.yandex.practicum.filmorate.storage.director;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Component
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DirectorDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Director> findAll() {
        String sql = "SELECT id, name FROM directors ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeDirector(rs.getInt("id"), rs.getString("name")));
    }

    @Override
    public Director findById(int id) {
        String sql = "SELECT id, name FROM directors WHERE id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeDirector(rs.getInt("id"), rs.getString("name")), id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Director with id=" + id + " not found"));
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM directors WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public Director create(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, director.getName());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new NotFoundException("Director id was not generated");
        }
        return findById(key.intValue());
    }

    @Override
    public Director update(Director director) {
        findById(director.getId());
        jdbcTemplate.update("UPDATE directors SET name = ? WHERE id = ?", director.getName(), director.getId());
        return findById(director.getId());
    }

    @Override
    public void delete(int id) {
        findById(id);
        jdbcTemplate.update("DELETE FROM directors WHERE id = ?", id);
    }

    private Director makeDirector(int id, String name) {
        Director director = new Director();
        director.setId(id);
        director.setName(name);
        return director;
    }
}
