package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.film.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.film.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

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
        GenreDbStorage.class
})
class UserControllerTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserDbStorage userStorage;

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

    private User createValidUser(String email, String login, String name, LocalDate birthday) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(name);
        user.setBirthday(birthday);
        return user;
    }
}