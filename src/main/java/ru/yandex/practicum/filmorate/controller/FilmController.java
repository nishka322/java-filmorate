package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController extends BaseController<Film> {
    private final List<Film> films = new ArrayList<>();
    private int nextId = 1;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

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
        log.info("Получен запрос на получение всех фильмов. Количество фильмов: {}", films.size());
        return new ArrayList<>(films);
    }

    @Override
    protected ResponseEntity<Object> addEntity(Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);
        try {
            validateEntity(film);
            film.setId(nextId++);
            films.add(film);
            log.info("Фильм успешно добавлен: {}", film);
            return ResponseEntity.status(HttpStatus.CREATED).body(film);
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
            for (int i = 0; i < films.size(); i++) {
                if (films.get(i).getId() == film.getId()) {
                    films.set(i, film);
                    log.info("Фильм успешно обновлен: {}", film);
                    return ResponseEntity.ok(film);
                }
            }
            log.error("Фильм с id {} не найден для обновления", film.getId());
            return createErrorResponse("Фильм с id " + film.getId() + " не найден", HttpStatus.NOT_FOUND);
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

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException e) {
        log.error("Ошибка валидации: {}", e.getMessage());
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);
        return createErrorResponse("Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}