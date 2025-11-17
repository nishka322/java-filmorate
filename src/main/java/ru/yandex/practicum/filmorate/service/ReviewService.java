package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;

@Slf4j
@Service
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;

    @Autowired
    public ReviewService(ReviewStorage reviewStorage, UserService userService, FilmService filmService) {
        this.reviewStorage = reviewStorage;
        this.userService = userService;
        this.filmService = filmService;
    }

    public Review createReview(Review review) {
        validateUserAndFilm(review.getUserId(), review.getFilmId());
        review.setUseful(0);
        Review created = reviewStorage.create(review);
        log.info("Создан отзыв {} для фильма {} пользователем {}", created.getReviewId(), created.getFilmId(), created.getUserId());
        return created;
    }

    public Review updateReview(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("Для обновления отзыва необходимо указать его идентификатор");
        }

        Review existing = getReviewById(review.getReviewId());
        review.setUserId(existing.getUserId());
        review.setFilmId(existing.getFilmId());
        review.setUseful(existing.getUseful());

        Review updated = reviewStorage.update(review);
        log.info("Обновлён отзыв {}", updated.getReviewId());
        return updated;
    }

    public void deleteReview(int id) {
        getReviewById(id);
        reviewStorage.delete(id);
        log.info("Удалён отзыв {}", id);
    }

    public Review getReviewById(int id) {
        return reviewStorage.getById(id).orElseThrow(() -> new IllegalArgumentException("Отзыв с id " + id + " не найден"));
    }

    public List<Review> getReviews(Integer filmId, Integer countParam) {
        int count = (countParam == null || countParam <= 0) ? 10 : countParam;
        if (filmId == null) {
            return reviewStorage.getAll(count);
        }

        filmService.getFilmById(filmId);
        return reviewStorage.getByFilmId(filmId, count);
    }

    public void addLike(int reviewId, int userId) {
        userService.getUserById(userId);
        getReviewById(reviewId);
        reviewStorage.addLike(reviewId, userId);
        log.debug("Пользователь {} поставил лайк отзыву {}", userId, reviewId);
    }

    public void addDislike(int reviewId, int userId) {
        userService.getUserById(userId);
        getReviewById(reviewId);
        reviewStorage.addDislike(reviewId, userId);
        log.debug("Пользователь {} поставил дизлайк отзыву {}", userId, reviewId);
    }

    public void removeLike(int reviewId, int userId) {
        userService.getUserById(userId);
        getReviewById(reviewId);
        reviewStorage.removeLike(reviewId, userId);
        log.debug("Пользователь {} удалил лайк с отзыва {}", userId, reviewId);
    }

    public void removeDislike(int reviewId, int userId) {
        userService.getUserById(userId);
        getReviewById(reviewId);
        reviewStorage.removeDislike(reviewId, userId);
        log.debug("Пользователь {} удалил дизлайк с отзыва {}", userId, reviewId);
    }

    private void validateUserAndFilm(Integer userId, Integer filmId) {
        if (userId == null || filmId == null || userId <= 0 || filmId <= 0) {
            throw new IllegalArgumentException("Необходимо указать идентификаторы пользователя и фильма");
        }
        userService.getUserById(userId);
        filmService.getFilmById(filmId);
    }
}