package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Film> getAll() {
        String filmsSql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name, m.description AS mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id";

        List<Film> films = jdbcTemplate.query(filmsSql, this::mapFilm);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    @Override
    public Optional<Film> getById(int id) {
        String filmSql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name, m.description AS mpa_description " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "WHERE f.id = ?";

        List<Film> films = jdbcTemplate.query(filmSql, this::mapFilm, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);
        loadGenresForFilms(List.of(film));

        return Optional.of(film);
    }

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, film.getReleaseDate() != null ? java.sql.Date.valueOf(film.getReleaseDate()) : null);
            stmt.setInt(4, film.getDuration());
            stmt.setObject(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return stmt;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());
        saveFilmGenres(film);
        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate() != null ? java.sql.Date.valueOf(film.getReleaseDate()) : null,
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId()
        );

        updateFilmGenres(film);
        return film;
    }

    @Override
    public void delete(int id) {
        String deleteLikesSql = "DELETE FROM likes WHERE film_id = ?";
        jdbcTemplate.update(deleteLikesSql, id);

        String deleteFilmGenresSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteFilmGenresSql, id);

        String deleteFilmSql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(deleteFilmSql, id);
    }

    @Override
    public boolean exists(int id) {
        String sql = "SELECT COUNT(*) FROM films WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    // Реализация Запросов для рекомендаций

    //id фильмов, где есть like от пользователя
    public Set<Integer> getLikedFilms(int userId) {
        String sql = "SELECT film_id FROM likes WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, userId));
    }

    // Пары для всех пользователей, которые лайкнули любой из переданных фильмов
    public Set<Integer> getUsersPairsForAnyFilms(Set<Integer> filmsId, int excludeUserId) {
        if (filmsId == null || filmsId.isEmpty()) {
            return Set.of();
        }
        String placeholders = String.join(",", Collections.nCopies(filmsId.size(), "?"));
        String sql = "SELECT DISTINCT user_id FROM likes WHERE film_id IN (" + placeholders + ") AND user_id <> ?";
        List<Object> params = new ArrayList<>(filmsId);
        params.add(excludeUserId);
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, params.toArray()));
    }

    // Список всех film_id который лайкнули пользователи
    public List<Integer> getFilmIdsLikedByUsers(Set<Integer> usersId) {
        if (usersId == null || usersId.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(usersId.size(), "?"));
        String sql = "SELECT film_id FROM likes WHERE user_id IN (" + placeholders + ")";
        return jdbcTemplate.queryForList(sql, Integer.class, usersId.toArray());
    }

    // Вывести фильмы по списку id и сохранить порядок переданного списка
    public List<Film> getFilmsByIdRestoringOrder(List<Integer> filmsId) {
        if (filmsId == null || filmsId.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", Collections.nCopies(filmsId.size(), "?"));
        String sql = """
                SELECT f.*, m.id AS mpa_id, m.name AS mpa_name, m.description AS mpa_description
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                WHERE f.id IN (""" + placeholders + ")";
        List<Film> films = jdbcTemplate.query(sql, this::mapFilm, filmsId.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        Map<Integer, Integer> order = new HashMap<>();
        for (int i = 0; i < filmsId.size(); i++) {
            order.put(filmsId.get(i), i);
        }
        films.sort(Comparator.comparingInt(f -> order.getOrDefault(f.getId(), Integer.MAX_VALUE)));
        return films;
    }

    // Остальная часть реализации приложения

    public MpaRating getMpaRatingById(int id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, this::mapMpaRating, id);
    }

    public List<MpaRating> getAllMpaRatings() {
        String sql = "SELECT * FROM mpa_ratings";
        return jdbcTemplate.query(sql, this::mapMpaRating);
    }

    public Genre getGenreById(int id) {
        String sql = "SELECT * FROM genres WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, this::mapGenre, id);
    }

    public List<Genre> getAllGenres() {
        String sql = "SELECT * FROM genres";
        return jdbcTemplate.query(sql, this::mapGenre);
    }

    public void addLike(int filmId, int userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public List<Integer> getLikes(int filmId) {
        String sql = "SELECT user_id FROM likes WHERE film_id = ?";
        return jdbcTemplate.queryForList(sql, Integer.class, filmId);
    }

    private void saveFilmGenres(Film film) {
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Genre genre : film.getGenres()) {
                jdbcTemplate.update(sql, film.getId(), genre.getId());
            }
        }
    }

    private void updateFilmGenres(Film film) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());
        saveFilmGenres(film);
    }

    public Film mapFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));

        java.sql.Date releaseDate = rs.getDate("release_date");
        if (releaseDate != null) {
            film.setReleaseDate(releaseDate.toLocalDate());
        }

        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            MpaRating mpa = new MpaRating();
            mpa.setId(mpaId);
            mpa.setName(rs.getString("mpa_name"));
            mpa.setDescription(rs.getString("mpa_description"));
            film.setMpa(mpa);
        }

        return film;
    }

    private MpaRating mapMpaRating(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        mpa.setDescription(rs.getString("description"));
        return mpa;
    }

    private Genre mapGenre(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }

    public void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());

        String sql = "SELECT fg.film_id, g.id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + String.join(",", Collections.nCopies(filmIds.size(), "?")) + ") " +
                "ORDER BY fg.film_id, g.id";

        Map<Integer, List<Genre>> genresByFilmId = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), (rs, rowNum) -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre();
            genre.setId(rs.getInt("id"));
            genre.setName(rs.getString("name"));

            genresByFilmId.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
            return null;
        });

        for (Film film : films) {
            List<Genre> genres = genresByFilmId.get(film.getId());
            if (genres != null) {
                film.setGenres(new LinkedHashSet<>(genres));
            } else {
                film.setGenres(new LinkedHashSet<>());
            }
        }
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}