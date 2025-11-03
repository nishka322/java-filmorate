-- Таблица рейтингов MPA
CREATE TABLE IF NOT EXISTS mpa_ratings (
                                           id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                           name VARCHAR(10) NOT NULL UNIQUE,
    description VARCHAR(255)
    );

-- Таблица жанров
CREATE TABLE IF NOT EXISTS genres (
                                      id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                      name VARCHAR(50) NOT NULL UNIQUE
    );

-- Основная таблица фильмов
CREATE TABLE IF NOT EXISTS films (
                                     id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                     name VARCHAR(255) NOT NULL,
    description TEXT,
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL,
    mpa_id INTEGER,
    FOREIGN KEY (mpa_id) REFERENCES mpa_ratings(id)
    );

-- Связующая таблица фильмов и жанров
CREATE TABLE IF NOT EXISTS film_genres (
                                           film_id INTEGER NOT NULL,
                                           genre_id INTEGER NOT NULL,
                                           PRIMARY KEY (film_id, genre_id),
    FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
    );

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                     email VARCHAR(255) NOT NULL UNIQUE,
    login VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    birthday DATE
    );

-- Таблица дружбы с статусами
CREATE TABLE IF NOT EXISTS friendships (
                                           user_id INTEGER NOT NULL,
                                           friend_id INTEGER NOT NULL,
                                           status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    CHECK (user_id != friend_id)
    );

-- Таблица лайков
CREATE TABLE IF NOT EXISTS likes (
                                     film_id INTEGER NOT NULL,
                                     user_id INTEGER NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (film_id, user_id),
    FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );