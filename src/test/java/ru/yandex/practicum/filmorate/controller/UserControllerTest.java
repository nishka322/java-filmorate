package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class UserControllerTest {
    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void createUserValidData() {
        User user = createValidUser("user@email.com", "login", "User Name", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertInstanceOf(User.class, response.getBody());
        User createdUser = (User) response.getBody();
        assertEquals(1, createdUser.getId());
        assertEquals("User Name", createdUser.getName());
    }

    @Test
    void createUserWithEmptyName() {
        User user = createValidUser("user@email.com", "login", "", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        User createdUser = (User) response.getBody();
        assertNotNull(createdUser);
        assertEquals("login", createdUser.getName());
    }

    @Test
    void createUserWithNullName() {
        User user = createValidUser("user@email.com", "login", null, LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        User createdUser = (User) response.getBody();
        assertNotNull(createdUser);
        assertEquals("login", createdUser.getName());
    }

    @Test
    void createUserWithWhitespaceName() {
        User user = createValidUser("user@email.com", "login", "   ", LocalDate.of(1990, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
        User createdUser = (User) response.getBody();
        assertNotNull(createdUser);
        assertEquals("login", createdUser.getName());
    }

    @Test
    void updateUserExistingUser() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1990, 1, 1));
        userController.createUser(user);

        User updatedUser = createValidUser("updated@email.com", "newlogin", "New Name", LocalDate.of(1995, 1, 1));
        updatedUser.setId(1);

        ResponseEntity<Object> response = userController.updateUser(updatedUser);

        assertEquals(200, response.getStatusCode().value());
        assertInstanceOf(User.class, response.getBody());
        User resultUser = (User) response.getBody();
        assertEquals("New Name", resultUser.getName());
        assertEquals("updated@email.com", resultUser.getEmail());
    }

    @Test
    void updateUserNonExistingUser() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1990, 1, 1));
        user.setId(999);

        ResponseEntity<Object> response = userController.updateUser(user);

        assertEquals(404, response.getStatusCode().value());
        assertInstanceOf(Map.class, response.getBody());
    }

    @Test
    void updateUserWithEmptyName() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1990, 1, 1));
        userController.createUser(user);

        User updatedUser = createValidUser("updated@email.com", "newlogin", "", LocalDate.of(1995, 1, 1));
        updatedUser.setId(1);

        ResponseEntity<Object> response = userController.updateUser(updatedUser);

        assertEquals(200, response.getStatusCode().value());
        User resultUser = (User) response.getBody();
        assertNotNull(resultUser);
        assertEquals("newlogin", resultUser.getName());
    }

    @Test
    void getAllUsersEmptyList() {
        List<User> users = userController.getAllUsers();

        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    void getAllUsersWithData() {
        User user1 = createValidUser("user1@email.com", "login1", "User One", LocalDate.of(1990, 1, 1));
        User user2 = createValidUser("user2@email.com", "login2", "User Two", LocalDate.of(1995, 1, 1));

        userController.createUser(user1);
        userController.createUser(user2);

        List<User> users = userController.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("User One", users.get(0).getName());
        assertEquals("User Two", users.get(1).getName());
    }

    @Test
    void createMultipleUsersCheckIds() {
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
        assertEquals(1, result1.getId());
        assertNotNull(result2);
        assertEquals(2, result2.getId());
        assertNotNull(result3);
        assertEquals(3, result3.getId());
    }

    @Test
    void createUserWithFutureBirthday() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.now().plusDays(1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void createUserWithCurrentDateBirthday() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.now());
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void createUserWithVeryOldBirthday() {
        User user = createValidUser("user@email.com", "login", "Name", LocalDate.of(1900, 1, 1));
        ResponseEntity<Object> response = userController.createUser(user);

        assertEquals(201, response.getStatusCode().value());
    }

    @Test
    void updateUserMaintainsNameWhenEmpty() {
        User user = createValidUser("user@email.com", "login", "Original Name", LocalDate.of(1990, 1, 1));
        userController.createUser(user);

        User updatedUser = createValidUser("updated@email.com", "newlogin", "", LocalDate.of(1995, 1, 1));
        updatedUser.setId(1);

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