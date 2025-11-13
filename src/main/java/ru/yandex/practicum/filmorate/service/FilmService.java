package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;

import java.util.*;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final FilmDbStorage filmDbStorage;
    private final UserService userService;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmService(FilmStorage filmStorage,
                       FilmDbStorage filmDbStorage,
                       UserService userService,
                       MpaDbStorage mpaStorage,
                       GenreDbStorage genreStorage,
                       JdbcTemplate jdbcTemplate) {
        this.filmStorage = filmStorage;
        this.filmDbStorage = filmDbStorage;
        this.userService = userService;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Film> getAllFilms() {
        log.debug("Получение списка всех фильмов");
        List<Film> films = filmStorage.getAll();
        log.debug("Получено {} фильмов", films.size());
        return films;
    }

    public Film getFilmById(int id) {
        log.debug("Поиск фильма с id {}", id);
        Film film = filmStorage.getById(id)
                .orElseThrow(() -> {
                    log.error("Фильм с id {} не найден", id);
                    return new IllegalArgumentException("Фильм с id " + id + " не найден");
                });
        return film;
    }

    public Film createFilm(Film film) {
        log.debug("Создание нового фильма: {}", film.getName());
        if (film.getMpa() != null && film.getMpa().getId() > 0) {
            MpaRating mpa = mpaStorage.getMpaRatingById(film.getMpa().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден"));
            film.setMpa(mpa);
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (genre.getId() > 0) {
                    genreStorage.getGenreById(genre.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Жанр с id " + genre.getId() + " не найден"));
                }
            }
        }

        Film createdFilm = filmStorage.create(film);
        log.info("Создан новый фильм: '{}' (id: {})", createdFilm.getName(), createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.debug("Обновление фильма с id {}", film.getId());

        Film existingFilm = filmStorage.getById(film.getId())
                .orElseThrow(() -> new IllegalArgumentException("Фильм с id " + film.getId() + " не найден"));

        if (film.getMpa() != null && film.getMpa().getId() > 0) {
            MpaRating mpa = mpaStorage.getMpaRatingById(film.getMpa().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Рейтинг MPA с id " + film.getMpa().getId() + " не найден"));
            film.setMpa(mpa);
        }

        Film updatedFilm = filmStorage.update(film);
        log.info("Фильм '{}' (id: {}) обновлен", updatedFilm.getName(), updatedFilm.getId());
        return updatedFilm;
    }

    public void addLike(int filmId, int userId) {
        log.debug("Добавление лайка: пользователь {} ставит лайк фильму {}", userId, filmId);

        userService.getUserById(userId);
        getFilmById(filmId);

        if (filmStorage instanceof FilmDbStorage) {
            FilmDbStorage filmDbStorage = (FilmDbStorage) filmStorage;
            filmDbStorage.addLike(filmId, userId);
        }

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        log.debug("Удаление лайка: пользователь {} удаляет лайк с фильма {}", userId, filmId);

        userService.getUserById(userId);
        getFilmById(filmId);

        if (filmStorage instanceof FilmDbStorage) {
            FilmDbStorage filmDbStorage = (FilmDbStorage) filmStorage;
            filmDbStorage.removeLike(filmId, userId);
        }

        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        log.debug("Получение {} популярных фильмов", count);

        if (filmStorage instanceof FilmDbStorage filmDbStorage) {
            String sql = "SELECT f.*, m.id AS mpa_id, m.name AS mpa_name, m.description AS mpa_description, " +
                    "COUNT(l.user_id) AS likes_count " +
                    "FROM films f " +
                    "LEFT JOIN mpa_ratings m ON f.mpa_id = m.id " +
                    "LEFT JOIN likes l ON f.id = l.film_id " +
                    "GROUP BY f.id, m.id, m.name, m.description " +
                    "ORDER BY likes_count DESC " +
                    "LIMIT ?";

            List<Film> films = filmDbStorage.getJdbcTemplate().query(sql, (rs, rowNum) -> {
                Film film = filmDbStorage.mapFilm(rs, rowNum);
                return film;
            }, count);

            if (!films.isEmpty()) {
                filmDbStorage.loadGenresForFilms(films);
            }

            log.info("Возвращено {} популярных фильмов", films.size());
            return films;
        }

        log.warn("FilmStorage не является FilmDbStorage");
        return List.of();
    }

    public boolean filmExists(int id) {
        log.debug("Проверка существования фильма с id {}", id);
        return filmStorage.exists(id);
    }

    public List<MpaRating> getAllMpaRatings() {
        log.debug("Получение списка всех рейтингов MPA");
        return mpaStorage.getAllMpaRatings();
    }

    public MpaRating getMpaRatingById(int id) {
        log.debug("Получение рейтинга MPA с id {}", id);
        return mpaStorage.getMpaRatingById(id)
                .orElseThrow(() -> new IllegalArgumentException("Рейтинг MPA с id " + id + " не найден"));
    }

    public List<Genre> getAllGenres() {
        log.debug("Получение списка всех жанров");
        return genreStorage.getAllGenres();
    }

    public Genre getGenreById(int id) {
        log.debug("Получение жанра с id {}", id);
        return genreStorage.getGenreById(id)
                .orElseThrow(() -> new IllegalArgumentException("Жанр с id " + id + " не найден"));
    }

    // Бизнес-логика для вывода общих фильмов друзей по рейтингу

    public List<Film> getFilmByPopularityCommon(int userId, int friendId) {
        userService.getUserById(userId);
        userService.getUserById(friendId);

        Set<Integer> filmsCommon = filmDbStorage.getCommonFilms(userId, friendId);

        if (filmsCommon.isEmpty()) {
            return List.of();
        }

        List<Film> films = new ArrayList<>();
        Map<Integer, Integer> likeCounts = new HashMap<>();

        for (Integer filmId : filmsCommon) {
            films.add(getFilmById(filmId));
            likeCounts.put(filmId, Optional.ofNullable(filmDbStorage.getLikeCount(filmId)).orElse(0));
        }

        films.sort((a, b) -> Integer.compare(likeCounts.getOrDefault(b.getId(), 0),
                likeCounts.getOrDefault(a.getId(), 0)));

        return films;
    }
    // Бизнес-логика для рекомендаций
    public List<Film> getRecomendation(int userId, int limit) {
        // Проверка на существование пользователей
        userService.getUserById(userId);

        if (!(filmStorage instanceof FilmDbStorage filmDbStorage)) {
            return List.of();
        }

        // Фильмы которые лайкнул целевой пользователь
        Set<Integer> likeByUser = filmDbStorage.getLikedFilms(userId);
        if (likeByUser.isEmpty()) {
            return List.of();
        }

        // Пользователи, лайкнувшие любой из фильмов(кроме самого userId)
        Set<Integer> neighborsUsers = filmDbStorage.getUsersPairsForAnyFilms(likeByUser, userId);
        if (neighborsUsers.isEmpty()) {
            return List.of();
        }

        // Подсчёт общих лайков у каждого соседа с целевым
        List<Integer> allFilmsBySimilar = filmDbStorage.getFilmIdsLikedByUsers(neighborsUsers);

        Map<Integer, Integer> scopeByFilm = new HashMap<>();
        for (Integer filmId : allFilmsBySimilar) {
            if (likeByUser.contains(filmId)) {
                continue;
            }
            scopeByFilm.merge(filmId, 1, Integer::sum);
        }
        if (scopeByFilm.isEmpty()) {
            return List.of();
        }

        // Вывод топ N-рекомендаций
        List<Integer> topFilmIds = scopeByFilm.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(Math.max(1, limit))
                .map(Map.Entry::getKey)
                .toList();

        return filmDbStorage.getFilmsByIdRestoringOrder(topFilmIds);
    }
}