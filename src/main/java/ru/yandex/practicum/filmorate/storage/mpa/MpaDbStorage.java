package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Component
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Mpa> findAll() {
        String sql = "SELECT id, name FROM mpa ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeMpa(rs.getInt("id"), rs.getString("name")));
    }

    @Override
    public Mpa findById(int id) {
        String sql = "SELECT id, name FROM mpa WHERE id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> makeMpa(rs.getInt("id"), rs.getString("name")), id).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Mpa with id=" + id + " not found"));
    }

    @Override
    public boolean existsById(int id) {
        String sql = "SELECT COUNT(*) FROM mpa WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    private Mpa makeMpa(int id, String name) {
        Mpa mpa = new Mpa();
        mpa.setId(id);
        mpa.setName(name);
        return mpa;
    }
}
