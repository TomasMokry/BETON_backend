package org.tomo.beton.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.tomo.beton.dtos.ErrorDto;
import org.tomo.beton.dtos.ProductDto;
import org.tomo.beton.entities.Product;
import org.tomo.beton.excetions.CartNotFoundException;
import org.tomo.beton.excetions.CategoryNotFoundException;
import org.tomo.beton.excetions.ProductNotFoundException;
import org.tomo.beton.mappers.ProductMapper;
import org.tomo.beton.repositories.CategoryRepository;
import org.tomo.beton.repositories.ProductRepository;

import java.util.Comparator;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/products")
@CrossOrigin(origins = "https://beton-hazel.vercel.app")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public List<ProductDto> getProducts(@RequestParam(name = "categoryId", required = false) Byte categoryId) {
        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);

        } else {
            products = productRepository.findAllWithCategory();
        }
        products.sort(Comparator.comparing(Product::getName));
        return products.stream().map(productMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            throw new ProductNotFoundException();
        }
        return ResponseEntity.ok(productMapper.toDto(product));
    }

    @PostMapping
    public ResponseEntity<ProductDto> saveProduct(
            @Valid @RequestBody ProductDto request,
            UriComponentsBuilder uriBuilder
    ) {
        var category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        if (category == null) {
            throw new CategoryNotFoundException();
        }
        var product = productMapper.toEntity(request);
        product.setCategory(category);
        productRepository.save(product);

        var uri = uriBuilder.path("/products/{id}").buildAndExpand(product.getId()).toUri();

        return ResponseEntity.created(uri).body(productMapper.toDto(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto request
    ) {
        var category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        if (category == null) {
            throw new CartNotFoundException();
        }

        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            throw new ProductNotFoundException();
        }
        productMapper.update(request, product);
        product.setCategory(category);
        productRepository.save(product);
        return ResponseEntity.ok(productMapper.toDto(product));
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id){
        var product = productRepository.findById(id).orElse(null);
        if (product == null) {
            throw new ProductNotFoundException();
        }
        productRepository.delete(product);
    }

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<ErrorDto> handleCartNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("Cart not found."));
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorDto> handleProductNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("Product not found in cart."));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ErrorDto> handleCategoryNotFound() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorDto("Category not found."));
    }

}
