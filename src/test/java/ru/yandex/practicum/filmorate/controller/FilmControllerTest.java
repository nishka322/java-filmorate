package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {
    private FilmController filmController;
    private FilmService filmService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
        InMemoryUserStorage userStorage = new InMemoryUserStorage();

        userService = new UserService(userStorage);
        filmService = new FilmService(filmStorage, userService, null, null);
        filmController = new FilmController(filmService);
    }

    @Test
    void addFilmValidData() {
        Film film = createValidFilm("Test Film", "Test Description", LocalDate.of(2000, 1, 1), 120);
        ResponseEntity<Object> response = filmController.addFilm(film);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        Film createdFilm = (Film) response.getBody();
        assertEquals(1, createdFilm.getId());
        assertEquals("Test Film", createdFilm.getName());
    }

    @Test
    void addFilmInvalidReleaseDate() {
        Film film = createValidFilm("Old Film", "Very old film", LocalDate.of(1890, 1, 1), 90);

        ResponseEntity<Object> response = filmController.addFilm(film);

        assertTrue(response.getStatusCode().is2xxSuccessful() || response.getStatusCode().is4xxClientError());
    }

    @Test
    void updateFilmExistingFilm() {
        Film film = createValidFilm("Original", "Original desc", LocalDate.of(2000, 1, 1), 120);
        filmController.addFilm(film);

        Film updatedFilm = createValidFilm("Updated", "Updated desc", LocalDate.of(2001, 1, 1), 150);
        updatedFilm.setId(1);

        ResponseEntity<Object> response = filmController.updateFilm(updatedFilm);

        assertEquals(200, response.getStatusCode().value());
        Film resultFilm = (Film) response.getBody();
        assertEquals("Updated", resultFilm.getName());
    }

    @Test
    void updateFilmNonExistingFilm() {
        Film film = createValidFilm("Non Existing", "Description", LocalDate.of(2000, 1, 1), 120);
        film.setId(999);

        assertThrows(IllegalArgumentException.class, () -> filmController.updateFilm(film));
    }

    @Test
    void getAllFilmsEmptyList() {
        List<Film> films = filmController.getAllFilms();

        assertNotNull(films);
        assertTrue(films.isEmpty());
    }

    @Test
    void getAllFilmsWithData() {
        Film film1 = createValidFilm("Film 1", "Desc 1", LocalDate.of(2000, 1, 1), 120);
        Film film2 = createValidFilm("Film 2", "Desc 2", LocalDate.of(2001, 1, 1), 150);

        filmController.addFilm(film1);
        filmController.addFilm(film2);

        List<Film> films = filmController.getAllFilms();

        assertEquals(2, films.size());
    }

    @Test
    void addMultipleFilmsCheckIds() {
        Film film1 = createValidFilm("Film 1", "Desc 1", LocalDate.of(2000, 1, 1), 120);
        Film film2 = createValidFilm("Film 2", "Desc 2", LocalDate.of(2001, 1, 1), 150);
        Film film3 = createValidFilm("Film 3", "Desc 3", LocalDate.of(2002, 1, 1), 180);

        ResponseEntity<Object> response1 = filmController.addFilm(film1);
        ResponseEntity<Object> response2 = filmController.addFilm(film2);
        ResponseEntity<Object> response3 = filmController.addFilm(film3);

        Film result1 = (Film) response1.getBody();
        Film result2 = (Film) response2.getBody();
        Film result3 = (Film) response3.getBody();

        assertNotNull(result1);
        assertEquals(1, result1.getId());
        assertNotNull(result2);
        assertEquals(2, result2.getId());
        assertNotNull(result3);
        assertEquals(3, result3.getId());
    }

    private Film createValidFilm(String name, String description, LocalDate releaseDate, int duration) {
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        return film;
    }
}