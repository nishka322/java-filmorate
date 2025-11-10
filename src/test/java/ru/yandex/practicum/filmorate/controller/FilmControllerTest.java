package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import({
        FilmController.class,
        FilmService.class,
        UserService.class,
        FilmDbStorage.class,
        UserDbStorage.class,
        MpaDbStorage.class,
        GenreDbStorage.class,
        DirectorService.class,
        DirectorDbStorage.class
})
class FilmControllerTest {

    @Autowired
    private FilmController filmController;

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    @Test
    public void addFilmValidData() {
        Film film = createValidFilm("Test Film", "Test Description", LocalDate.of(2000, 1, 1), 120);
        ResponseEntity<Object> response = filmController.addFilm(film);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        Film createdFilm = (Film) response.getBody();
        assertEquals("Test Film", createdFilm.getName());
    }

    @Test
    public void addFilmInvalidReleaseDate() {
        Film film = createValidFilm("Old Film", "Very old film", LocalDate.of(1890, 1, 1), 90);

        ResponseEntity<Object> response = filmController.addFilm(film);

        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    public void updateFilmExistingFilm() {
        Film film = createValidFilm("Original", "Original desc", LocalDate.of(2000, 1, 1), 120);
        ResponseEntity<Object> createResponse = filmController.addFilm(film);
        Film createdFilm = (Film) createResponse.getBody();

        Film updatedFilm = createValidFilm("Updated", "Updated desc", LocalDate.of(2001, 1, 1), 150);
        assertNotNull(createdFilm);
        updatedFilm.setId(createdFilm.getId());

        ResponseEntity<Object> response = filmController.updateFilm(updatedFilm);

        assertEquals(200, response.getStatusCode().value());
        Film resultFilm = (Film) response.getBody();
        assertNotNull(resultFilm);
        assertEquals("Updated", resultFilm.getName());
    }

    @Test
    public void updateFilmNonExistingFilm() {
        Film film = createValidFilm("Non Existing", "Description", LocalDate.of(2000, 1, 1), 120);
        film.setId(999);

        assertThrows(RuntimeException.class, () -> filmController.updateFilm(film));
    }

    @Test
    public void getAllFilmsEmptyList() {
        List<Film> films = filmController.getAllFilms();

        assertNotNull(films);
        assertTrue(films.isEmpty());
    }

    @Test
    public void getAllFilmsWithData() {
        Film film1 = createValidFilm("Film 1", "Desc 1", LocalDate.of(2000, 1, 1), 120);
        Film film2 = createValidFilm("Film 2", "Desc 2", LocalDate.of(2001, 1, 1), 150);

        filmController.addFilm(film1);
        filmController.addFilm(film2);

        List<Film> films = filmController.getAllFilms();

        assertEquals(2, films.size());
    }

    @Test
    public void addMultipleFilmsCheckIds() {
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
        assertNotNull(result2);
        assertNotNull(result3);

        assertNotEquals(result1.getId(), result2.getId());
        assertNotEquals(result2.getId(), result3.getId());
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