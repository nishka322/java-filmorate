package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmDbStorage.class, UserDbStorage.class, MpaDbStorage.class, GenreDbStorage.class})
class FilmDbStorageTest {

    @Autowired
    private FilmDbStorage filmStorage;

    @Autowired
    private UserDbStorage userStorage;

    private Film testFilm;
    private int userA;
    private int userB;
    private int userC;

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

        // Создаем режиссеров для тестов поиска
        Director nolan = new Director();
        nolan.setName("Кристофер Нолан");
        Director createdNolan = filmStorage.createDirector(nolan);

        Director tarantino = new Director();
        tarantino.setName("Квентин Тарантино");
        Director createdTarantino = filmStorage.createDirector(tarantino);

        Director cameron = new Director();
        cameron.setName("Джеймс Кэмерон");
        Director createdCameron = filmStorage.createDirector(cameron);

        // Создаем фильмы для тестов поиска
        Film film1 = new Film();
        film1.setName("Крадущийся тигр");
        film1.setDescription("Боевик про тигра");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        film1.setMpa(mpa);
        Film createdFilm1 = filmStorage.create(film1);
        filmStorage.addDirectorToFilm(createdFilm1.getId(), createdNolan.getId());

        Film film2 = new Film();
        film2.setName("Крадущийся в ночи");
        film2.setDescription("Триллер");
        film2.setReleaseDate(LocalDate.of(2001, 1, 1));
        film2.setDuration(110);
        film2.setMpa(mpa);
        Film createdFilm2 = filmStorage.create(film2);
        filmStorage.addDirectorToFilm(createdFilm2.getId(), createdTarantino.getId());

        Film film3 = new Film();
        film3.setName("Спящий дракон");
        film3.setDescription("Фэнтези");
        film3.setReleaseDate(LocalDate.of(2002, 1, 1));
        film3.setDuration(100);
        film3.setMpa(mpa);
        Film createdFilm3 = filmStorage.create(film3);
        filmStorage.addDirectorToFilm(createdFilm3.getId(), createdNolan.getId());

        Film film4 = new Film();
        film4.setName("КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");
        film4.setDescription("Боевик в верхнем регистре");
        film4.setReleaseDate(LocalDate.of(2003, 1, 1));
        film4.setDuration(130);
        film4.setMpa(mpa);
        Film createdFilm4 = filmStorage.create(film4);
        filmStorage.addDirectorToFilm(createdFilm4.getId(), createdCameron.getId());

        // Создаем пользователей для тестов общих фильмов
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

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        anotherFilm.setMpa(mpa);

        filmStorage.create(anotherFilm);

        List<Film> films = filmStorage.getAll();

        assertThat(films).hasSize(6);
        assertThat(films).extracting(Film::getName)
                .contains("Test Film", "Another Film");
        assertThat(films).extracting(Film::getName)
                .contains("Крадущийся тигр", "Крадущийся в ночи",
                        "Спящий дракон", "КРАДУЩИЙСЯ ТИГР: ВОЗВРАЩЕНИЕ КОТА");
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

    // Тесты для функционала поиска
    @Test
    public void testSearchFilmsByDirectorOnly() {
        List<Film> foundFilms = filmStorage.searchFilms("Нолан", "director");
        assertThat(foundFilms).hasSize(2);
        assertThat(foundFilms).extracting(Film::getName)
                .containsExactlyInAnyOrder("Крадущийся тигр", "Спящий дракон");
    }

    @Test
    public void testSearchFilmsByTitleAndDirector() {
        List<Film> foundFilms = filmStorage.searchFilms("крад", "title,director");
        assertThat(foundFilms).hasSize(3);

        List<Film> nolanFilms = filmStorage.searchFilms("Нолан", "title,director");
        assertThat(nolanFilms).hasSize(2);
    }

    @Test
    public void testSearchFilmsCaseInsensitiveDirector() {
        List<Film> lowerCase = filmStorage.searchFilms("нолан", "director");
        List<Film> upperCase = filmStorage.searchFilms("НОЛАН", "director");
        List<Film> mixedCase = filmStorage.searchFilms("НоЛаН", "director");

        assertThat(lowerCase).hasSize(2);
        assertThat(upperCase).hasSize(2);
        assertThat(mixedCase).hasSize(2);
    }

    // Тесты для функционала общих фильмов
    @Test
    void getCommonFilms_returnsIntersectionOnly() {
        int f1 = filmStorage.create(makeFilm("F1")).getId();
        int f2 = filmStorage.create(makeFilm("F2")).getId();
        int f3 = filmStorage.create(makeFilm("F3")).getId();

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
        int f1 = filmStorage.create(makeFilm("F1")).getId();
        int f2 = filmStorage.create(makeFilm("F2")).getId();
        int f3 = filmStorage.create(makeFilm("F3")).getId();

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
        int f1 = filmStorage.create(makeFilm("F1")).getId();
        filmStorage.addLike(f1, userA);
        filmStorage.addLike(f1, userB);
        filmStorage.removeLike(f1, userA);
        filmStorage.addLike(f1, userA);

        Set<Integer> ids = filmStorage.getCommonFilms(userA, userB);
        assertThat(ids).containsExactlyInAnyOrder(f1);
    }

    @Test
    void getFilmsByIdsPreserveOrder_returnsInSameOrder() {
        // создаём 3 фильма
        Film f1 = filmStorage.create(makeFilm("Order-1"));
        Film f2 = filmStorage.create(makeFilm("Order-2"));
        Film f3 = filmStorage.create(makeFilm("Order-3"));

        // Запрос в "перемешанном" порядке: f2, f1
        List<Film> ordered = filmStorage.getFilmsByIdRestoringOrder(Arrays.asList(f2.getId(), f1.getId()));

        assertThat(ordered).hasSize(2);
        assertThat(ordered.get(0).getId()).isEqualTo(f2.getId());
        assertThat(ordered.get(1).getId()).isEqualTo(f1.getId());
    }

    /* Утилита для локального создания фильма с MPA */
    private Film makeFilm(String name) {
        Film f = new Film();
        f.setName(name);
        f.setDescription(name + " desc");
        f.setReleaseDate(LocalDate.of(2000, 1, 1));
        f.setDuration(100);
        MpaRating m = new MpaRating();
        m.setId(1);
        f.setMpa(m);
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