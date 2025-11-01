# java-filmorate

## БД

Для хранения данных реализована реляционная база данных. Ниже представлена ER-диаграмма структуры БД выполненная в `mermaid`. SVG-файл доступен [тут](FilmorateDiagram.svg).

```mermaid
erDiagram
    films {
        int id PK
        string name
        string description
        date release_date
        int duration
        int mpa_id FK
    }
    
    mpa_ratings {
        int id PK
        string name
        string description
    }
    
    genres {
        int id PK
        string name
    }
    
    film_genres {
        int film_id PK,FK
        int genre_id PK,FK
    }
    
    users {
        int id PK
        string email
        string login
        string name
        date birthday
    }
    
    friendships {
        int user_id PK,FK
        int friend_id PK,FK
        string status
        datetime created_at
    }
    
    likes {
        int film_id PK,FK
        int user_id PK,FK
        datetime created_at
    }

    films ||--o{ mpa_ratings : has
    films ||--o{ film_genres : has
    film_genres }o--|| genres : includes
    films ||--o{ likes : receives
    users ||--o{ likes : gives
    users ||--o{ friendships : initiates
```