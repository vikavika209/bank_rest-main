package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "PageCardResponse", description = "Страница с картами")
public class PageCardResponseSchema {

    @ArraySchema(schema = @Schema(implementation = CardResponseDto.class))
    private List<CardResponseDto> content;

    @Schema(example = "0", description = "Номер текущей страницы (0..N)")
    private int number;

    @Schema(example = "20", description = "Размер страницы")
    private int size;

    @Schema(example = "123", description = "Общее количество элементов")
    private long totalElements;

    @Schema(example = "7", description = "Общее количество страниц")
    private int totalPages;

    @Schema(example = "true", description = "Является ли страница первой")
    private boolean first;

    @Schema(example = "false", description = "Является ли страница последней")
    private boolean last;
}