package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);

    @Autowired
    public FilmService(FilmStorage filmStorage, UserService userService) {
        this.filmStorage = filmStorage;
        this.userService = userService;
    }

    public List<Film> getAllFilms() {
        log.debug("Получение списка всех фильмов");
        List<Film> films = filmStorage.getAll();
        log.debug("Получено {} фильмов", films.size());
        return films;
    }


    public void addLike(int filmId, int userId) {
        log.debug("Добавление лайка: пользователь {} ставит лайк фильму {}", userId, filmId);
        Film film = getFilmById(filmId);
        userService.getUserById(userId);
        if (film.getLikes().contains(userId)) {
            log.warn("Пользователь {} уже ставил лайк фильму {}", userId, filmId);
            throw new IllegalArgumentException("Пользователь уже поставил лайк этому фильму");
        }
        film.getLikes().add(userId);
        filmStorage.update(film);
        log.info("Пользователь {} поставил лайк фильму {} (всего лайков: {})",
                userId, filmId, film.getLikes().size());
    }

    public void removeLike(int filmId, int userId) {
        log.debug("Удаление лайка: пользователь {} удаляет лайк с фильма {}", userId, filmId);
        Film film = getFilmById(filmId);
        if (!film.getLikes().contains(userId)) {
            log.warn("Пользователь {} не ставил лайк фильму {}", userId, filmId);
            throw new IllegalArgumentException("Пользователь не ставил лайк этому фильму");
        }
        film.getLikes().remove(userId);
        filmStorage.update(film);
        log.info("Пользователь {} удалил лайк с фильма {} (осталось лайков: {})",
                userId, filmId, film.getLikes().size());
    }

    public List<Film> getPopularFilms(int count) {
        log.debug("Получение {} популярных фильмов", count);
        List<Film> popularFilms = filmStorage.getAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());

        if (log.isDebugEnabled()) {
            String popularFilmsInfo = popularFilms.stream()
                    .map(film -> String.format("Фильм '%s' (id: %d) - %d лайков",
                            film.getName(), film.getId(), film.getLikes().size()))
                    .collect(Collectors.joining(", "));
            log.debug("Популярные фильмы: {}", popularFilmsInfo);
        }

        log.info("Возвращено {} популярных фильмов", popularFilms.size());
        return popularFilms;
    }

    public Film getFilmById(int id) {
        log.debug("Поиск фильма с id {}", id);
        return filmStorage.getById(id)
                .orElseThrow(() -> {
                    log.error("Фильм с id {} не найден", id);
                    return new IllegalArgumentException("Фильм с id " + id + " не найден");
                });
    }

    public Film createFilm(Film film) {
        log.debug("Создание нового фильма: {}", film.getName());
        Film createdFilm = filmStorage.create(film);
        log.info("Создан новый фильм: '{}' (id: {})", createdFilm.getName(), createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.debug("Обновление фильма с id {}", film.getId());
        Film updatedFilm = filmStorage.update(film);
        log.info("Фильм '{}' (id: {}) обновлен", updatedFilm.getName(), updatedFilm.getId());
        return updatedFilm;
    }

    public boolean filmExists(int id) {
        log.debug("Проверка существования фильма с id {}", id);
        return filmStorage.exists(id);
    }
}