package ru.yandex.practicum.filmorate.storage.review;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ReviewDbStorage implements ReviewStorage {

    private static final String BASE_SELECT = "SELECT r.id AS review_id, r.content, r.is_positive, " +
            "r.user_id, r.film_id, r.useful FROM reviews r";

    private final JdbcTemplate jdbcTemplate;
    private final ReviewRowMapper reviewRowMapper;

    @Autowired
    public ReviewDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.reviewRowMapper = new ReviewRowMapper();
    }

    @Override
    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id, useful) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> prepareCreateStatement(connection, sql, review), keyHolder);

        Number key = extractGeneratedId(keyHolder);
        int id = key.intValue();
        return getById(id).orElseThrow(() -> new IllegalStateException("Не удалось создать отзыв"));
    }

    private Number extractGeneratedId(KeyHolder keyHolder) {
        if (keyHolder.getKeys() != null && !keyHolder.getKeys().isEmpty()) {
            Object idValue = keyHolder.getKeys().get("id");
            if (idValue == null) {
                idValue = keyHolder.getKeys().get("ID");
            }
            if (idValue instanceof Number number) {
                return number;
            }
        }
        return keyHolder.getKey();
    }

    private PreparedStatement prepareCreateStatement(Connection connection, String sql, Review review) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, review.getContent());
        stmt.setBoolean(2, review.getIsPositive());
        stmt.setInt(3, review.getUserId());
        stmt.setInt(4, review.getFilmId());
        stmt.setInt(5, review.getUseful() == null ? 0 : review.getUseful());
        return stmt;
    }

    @Override
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                review.getContent(),
                review.getIsPositive(),
                review.getReviewId());

        return getById(review.getReviewId())
                .orElseThrow(() -> new IllegalArgumentException("Отзыв с id " + review.getReviewId() + " не найден"));
    }

    @Override
    public void delete(int id) {
        String deleteLikesSql = "DELETE FROM review_likes WHERE review_id = ?";
        jdbcTemplate.update(deleteLikesSql, id);

        String deleteReviewSql = "DELETE FROM reviews WHERE id = ?";
        jdbcTemplate.update(deleteReviewSql, id);
    }

    @Override
    public Optional<Review> getById(int id) {
        String sql = BASE_SELECT + " WHERE r.id = ?";
        List<Review> reviews = jdbcTemplate.query(sql, reviewRowMapper, id);
        return reviews.stream().findFirst();
    }

    @Override
    public List<Review> getByFilmId(int filmId, int count) {
        String sql = BASE_SELECT + " WHERE r.film_id = ? ORDER BY r.useful DESC, r.id LIMIT ?";
        return jdbcTemplate.query(sql, reviewRowMapper, filmId, count);
    }

    @Override
    public List<Review> getAll(int count) {
        String sql = BASE_SELECT + " ORDER BY r.useful DESC, r.id LIMIT ?";
        return jdbcTemplate.query(sql, reviewRowMapper, count);
    }

    @Override
    public void addLike(int reviewId, int userId) {
        upsertReaction(reviewId, userId, true);
    }

    @Override
    public void addDislike(int reviewId, int userId) {
        upsertReaction(reviewId, userId, false);
    }

    @Override
    public void removeLike(int reviewId, int userId) {
        removeReaction(reviewId, userId, true);
    }

    @Override
    public void removeDislike(int reviewId, int userId) {
        removeReaction(reviewId, userId, false);
    }

    private void upsertReaction(int reviewId, int userId, boolean isPositive) {
        String selectSql = "SELECT is_positive FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbcTemplate.query(selectSql, (rs, rowNum) -> rs.getBoolean("is_positive"), reviewId, userId);

        if (existing.isEmpty()) {
            String insertSql = "INSERT INTO review_likes (review_id, user_id, is_positive) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, reviewId, userId, isPositive);
            adjustUseful(reviewId, isPositive ? 1 : -1);
            return;
        }

        boolean current = existing.get(0);
        if (current == isPositive) {
            return;
        }

        String updateSql = "UPDATE review_likes SET is_positive = ? WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(updateSql, isPositive, reviewId, userId);
        adjustUseful(reviewId, isPositive ? 2 : -2);
    }

    private void removeReaction(int reviewId, int userId, boolean isPositive) {
        String selectSql = "SELECT is_positive FROM review_likes WHERE review_id = ? AND user_id = ?";
        List<Boolean> existing = jdbcTemplate.query(selectSql, (rs, rowNum) -> rs.getBoolean("is_positive"), reviewId, userId);

        if (existing.isEmpty() || existing.get(0) != isPositive) {
            return;
        }

        String deleteSql = "DELETE FROM review_likes WHERE review_id = ? AND user_id = ?";
        jdbcTemplate.update(deleteSql, reviewId, userId);
        adjustUseful(reviewId, isPositive ? -1 : 1);
    }

    private void adjustUseful(int reviewId, int delta) {
        String sql = "UPDATE reviews SET useful = useful + ? WHERE id = ?";
        jdbcTemplate.update(sql, delta, reviewId);
    }
}
