package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateCompilationRequest {

    @Size(min = 1, max = 50, message = "Название подборки должно быть от 1 до 50 символов")
    private String title;

    private Set<Long> events;
    private Boolean pinned;
}