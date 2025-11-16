package ru.yandex.practicum.filmorate.storage.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(ReviewDbStorage.class)
class ReviewDbStorageTest {

    @Autowired
    private ReviewDbStorage reviewStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private int authorId;
    private int filmId;

    @BeforeEach
    void setUp() {
        authorId = insertUser("author" + System.nanoTime());
        filmId = insertFilm("film" + System.nanoTime());
    }

    @Test
    void createReviewShouldPersistAndReturnGeneratedId() {
        Review created = reviewStorage.create(newReview("First", true, 0));

        assertThat(created.getReviewId()).isNotNull();

        Optional<Review> loaded = reviewStorage.getById(created.getReviewId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getContent()).isEqualTo("First");
        assertThat(loaded.get().getUseful()).isZero();
    }

    @Test
    void updateReviewShouldChangeContentAndPolarity() {
        Review created = reviewStorage.create(newReview("Original", true, 0));

        created.setContent("Updated");
        created.setIsPositive(false);
        Review updated = reviewStorage.update(created);

        assertThat(updated.getContent()).isEqualTo("Updated");
        assertThat(updated.getIsPositive()).isFalse();

        Optional<Review> loaded = reviewStorage.getById(created.getReviewId());
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getContent()).isEqualTo("Updated");
    }

    @Test
    void deleteReviewShouldRemoveRecord() {
        Review created = reviewStorage.create(newReview("To delete", true, 0));

        reviewStorage.delete(created.getReviewId());

        assertThat(reviewStorage.getById(created.getReviewId())).isEmpty();
    }

    @Test
    void getByFilmIdShouldReturnSortedLimitedList() {
        Review lowUseful = newReview("low", true, 1);
        Review highUseful = newReview("high", true, 5);
        Review midUseful = newReview("mid", true, 3);

        Review createdHigh = reviewStorage.create(highUseful);
        Review createdLow = reviewStorage.create(lowUseful);
        Review createdMid = reviewStorage.create(midUseful);

        List<Review> limited = reviewStorage.getByFilmId(filmId, 2);

        assertThat(limited).hasSize(2);
        assertThat(limited.get(0).getReviewId()).isEqualTo(createdHigh.getReviewId());
        assertThat(limited.get(1).getReviewId()).isEqualTo(createdMid.getReviewId());
        assertThat(limited).extracting(Review::getUseful).containsExactly(5, 3);

        // ensure all reviews still retrievable
        assertThat(reviewStorage.getById(createdLow.getReviewId())).isPresent();
    }

    @Test
    void getAllShouldRespectCountAndOrdering() {
        reviewStorage.create(newReview("first", true, 0));
        reviewStorage.create(newReview("second", true, 4));
        reviewStorage.create(newReview("third", false, 2));

        List<Review> reviews = reviewStorage.getAll(2);

        assertThat(reviews).hasSize(2);
        assertThat(reviews).extracting(Review::getUseful).containsExactly(4, 2);
    }

    @Test
    void likeAndDislikeFlowShouldAdjustUsefulScore() {
        Review created = reviewStorage.create(newReview("likeable", true, 0));
        int voterId = insertUser("voter" + System.nanoTime());

        reviewStorage.addLike(created.getReviewId(), voterId);
        Review afterLike = reviewStorage.getById(created.getReviewId()).orElseThrow();
        assertThat(afterLike.getUseful()).isEqualTo(1);

        reviewStorage.addDislike(created.getReviewId(), voterId);
        Review afterDislike = reviewStorage.getById(created.getReviewId()).orElseThrow();
        assertThat(afterDislike.getUseful()).isEqualTo(-1);

        reviewStorage.removeDislike(created.getReviewId(), voterId);
        Review afterRemoval = reviewStorage.getById(created.getReviewId()).orElseThrow();
        assertThat(afterRemoval.getUseful()).isEqualTo(0);
    }

    private Review newReview(String content, boolean isPositive, int useful) {
        return Review.builder()
                .content(content)
                .isPositive(isPositive)
                .userId(authorId)
                .filmId(filmId)
                .useful(useful)
                .build();
    }

    private int insertUser(String login) {
        String email = login + "@mail.ru";
        jdbcTemplate.update("INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                email,
                login,
                "User " + login,
                Date.valueOf(LocalDate.of(1990, 1, 1)));
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE login = ?", Integer.class, login);
    }

    private int insertFilm(String name) {
        jdbcTemplate.update("INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)",
                name,
                name + " description",
                Date.valueOf(LocalDate.of(2000, 1, 1)),
                120,
                1);
        return jdbcTemplate.queryForObject("SELECT id FROM films WHERE name = ?", Integer.class, name);
    }
}
