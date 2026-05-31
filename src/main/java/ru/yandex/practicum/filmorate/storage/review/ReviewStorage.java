package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage {
    Review findById(int id);

    List<Review> findByFilmId(Integer filmId, int count);

    Review create(Review review);

    Review update(Review review);

    void delete(int id);

    void addReaction(int reviewId, int userId, int value);

    void deleteReaction(int reviewId, int userId, int value);
}
