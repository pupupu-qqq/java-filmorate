package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@Service
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final EventStorage eventStorage;

    @Autowired
    public ReviewService(
            ReviewStorage reviewStorage,
            @Qualifier("userDbStorage") UserStorage userStorage,
            @Qualifier("filmDbStorage") FilmStorage filmStorage,
            EventStorage eventStorage
    ) {
        this.reviewStorage = reviewStorage;
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
        this.eventStorage = eventStorage;
    }

    public Review findById(int id) {
        return reviewStorage.findById(id);
    }

    public List<Review> findByFilmId(Integer filmId, int count) {
        if (count < 0) {
            throw new ValidationException("Reviews count cannot be negative");
        }
        if (filmId != null) {
            filmStorage.findById(filmId);
        }
        return reviewStorage.findByFilmId(filmId, count);
    }

    public Review create(Review review) {
        validate(review);
        checkReferences(review);
        Review createdReview = reviewStorage.create(review);
        eventStorage.addEvent(createdReview.getUserId(), "REVIEW", "ADD", createdReview.getReviewId());
        log.info("Created review: {}", createdReview);
        return createdReview;
    }

    public Review update(Review review) {
        validate(review);
        Review savedReview = reviewStorage.findById(review.getReviewId());
        review.setUserId(savedReview.getUserId());
        review.setFilmId(savedReview.getFilmId());
        Review updatedReview = reviewStorage.update(review);
        eventStorage.addEvent(updatedReview.getUserId(), "REVIEW", "UPDATE", updatedReview.getReviewId());
        log.info("Updated review: {}", updatedReview);
        return updatedReview;
    }

    public void delete(int id) {
        Review review = reviewStorage.findById(id);
        reviewStorage.delete(id);
        eventStorage.addEvent(review.getUserId(), "REVIEW", "REMOVE", id);
        log.info("Deleted review with id={}", id);
    }

    public void addLike(int id, int userId) {
        checkUserExists(userId);
        reviewStorage.addReaction(id, userId, 1);
    }

    public void addDislike(int id, int userId) {
        checkUserExists(userId);
        reviewStorage.addReaction(id, userId, -1);
    }

    public void deleteLike(int id, int userId) {
        checkUserExists(userId);
        reviewStorage.deleteReaction(id, userId, 1);
    }

    public void deleteDislike(int id, int userId) {
        checkUserExists(userId);
        reviewStorage.deleteReaction(id, userId, -1);
    }

    private void validate(Review review) {
        if (review == null) {
            throw new ValidationException("Review is not passed");
        }
        if (!StringUtils.hasText(review.getContent())) {
            throw new ValidationException("Review content cannot be empty");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("Review positive flag is not passed");
        }
        if (review.getUserId() == 0) {
            throw new ValidationException("Review user id is not passed");
        }
        if (review.getFilmId() == 0) {
            throw new ValidationException("Review film id is not passed");
        }
    }

    private void checkReferences(Review review) {
        checkUserExists(review.getUserId());
        filmStorage.findById(review.getFilmId());
    }

    private void checkUserExists(int userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " not found");
        }
    }
}
