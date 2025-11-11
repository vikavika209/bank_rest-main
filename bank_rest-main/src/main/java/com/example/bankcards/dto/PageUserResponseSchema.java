package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PageUserResponse", description = "Страница пользователей")
public class PageUserResponseSchema {

    @ArraySchema(schema = @Schema(implementation = UserResponseDto.class))
    public List<UserResponseDto> content;

    @Schema(example = "0", description = "Номер страницы (0..N)")
    public int number;

    @Schema(example = "20", description = "Размер страницы")
    public int size;

    @Schema(example = "123", description = "Общее количество элементов")
    public long totalElements;

    @Schema(example = "7", description = "Общее количество страниц")
    public int totalPages;

    @Schema(example = "true", description = "Является ли страница первой")
    public boolean first;

    @Schema(example = "false", description = "Является ли страница последней")
    public boolean last;
}

