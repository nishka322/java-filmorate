package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.util.Set;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmDbStorage.class, UserDbStorage.class, MpaDbStorage.class, GenreDbStorage.class})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    private int userA;
    private int userB;
    private int userC;


    private Film testFilm;

    @BeforeEach
    public void setUp() {
        testFilm = new Film();
        testFilm.setName("Test Film");
        testFilm.setDescription("Test Description");
        testFilm.setReleaseDate(LocalDate.of(2020, 1, 1));
        testFilm.setDuration(120);

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        testFilm.setMpa(mpa);

        userA = createUser("a@mail.com", "a");
        userB = createUser("b@mail.com", "b");
        userC = createUser("c@mail.com", "c");

    }

    @Test
    public void testCreateFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        assertThat(createdFilm).isNotNull();
        assertThat(createdFilm.getId()).isPositive();
        assertThat(createdFilm.getName()).isEqualTo("Test Film");
        assertThat(createdFilm.getDuration()).isEqualTo(120);
    }

    @Test
    public void testGetFilmById() {
        Film createdFilm = filmStorage.create(testFilm);
        Optional<Film> foundFilm = filmStorage.getById(createdFilm.getId());

        assertThat(foundFilm).isPresent();
        assertThat(foundFilm.get().getName()).isEqualTo("Test Film");
    }

    @Test
    public void testGetAllFilms() {
        filmStorage.create(testFilm);

        Film anotherFilm = new Film();
        anotherFilm.setName("Another Film");
        anotherFilm.setDescription("Another Description");
        anotherFilm.setReleaseDate(LocalDate.of(2021, 1, 1));
        anotherFilm.setDuration(90);
        filmStorage.create(anotherFilm);

        List<Film> films = filmStorage.getAll();

        assertThat(films).hasSize(2);
        assertThat(films).extracting(Film::getName)
                .containsExactlyInAnyOrder("Test Film", "Another Film");
    }

    @Test
    public void testUpdateFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        createdFilm.setName("Updated Film");
        createdFilm.setDuration(150);

        Film updatedFilm = filmStorage.update(createdFilm);

        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
        assertThat(updatedFilm.getDuration()).isEqualTo(150);

        Optional<Film> foundFilm = filmStorage.getById(createdFilm.getId());
        assertThat(foundFilm).isPresent();
        assertThat(foundFilm.get().getName()).isEqualTo("Updated Film");
    }

    @Test
    public void testFilmExists() {
        Film createdFilm = filmStorage.create(testFilm);

        boolean exists = filmStorage.exists(createdFilm.getId());
        boolean notExists = filmStorage.exists(999);

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    public void testDeleteFilm() {
        Film createdFilm = filmStorage.create(testFilm);

        boolean existsBefore = filmStorage.exists(createdFilm.getId());
        filmStorage.delete(createdFilm.getId());
        boolean existsAfter = filmStorage.exists(createdFilm.getId());

        assertThat(existsBefore).isTrue();
        assertThat(existsAfter).isFalse();
    }

    @Test
    public void testGetAllMpaRatings() {
        List<MpaRating> mpaRatings = filmStorage.getAllMpaRatings();

        assertThat(mpaRatings).isNotEmpty();
        assertThat(mpaRatings).extracting(MpaRating::getName)
                .contains("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    public void testGetAllGenres() {
        List<Genre> genres = filmStorage.getAllGenres();

        assertThat(genres).isNotEmpty();
        assertThat(genres).extracting(Genre::getName)
                .contains("Комедия", "Драма", "Боевик");
    }

    @Test
    void getCommonFilms_returnsIntersectionOnly() {
        int f1 = filmStorage.create(film("F1")).getId();
        int f2 = filmStorage.create(film("F2")).getId();
        int f3 = filmStorage.create(film("F3")).getId();

        filmStorage.addLike(f1, userA);
        filmStorage.addLike(f1, userB);

        filmStorage.addLike(f2, userA);
        filmStorage.addLike(f2, userB);
        filmStorage.addLike(f2, userC);

        Set<Integer> ids = filmStorage.getCommonFilms(userA, userB);
        assertThat(ids).containsExactlyInAnyOrder(f1, f2);
        assertThat(ids).doesNotContain(f3);
    }

    @Test
    void getLikeCount_countsAccurately() {
        int f1 = filmStorage.create(film("F1")).getId();
        int f2 = filmStorage.create(film("F2")).getId();
        int f3 = filmStorage.create(film("F3")).getId();

        filmStorage.addLike(f1, userA);
        filmStorage.addLike(f1, userB);

        filmStorage.addLike(f2, userA);
        filmStorage.addLike(f2, userB);
        filmStorage.addLike(f2, userC);

        assertThat(filmStorage.getLikeCount(f1)).isEqualTo(2);
        assertThat(filmStorage.getLikeCount(f2)).isEqualTo(3);
        assertThat(filmStorage.getLikeCount(f3)).isEqualTo(0);
    }

    @Test
    void getCommonFilms_isDistinct_noDuplicates() {
        int f1 = filmStorage.create(film("F1")).getId();
        filmStorage.addLike(f1, userA);
        filmStorage.addLike(f1, userB);
        filmStorage.removeLike(f1, userA);
        filmStorage.addLike(f1, userA);

        Set<Integer> ids = filmStorage.getCommonFilms(userA, userB);
        assertThat(ids).containsExactlyInAnyOrder(f1);
    }

    private Film film(String name) {
        Film f = new Film();
        f.setName(name);
        f.setDescription(name + " desc");
        f.setReleaseDate(LocalDate.of(2000, 1, 1));
        f.setDuration(100);
        return f;
    }

    private int createUser(String email, String login) {
        User u = new User();
        u.setEmail(email);
        u.setLogin(login);
        u.setName(login.toUpperCase());
        u.setBirthday(LocalDate.of(1990, 1, 1));
        return userStorage.create(u).getId();
    }

}