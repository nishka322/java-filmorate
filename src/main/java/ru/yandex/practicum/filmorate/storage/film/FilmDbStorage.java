package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Director;
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
            loadDirectorsForFilms(films);
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
        loadDirectorsForFilms(List.of(film));

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
        saveFilmDirectors(film);
        loadGenresForFilms(List.of(film));
        loadDirectorsForFilms(List.of(film));
        return film;
    }

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
        updateFilmDirectors(film);
        loadGenresForFilms(List.of(film));
        loadDirectorsForFilms(List.of(film));
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

    @Override
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

    // Реализация запроса на вывод общих фильмов друзей
    public Set<Integer> getCommonFilms(int userId, int friendId) {
        String sql = """
                SELECT DISTINCT l1.film_id
                FROM likes l1
                JOIN likes l2 ON l1.film_id = l2.film_id
                WHERE l1.user_id = ? AND l2.user_id = ?
                """;
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, userId, friendId));
    }

    @Override
    public Integer getLikeCount(int filmId) {
        String sql = """
                SELECT COUNT(*) FROM likes WHERE film_id = ?;
                """;
        return jdbcTemplate.queryForObject(sql, Integer.class, filmId);
    }

    // Остальная часть реализации приложения

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

    public void addDirectorToFilm(int filmId, int directorId) {
        String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, directorId);
    }

    public Director createDirector(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, director.getName());
            return stmt;
        }, keyHolder);

        director.setId(keyHolder.getKey().intValue());
        return director;
    }

    public List<Director> getAllDirectors() {
        String sql = "SELECT * FROM directors";
        return jdbcTemplate.query(sql, this::mapDirector);
    }

    private Director mapDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));
        return director;
    }

    private void loadDirectorsForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());

        String sql = "SELECT fd.film_id, d.id, d.name " +
                "FROM film_directors fd " +
                "JOIN directors d ON fd.director_id = d.id " +
                "WHERE fd.film_id IN (" + String.join(",", Collections.nCopies(filmIds.size(), "?")) + ")";

        Map<Integer, List<Director>> directorsByFilmId = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), (rs, rowNum) -> {
            int filmId = rs.getInt("film_id");
            Director director = new Director();
            director.setId(rs.getInt("id"));
            director.setName(rs.getString("name"));

            directorsByFilmId.computeIfAbsent(filmId, k -> new ArrayList<>()).add(director);
            return null;
        });

        for (Film film : films) {
            List<Director> directors = directorsByFilmId.get(film.getId());
            if (directors != null) {
                film.setDirectors(directors);
            } else {
                film.setDirectors(new ArrayList<>());
            }
        }
    }

    public List<Film> searchFilms(String query, String searchBy) {
        String searchQuery = "%" + query.toLowerCase() + "%";

        String[] searchParams = searchBy.split(",");
        boolean searchTitle = false;
        boolean searchDirector = false;

        for (String param : searchParams) {
            if ("title".equals(param.trim())) searchTitle = true;
            if ("director".equals(param.trim())) searchDirector = true;
        }

        if (!searchTitle && !searchDirector) {
            searchTitle = true;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT f.*, m.id AS mpa_id, m.name AS mpa_name, m.description AS mpa_description ")
                .append("FROM films f ")
                .append("LEFT JOIN mpa_ratings m ON f.mpa_id = m.id ");

        List<Object> params = new ArrayList<>();

        if (searchTitle && searchDirector) {
            sql.append("LEFT JOIN film_directors fd ON f.id = fd.film_id ")
                    .append("LEFT JOIN directors d ON fd.director_id = d.id ")
                    .append("WHERE LOWER(f.name) LIKE LOWER(?) OR LOWER(d.name) LIKE LOWER(?) ");
            params.add(searchQuery);
            params.add(searchQuery);
        } else if (searchTitle) {
            sql.append("WHERE LOWER(f.name) LIKE LOWER(?) ");
            params.add(searchQuery);
        } else if (searchDirector) {
            sql.append("LEFT JOIN film_directors fd ON f.id = fd.film_id ")
                    .append("LEFT JOIN directors d ON fd.director_id = d.id ")
                    .append("WHERE LOWER(d.name) LIKE LOWER(?) ");
            params.add(searchQuery);
        }

        sql.append("ORDER BY f.id");

        List<Film> films = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapFilm(rs, rowNum), params.toArray());

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    public List<Film> getFilmsByDirector(int directorId, String sortBy) {
        String baseSql = "SELECT DISTINCT f.*, m.id AS mpa_id, m.name AS mpa_name, m.description AS mpa_description, " +
                "COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN likes l ON f.id = l.film_id " +
                "LEFT JOIN film_directors fd ON f.id = fd.film_id " +
                "WHERE fd.director_id = ? " +
                "GROUP BY f.id, m.id, m.name, m.description ";

        String orderBy;
        if ("year".equals(sortBy)) {
            orderBy = "ORDER BY f.release_date";
        } else if ("likes".equals(sortBy)) {
            orderBy = "ORDER BY likes_count DESC";
        } else {
            orderBy = "ORDER BY f.id";
        }

        String finalSql = baseSql + orderBy;
        List<Film> films = jdbcTemplate.query(finalSql, this::mapFilm, directorId);

        if (!films.isEmpty()) {
            loadGenresForFilms(films);
            loadDirectorsForFilms(films);
        }

        return films;
    }

    private void saveFilmDirectors(Film film) {
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            String sql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
            for (Director director : film.getDirectors()) {
                jdbcTemplate.update(sql, film.getId(), director.getId());
            }
        }
    }

    private void updateFilmDirectors(Film film) {
        String deleteSql = "DELETE FROM film_directors WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());
        saveFilmDirectors(film);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}