package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private Integer reviewId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    private String content;

    @NotNull(message = "Необходимо указать тип отзыва")
    private Boolean isPositive;

    @NotNull(message = "Необходимо указать пользователя")
    private Integer userId;

    @NotNull(message = "Необходимо указать фильм")
    private Integer filmId;

    @Builder.Default
    private Integer useful = 0;
}
