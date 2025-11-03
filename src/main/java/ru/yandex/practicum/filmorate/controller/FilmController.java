package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.service.FilmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController extends BaseController<Film> {
    private final FilmService filmService;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> addFilm(@Valid @RequestBody Film film) {
        return addEntity(film);
    }

    @PutMapping
    public ResponseEntity<Object> updateFilm(@Valid @RequestBody Film film) {
        return updateEntity(film);
    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов. Количество фильмов: {}", filmService.getAllFilms().size());
        return filmService.getAllFilms();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getFilm(@PathVariable int id) {
        try {
            Film film = filmService.getFilmById(id);
            return ResponseEntity.ok(film);
        } catch (IllegalArgumentException e) {
            log.error("Фильм с id {} не найден", id);
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Object> addLike(@PathVariable int id, @PathVariable int userId) {
        try {
            filmService.addLike(id, userId);
            log.info("Пользователь {} поставил лайк фильму {}", userId, id);
            return ResponseEntity.ok(Map.of(
                    "message", "Лайк успешно добавлен",
                    "filmId", id,
                    "userId", userId
            ));
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при добавлении лайка: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Object> removeLike(@PathVariable int id, @PathVariable int userId) {
        try {
            filmService.removeLike(id, userId);
            log.info("Пользователь {} удалил лайк с фильма {}", userId, id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Ошибка при удалении лайка: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(name = "count", defaultValue = "10") int count) {
        log.info("Получен запрос на получение {} популярных фильмов", count);
        return filmService.getPopularFilms(count);
    }

    @Override
    protected ResponseEntity<Object> addEntity(Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);
        try {
            validateEntity(film);
            Film createdFilm = filmService.createFilm(film);
            log.info("Фильм успешно добавлен: {}", createdFilm);
            return ResponseEntity.ok(createdFilm);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при добавлении фильма: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    protected ResponseEntity<Object> updateEntity(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);
        try {
            validateEntity(film);

            if (!filmService.filmExists(film.getId())) {
                log.error("Фильм с id {} не найден для обновления", film.getId());
                return createErrorResponse("Фильм с id " + film.getId() + " не найден", HttpStatus.NOT_FOUND);
            }

            Film updatedFilm = filmService.updateFilm(film);
            log.info("Фильм успешно обновлен: {}", updatedFilm);
            return ResponseEntity.ok(updatedFilm);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении фильма: {}", e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    protected void validateEntity(Film film) throws ValidationException {
        if (film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.error("Ошибка валидации: дата релиза {} раньше минимальной допустимой даты {}",
                    film.getReleaseDate(), MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}