package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import org.junit.jupiter.api.BeforeEach;
import ru.yandex.practicum.filmorate.model.User;


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
        GenreDbStorage.class
})
class FilmControllerTest {

    @Autowired
    private FilmController filmController;

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    @Autowired
    private UserService userService;

    private int userA;
    private int userB;
    private int userC;

    @BeforeEach
    void initUsers() {
        userA = createUser("a@mail.com", "a");
        userB = createUser("b@mail.com", "b");
        userC = createUser("c@mail.com", "c");

        userService.addFriend(userA, userB);
        userService.addFriend(userB, userA);
    }

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

    @Test
    void getCommonFilms_sortedByPopularity_desc() {
        int f1 = ((Film) filmController.addFilm(createValidFilm("F1", "d1", LocalDate.of(2000, 1, 1), 100)).getBody()).getId();
        int f2 = ((Film) filmController.addFilm(createValidFilm("F2", "d2", LocalDate.of(2001, 1, 1), 110)).getBody()).getId();
        int f3 = ((Film) filmController.addFilm(createValidFilm("F3", "d3", LocalDate.of(2002, 1, 1), 120)).getBody()).getId();

        addLike(f1, userA);
        addLike(f1, userB);
        addLike(f2, userA);
        addLike(f2, userB);
        addLike(f2, userC);

        List<Film> common = filmController.getFilmByPopularityCommon(userA, userB);
        assertNotNull(common);
        assertEquals(2, common.size());
        assertEquals(f2, common.get(0).getId());
        assertEquals(f1, common.get(1).getId());
        assertTrue(common.stream().noneMatch(f -> f.getId() == f3));
    }

    @Test
    void getCommonFilms_noIntersection_returnsEmpty() {
        int f1 = ((Film) filmController.addFilm(createValidFilm("F1", "d1", LocalDate.of(2000, 1, 1), 100)).getBody()).getId();
        int f2 = ((Film) filmController.addFilm(createValidFilm("F2", "d2", LocalDate.of(2001, 1, 1), 110)).getBody()).getId();

        addLike(f1, userA);
        addLike(f2, userB);

        List<Film> common = filmController.getFilmByPopularityCommon(userA, userB);
        assertNotNull(common);
        assertTrue(common.isEmpty());
    }

    @Test
    void getCommonFilms_unknownUser_throws() {
        assertThrows(RuntimeException.class, () -> filmController.getFilmByPopularityCommon(9999, userB));
    }

    private int createUser(String email, String login) {
        User u = new User();
        u.setEmail(email);
        u.setLogin(login);
        u.setName(login.toUpperCase());
        u.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.createUser(u).getId();
    }

    private void addLike(int filmId, int userId) {
        // если у тебя другой метод/подпись — замени имя вызова
        filmController.addLike(filmId, userId);
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