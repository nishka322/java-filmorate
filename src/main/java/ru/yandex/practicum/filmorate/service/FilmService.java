package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       UserService userService,
                       MpaDbStorage mpaStorage,
                       GenreDbStorage genreStorage) {
        this.filmStorage = filmStorage;
        this.userService = userService;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    public List<Film> getAllFilms() {
        log.debug("Получение списка всех фильмов");
        List<Film> films = filmStorage.getAll();

        films.forEach(this::loadFilmDetails);

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

        loadFilmDetails(film);
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

        if (!filmStorage.exists(film.getId())) {
            throw new IllegalArgumentException("Фильм с id " + film.getId() + " не найден");
        }

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
        Film film = getFilmById(filmId);

        if (filmStorage instanceof FilmDbStorage) {
            FilmDbStorage filmDbStorage = (FilmDbStorage) filmStorage;
            filmDbStorage.addLike(filmId, userId);
        } else {
            if (film.getLikes().contains(userId)) {
                log.warn("Пользователь {} уже ставил лайк фильму {}", userId, filmId);
                throw new IllegalArgumentException("Пользователь уже поставил лайк этому фильму");
            }
            film.getLikes().add(userId);
            filmStorage.update(film);
        }

        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        log.debug("Удаление лайка: пользователь {} удаляет лайк с фильма {}", userId, filmId);

        userService.getUserById(userId);
        Film film = getFilmById(filmId);

        if (filmStorage instanceof FilmDbStorage) {
            FilmDbStorage filmDbStorage = (FilmDbStorage) filmStorage;
            filmDbStorage.removeLike(filmId, userId);
        } else {
            if (!film.getLikes().contains(userId)) {
                log.warn("Пользователь {} не ставил лайк фильму {}", userId, filmId);
                throw new IllegalArgumentException("Пользователь не ставил лайк этому фильму");
            }
            film.getLikes().remove(userId);
            filmStorage.update(film);
        }

        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        log.debug("Получение {} популярных фильмов", count);

        List<Film> allFilms = getAllFilms();

        List<Film> popularFilms = allFilms.stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());

        log.info("Возвращено {} популярных фильмов", popularFilms.size());
        return popularFilms;
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

    private void loadFilmDetails(Film film) {
        if (filmStorage instanceof FilmDbStorage filmDbStorage) {
            try {
                MpaRating mpa = filmDbStorage.loadFilmMpa(film.getId());
                if (mpa != null) {
                    film.setMpa(mpa);
                }
            } catch (Exception e) {
                log.debug("Не удалось загрузить MPA для фильма {}", film.getId());
            }

            List<Genre> genres = filmDbStorage.getFilmGenres(film.getId());
            film.setGenres(new java.util.HashSet<>(genres));

            List<Integer> likes = filmDbStorage.getLikes(film.getId());
            film.setLikes(new java.util.HashSet<>(likes));
        }
    }
}