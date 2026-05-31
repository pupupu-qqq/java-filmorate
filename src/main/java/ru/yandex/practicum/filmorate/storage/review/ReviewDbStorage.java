package ru.yandex.practicum.filmorate.storage.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Component
public class ReviewDbStorage implements ReviewStorage {
    private static final String REVIEW_SELECT = """
            SELECT r.id,
                   r.content,
                   r.is_positive,
                   r.user_id,
                   r.film_id,
                   COALESCE(SUM(rr.reaction), 0) AS useful
            FROM reviews AS r
            LEFT JOIN review_reactions AS rr ON r.id = rr.review_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Review> reviewMapper = (rs, rowNum) -> {
        Review review = new Review();
        review.setReviewId(rs.getInt("id"));
        review.setContent(rs.getString("content"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getInt("user_id"));
        review.setFilmId(rs.getInt("film_id"));
        review.setUseful(rs.getInt("useful"));
        return review;
    };

    @Autowired
    public ReviewDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Review findById(int id) {
        String sql = REVIEW_SELECT + """
                WHERE r.id = ?
                GROUP BY r.id, r.content, r.is_positive, r.user_id, r.film_id
                """;
        return jdbcTemplate.query(sql, reviewMapper, id).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Review with id=" + id + " not found"));
    }

    @Override
    public List<Review> findByFilmId(Integer filmId, int count) {
        if (filmId == null) {
            return jdbcTemplate.query(REVIEW_SELECT + """
                    GROUP BY r.id, r.content, r.is_positive, r.user_id, r.film_id
                    ORDER BY useful DESC, r.id
                    LIMIT ?
                    """, reviewMapper, count);
        }
        return jdbcTemplate.query(REVIEW_SELECT + """
                WHERE r.film_id = ?
                GROUP BY r.id, r.content, r.is_positive, r.user_id, r.film_id
                ORDER BY useful DESC, r.id
                LIMIT ?
                """, reviewMapper, filmId, count);
    }

    @Override
    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, review.getContent());
            statement.setBoolean(2, review.getIsPositive());
            statement.setInt(3, review.getUserId());
            statement.setInt(4, review.getFilmId());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new NotFoundException("Review id was not generated");
        }

        return findById(key.intValue());
    }

    @Override
    public Review update(Review review) {
        findById(review.getReviewId());
        String sql = """
                UPDATE reviews
                SET content = ?,
                    is_positive = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, review.getContent(), review.getIsPositive(), review.getReviewId());
        return findById(review.getReviewId());
    }

    @Override
    public void delete(int id) {
        findById(id);
        jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", id);
    }

    @Override
    public void addReaction(int reviewId, int userId, int value) {
        findById(reviewId);
        String sql = """
                MERGE INTO review_reactions (review_id, user_id, reaction)
                KEY (review_id, user_id)
                VALUES (?, ?, ?)
                """;
        jdbcTemplate.update(sql, reviewId, userId, value);
    }

    @Override
    public void deleteReaction(int reviewId, int userId, int value) {
        findById(reviewId);
        String sql = "DELETE FROM review_reactions WHERE review_id = ? AND user_id = ? AND reaction = ?";
        jdbcTemplate.update(sql, reviewId, userId, value);
    }
}
