package ru.yandex.practicum.filmorate.storage.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.util.List;

@Component
public class EventDbStorage implements EventStorage {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Event> eventMapper = (rs, rowNum) -> {
        Event event = new Event();
        event.setEventId(rs.getInt("id"));
        event.setTimestamp(rs.getLong("timestamp"));
        event.setUserId(rs.getInt("user_id"));
        event.setEventType(rs.getString("event_type"));
        event.setOperation(rs.getString("operation"));
        event.setEntityId(rs.getInt("entity_id"));
        return event;
    };

    @Autowired
    public EventDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void addEvent(int userId, String eventType, String operation, int entityId) {
        String sql = """
                INSERT INTO events (timestamp, user_id, event_type, operation, entity_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, System.currentTimeMillis(), userId, eventType, operation, entityId);
    }

    @Override
    public List<Event> findByUserId(int userId) {
        String sql = """
                SELECT id, timestamp, user_id, event_type, operation, entity_id
                FROM events
                WHERE user_id = ?
                ORDER BY id
                """;
        return jdbcTemplate.query(sql, eventMapper, userId);
    }
}
