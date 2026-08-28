package org.tomo.beton.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.tomo.beton.validations.NotNullEmptyBlank;

import java.math.BigDecimal;

@Data
public class ProductDto {
    private Long id;
    @NotNullEmptyBlank
    private String name;
    @NotNullEmptyBlank
    private String size;
    @Positive
    @NotNull
    private BigDecimal price;
    @Positive
    @NotNull
    private int height;
    @NotNullEmptyBlank
    private String description;
    @Positive
    @NotNull
    private int width;
    @Positive
    @NotNull
    private int weight;
    @Positive
    @NotNull
    private int length;
    @NotNullEmptyBlank
    private String color;
    @NotNullEmptyBlank
    private String urlImage;
    @Positive
    @NotNull
    private int amount;
    private Byte categoryId;
}
