package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.DirectorDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.stream.Collectors;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import({
        FilmController.class,
        UserController.class,
        FilmService.class,
        UserService.class,
        FilmDbStorage.class,
        UserDbStorage.class,
        MpaDbStorage.class,
        GenreDbStorage.class,
        DirectorService.class,
        DirectorDbStorage.class
})
class UserControllerTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserDbStorage userStorage;

    @Autowired
    private FilmService filmService;

    @Autowired
    private FilmDbStorage filmStorage;


    @BeforeEach
    public void setUp() {
        userStorage.getAll().forEach(user -> userStorage.delete(user.getId()));
    }

    @Test
    public void createUserValidData() {
        User user = createValidUser("user@email.com", "login", "User Name", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(User.class, response.getBody());
        User createdUser = (User) response.getBody();
        assertEquals("User Name", createdUser.getName());
    }

    @Test
    public void createUserWithEmptyName() {
        User user = createValidUser("user@email.com", "login", "", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        User createdUser = (User) response.getBody();
        assertNotNull(createdUser);
        assertEquals("login", createdUser.getName());
    }

    @Test
    public void createUserWithNullName() {
        User user = createValidUser("user@email.com", "login", null, LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        User createdUser = (User) response.getBody();
        assertNotNull(createdUser);
        assertEquals("login", createdUser.getName());
    }

    @Test
    public void createUserWithWhitespaceName() {
        User user = createValidUser("user@email.com", "login", "   ", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        User createdUser = (User) response.getBody();
        assertNotNull(createdUser);
        assertEquals("login", createdUser.getName());
    }

    @Test
    public void updateUserExistingUser() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> createResponse = userController.createUser(user);
        User createdUser = (User) createResponse.getBody();

        User updatedUser = createValidUser("updated@email.com", "newlogin", "New Name", LocalDate.of(1995, 1, 1));
        assertNotNull(createdUser);
        updatedUser.setId(createdUser.getId());

        ResponseEntity<Object> response = userController.updateUser(updatedUser);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(User.class, response.getBody());
        User resultUser = (User) response.getBody();
        assertEquals("New Name", resultUser.getName());
        assertEquals("updated@email.com", resultUser.getEmail());
    }

    @Test
    public void updateUserNonExistingUser() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1990, 1, 1));
        user.setId(999);

        ResponseEntity<Object> response = userController.updateUser(user);

        assertEquals(404, response.getStatusCode().value());
        assertInstanceOf(Map.class, response.getBody());
    }

    @Test
    public void updateUserWithEmptyName() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> createResponse = userController.createUser(user);
        User createdUser = (User) createResponse.getBody();

        User updatedUser = createValidUser("updated@email.com", "newlogin", "", LocalDate.of(1995, 1, 1));
        assertNotNull(createdUser);
        updatedUser.setId(createdUser.getId());

        ResponseEntity<Object> response = userController.updateUser(updatedUser);

        assertEquals(200, response.getStatusCode().value());
        User resultUser = (User) response.getBody();
        assertNotNull(resultUser);
        assertEquals("newlogin", resultUser.getName());
    }

    @Test
    public void getAllUsersEmptyList() {
        List<User> users = userController.getAllUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void getAllUsersWithData() {
        User user1 = createValidUser("user1@email.com", "login1", "User One", LocalDate.of(1990, 1, 1));
        User user2 = createValidUser("user2@email.com", "login2", "User Two", LocalDate.of(1995, 1, 1));

        userController.createUser(user1);
        userController.createUser(user2);

        List<User> users = userController.getAllUsers();

        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("User One")));
        assertTrue(users.stream().anyMatch(u -> u.getName().equals("User Two")));
    }

    @Test
    public void createMultipleUsersCheckIds() {
        User user1 = createValidUser("user1@email.com", "login1", "User 1", LocalDate.of(1990, 1, 1));
        User user2 = createValidUser("user2@email.com", "login2", "User 2", LocalDate.of(1995, 1, 1));
        User user3 = createValidUser("user3@email.com", "login3", "User 3", LocalDate.of(2000, 1, 1));

        ResponseEntity<Object> response1 = userController.createUser(user1);
        ResponseEntity<Object> response2 = userController.createUser(user2);
        ResponseEntity<Object> response3 = userController.createUser(user3);

        User result1 = (User) response1.getBody();
        User result2 = (User) response2.getBody();
        User result3 = (User) response3.getBody();

        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);

        assertNotEquals(result1.getId(), result2.getId());
        assertNotEquals(result2.getId(), result3.getId());
    }

    @Test
    public void createUserWithFutureBirthday() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.now().plusDays(1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    public void createUserWithCurrentDateBirthday() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.now());
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    public void createUserWithVeryOldBirthday() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1900, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    public void updateUserMaintainsNameWhenEmpty() {
        User user = createValidUser("user@email.com", "login", "Original Name", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> createResponse = userController.createUser(user);
        User createdUser = (User) createResponse.getBody();

        User updatedUser = createValidUser("updated@email.com", "newlogin", "", LocalDate.of(1995, 1, 1));
        assertNotNull(createdUser);
        updatedUser.setId(createdUser.getId());

        userController.updateUser(updatedUser);
        List<User> users = userController.getAllUsers();

        assertEquals("newlogin", users.getFirst().getName());
    }

    @Test
    public void getRecommendationsReturnsFromSimilarUsers() {
        // Создание пользователей
        User target = createValidUser("t@mail.com", "target", "Target", LocalDate.of(1990, 1, 1));
        User similar = createValidUser("s@mail.com", "similar", "Similar", LocalDate.of(1991, 1, 1));
        User dissimilar = createValidUser("d@mail.com", "dissim", "Dissim", LocalDate.of(1992, 1, 1));

        int targetId = ((User) userController.createUser(target).getBody()).getId();
        int similarId = ((User) userController.createUser(similar).getBody()).getId();
        int dissimilarId = ((User) userController.createUser(dissimilar).getBody()).getId();

        // Создания фильмов
        int fX = filmStorage.create(film("X")).getId();
        int fY = filmStorage.create(film("Y")).getId();
        int fZ = filmStorage.create(film("Z")).getId();

        // Лайки: целевой любит X, похожий любит X,Y,Z; непохожий любит только Z (без пересечения)
        filmService.addLike(fX, targetId);

        filmService.addLike(fX, similarId);
        filmService.addLike(fY, similarId);
        filmService.addLike(fZ, similarId);

        filmService.addLike(fZ, dissimilarId);

        // Вызов эндпоинта
        ResponseEntity<Object> resp = userController.getRecommendations(targetId, 10);

        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        @SuppressWarnings("unchecked")
        List<Film> list = (List<Film>) resp.getBody();

        // ожидаем Y и Z (от похожего), но не X (уже лайкнут)
        List<Integer> ids = list.stream().map(Film::getId).collect(Collectors.toList());
        assertTrue(ids.contains(fY), "Ожидаем фильм Y среди рекомендаций");
        assertTrue(ids.contains(fZ), "Ожидаем фильм Z среди рекомендаций");
        assertFalse(ids.contains(fX), "Фильм X не должен рекомендоваться (уже лайкнут)");
    }

    @Test
    public void getRecommendationsRespectsLimitAndOrder() {
        // Создание пользователей
        User target = createValidUser("t2@mail.com", "target2", "Target2", LocalDate.of(1990, 1, 1));
        User sim1 = createValidUser("s1@mail.com", "sim1", "Similar1", LocalDate.of(1991, 1, 1));
        User sim2 = createValidUser("s2@mail.com", "sim2", "Similar2", LocalDate.of(1992, 1, 1));

        int targetId = ((User) userController.createUser(target).getBody()).getId();
        int sim1Id = ((User) userController.createUser(sim1).getBody()).getId();
        int sim2Id = ((User) userController.createUser(sim2).getBody()).getId();

        // Создание фильмов
        int fA = filmStorage.create(film("A")).getId();
        int fB = filmStorage.create(film("B")).getId();
        int fC = filmStorage.create(film("C")).getId();

        // Целевой лайкнул A
        filmService.addLike(fA, targetId);

        // Похожие лайкают:
        // sim1: A, B
        filmService.addLike(fA, sim1Id);
        filmService.addLike(fB, sim1Id);
        // sim2: A, B, C  (B получит больший «скор», чем C)
        filmService.addLike(fA, sim2Id);
        filmService.addLike(fB, sim2Id);
        filmService.addLike(fC, sim2Id);

        ResponseEntity<Object> resp = userController.getRecommendations(targetId, 1);

        assertEquals(200, resp.getStatusCode().value());
        @SuppressWarnings("unchecked")
        List<Film> list = (List<Film>) resp.getBody();

        assertNotNull(list);
        assertEquals(1, list.size(), "Должен учитываться limit=1");
        assertEquals(fB, list.get(0).getId(), "B должен быть первым по скору");
    }

    // Утилита для быстрого создания фильма
    private Film film(String name) {
        Film f = new Film();
        f.setName(name);
        f.setDescription(name + " desc");
        f.setReleaseDate(LocalDate.of(2000, 1, 1));
        f.setDuration(100);
        return f;
    }

    private User createValidUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        return user;
    }
}