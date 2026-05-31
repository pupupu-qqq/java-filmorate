package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component("filmDbStorage")
public class FilmDbStorage implements FilmStorage {
    private static final String FILM_SELECT = """
            SELECT f.id,
                   f.name,
                   f.description,
                   f.release_date,
                   f.duration,
                   m.id AS mpa_id,
                   m.name AS mpa_name
            FROM films AS f
            LEFT JOIN mpa AS m ON f.mpa_id = m.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Film> filmMapper = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            Mpa mpa = new Mpa();
            mpa.setId(mpaId);
            mpa.setName(rs.getString("mpa_name"));
            film.setMpa(mpa);
        }

        film.setGenres(findGenresByFilmId(film.getId()));
        film.setDirectors(findDirectorsByFilmId(film.getId()));
        film.setLikes(findLikesByFilmId(film.getId()));
        return film;
    };

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Film> findAll() {
        return jdbcTemplate.query(FILM_SELECT + " ORDER BY f.id", filmMapper);
    }

    @Override
    public List<Film> findPopular(int count) {
        String sql = FILM_SELECT + """
                LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                ORDER BY COUNT(fl.user_id) DESC, f.id
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, filmMapper, count);
    }

    @Override
    public List<Film> findPopular(int count, Integer genreId, Integer year) {
        String sql = FILM_SELECT + """
                LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                LEFT JOIN film_genres AS fg ON f.id = fg.film_id
                WHERE (? IS NULL OR fg.genre_id = ?)
                  AND (? IS NULL OR EXTRACT(YEAR FROM f.release_date) = ?)
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                ORDER BY COUNT(DISTINCT fl.user_id) DESC, f.id
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, filmMapper, genreId, genreId, year, year, count);
    }

    @Override
    public List<Film> findCommon(int userId, int friendId) {
        String sql = FILM_SELECT + """
                JOIN film_likes AS user_likes ON f.id = user_likes.film_id
                JOIN film_likes AS friend_likes ON f.id = friend_likes.film_id
                LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                WHERE user_likes.user_id = ? AND friend_likes.user_id = ?
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                ORDER BY COUNT(DISTINCT fl.user_id) DESC, f.id
                """;
        return jdbcTemplate.query(sql, filmMapper, userId, friendId);
    }

    @Override
    public List<Film> findByDirector(int directorId, String sortBy) {
        String orderBy = "year".equals(sortBy) ? "f.release_date, f.id" : "COUNT(fl.user_id) DESC, f.id";
        String sql = FILM_SELECT + """
                JOIN film_directors AS fd ON f.id = fd.film_id
                LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                WHERE fd.director_id = ?
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                ORDER BY
                """ + orderBy;
        return jdbcTemplate.query(sql, filmMapper, directorId);
    }

    @Override
    public List<Film> search(String query, String by) {
        String searchBy = by == null ? null : by.toLowerCase();
        boolean searchByTitle = searchBy == null || searchBy.contains("title") || searchBy.contains("name");
        boolean searchByDescription = searchBy == null || searchBy.contains("description");
        String likeQuery = "%" + query.toLowerCase() + "%";

        if (searchByTitle && searchByDescription) {
            return jdbcTemplate.query(FILM_SELECT + """
                    LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                    WHERE LOWER(f.name) LIKE ? OR LOWER(f.description) LIKE ?
                    GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                    ORDER BY COUNT(fl.user_id) DESC, f.id
                    """, filmMapper, likeQuery, likeQuery);
        }
        if (searchByDescription) {
            return jdbcTemplate.query(FILM_SELECT + """
                    LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                    WHERE LOWER(f.description) LIKE ?
                    GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                    ORDER BY COUNT(fl.user_id) DESC, f.id
                    """, filmMapper, likeQuery);
        }
        return jdbcTemplate.query(FILM_SELECT + """
                LEFT JOIN film_likes AS fl ON f.id = fl.film_id
                WHERE LOWER(f.name) LIKE ?
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                ORDER BY COUNT(fl.user_id) DESC, f.id
                """, filmMapper, likeQuery);
    }

    @Override
    public List<Film> findRecommendations(int userId) {
        String sql = FILM_SELECT + """
                JOIN film_likes AS recommended ON f.id = recommended.film_id
                JOIN (
                    SELECT fl.user_id, COUNT(*) AS common_likes
                    FROM film_likes AS fl
                    JOIN film_likes AS user_likes ON fl.film_id = user_likes.film_id
                    WHERE user_likes.user_id = ? AND fl.user_id <> ?
                    GROUP BY fl.user_id
                ) AS similar_users ON recommended.user_id = similar_users.user_id
                WHERE f.id NOT IN (SELECT film_id FROM film_likes WHERE user_id = ?)
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, m.id, m.name
                ORDER BY MAX(similar_users.common_likes) DESC, f.id
                """;
        return jdbcTemplate.query(sql, filmMapper, userId, userId, userId);
    }

    @Override
    public Film findById(int id) {
        return jdbcTemplate.query(FILM_SELECT + " WHERE f.id = ?", filmMapper, id).stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Film with id=" + id + " not found"));
    }

    @Override
    public Film create(Film film) {
        checkFilmReferences(film);
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            statement.setObject(5, getMpaId(film));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new NotFoundException("Film id was not generated");
        }

        film.setId(key.intValue());
        saveFilmGenres(film);
        saveFilmDirectors(film);
        return findById(film.getId());
    }

    @Override
    public Film update(Film film) {
        findById(film.getId());
        checkFilmReferences(film);
        String sql = """
                UPDATE films
                SET name = ?,
                    description = ?,
                    release_date = ?,
                    duration = ?,
                    mpa_id = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(
                sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                getMpaId(film),
                film.getId()
        );
        saveFilmGenres(film);
        saveFilmDirectors(film);
        return findById(film.getId());
    }

    @Override
    public void delete(int id) {
        findById(id);
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public void addLike(int id, int userId) {
        findById(id);
        String sql = "MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, id, userId);
    }

    @Override
    public void deleteLike(int id, int userId) {
        findById(id);
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", id, userId);
    }

    private void checkFilmReferences(Film film) {
        Integer mpaId = getMpaId(film);
        if (mpaId != null && !existsMpaById(mpaId)) {
            throw new NotFoundException("Mpa with id=" + mpaId + " not found");
        }
        for (Integer genreId : getGenreIds(film)) {
            if (!existsGenreById(genreId)) {
                throw new NotFoundException("Genre with id=" + genreId + " not found");
            }
        }
        for (Integer directorId : getDirectorIds(film)) {
            if (!existsDirectorById(directorId)) {
                throw new NotFoundException("Director with id=" + directorId + " not found");
            }
        }
    }

    private Integer getMpaId(Film film) {
        if (film.getMpa() == null) {
            return null;
        }
        return film.getMpa().getId();
    }

    private Set<Integer> getGenreIds(Film film) {
        Set<Integer> genreIds = new LinkedHashSet<>();
        if (film.getGenres() == null) {
            return genreIds;
        }
        for (Genre genre : film.getGenres()) {
            genreIds.add(genre.getId());
        }
        return genreIds;
    }

    private Set<Integer> getDirectorIds(Film film) {
        Set<Integer> directorIds = new LinkedHashSet<>();
        if (film.getDirectors() == null) {
            return directorIds;
        }
        for (Director director : film.getDirectors()) {
            directorIds.add(director.getId());
        }
        return directorIds;
    }

    private boolean existsMpaById(int id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mpa WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private boolean existsGenreById(int id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM genres WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private boolean existsDirectorById(int id) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM directors WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private void saveFilmGenres(Film film) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        Set<Integer> genreIds = getGenreIds(film);
        if (genreIds.isEmpty()) {
            return;
        }

        List<Object[]> batchArgs = genreIds.stream()
                .map(genreId -> new Object[]{film.getId(), genreId})
                .toList();
        jdbcTemplate.batchUpdate("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)", batchArgs);
    }

    private void saveFilmDirectors(Film film) {
        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        Set<Integer> directorIds = getDirectorIds(film);
        if (directorIds.isEmpty()) {
            return;
        }

        List<Object[]> batchArgs = directorIds.stream()
                .map(directorId -> new Object[]{film.getId(), directorId})
                .toList();
        jdbcTemplate.batchUpdate("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", batchArgs);
    }

    private Set<Genre> findGenresByFilmId(int filmId) {
        String sql = """
                SELECT g.id, g.name
                FROM genres AS g
                JOIN film_genres AS fg ON g.id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.id
                """;
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId));
    }

    private Set<Integer> findLikesByFilmId(int filmId) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ? ORDER BY user_id";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("user_id"), filmId));
    }

    private Set<Director> findDirectorsByFilmId(int filmId) {
        String sql = """
                SELECT d.id, d.name
                FROM directors AS d
                JOIN film_directors AS fd ON d.id = fd.director_id
                WHERE fd.film_id = ?
                ORDER BY d.id
                """;
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> {
            Director director = new Director();
            director.setId(rs.getInt("id"));
            director.setName(rs.getString("name"));
            return director;
        }, filmId));
    }
}
