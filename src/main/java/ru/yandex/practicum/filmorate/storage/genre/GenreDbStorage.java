package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Component
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Genre> findAll() {
        String sql = "SELECT id, name FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeGenre(rs.getInt("id"), rs.getString("name")));
    }

    @Override
    public Genre findById(int id) {
        String sql = "SELECT id, name FROM genres WHERE id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeGenre(rs.getInt("id"), rs.getString("name")), id).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Genre with id=" + id + " not found"));
    }

    @Override
    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM genres WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private Genre makeGenre(int id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }
}
