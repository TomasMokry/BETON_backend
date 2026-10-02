package org.tomo.beton.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.tomo.beton.dtos.ErrorDto;
import org.tomo.beton.dtos.MarketPlaceDto;
import org.tomo.beton.dtos.MarketPlaceRequest;
import org.tomo.beton.excetions.MarketPlaceInUseException;
import org.tomo.beton.excetions.MarketPlaceNotFoundException;
import org.tomo.beton.service.MarketPlaceService;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/marketplaces")
public class MarketPlaceController {
    private final MarketPlaceService marketPlaceService;

    @GetMapping
    public List<MarketPlaceDto> getMarketPlaces(
            @RequestParam(name = "includeArchived", defaultValue = "false") boolean includeArchived
    ) {
        return marketPlaceService.getMarketPlaces(includeArchived);
    }

    @GetMapping("/{id}")
    public MarketPlaceDto getMarketPlace(@PathVariable Long id) {
        return marketPlaceService.getMarketPlace(id);
    }

    @PostMapping
    public ResponseEntity<MarketPlaceDto> createMarketPlace(
            @Valid @RequestBody MarketPlaceRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        var marketPlace = marketPlaceService.create(request);
        var uri = uriBuilder.path("/marketplaces/{id}").buildAndExpand(marketPlace.getId()).toUri();
        return ResponseEntity.created(uri).body(marketPlace);
    }

    @PutMapping("/{id}")
    public MarketPlaceDto updateMarketPlace(
            @PathVariable Long id,
            @Valid @RequestBody MarketPlaceRequest request
    ) {
        return marketPlaceService.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public MarketPlaceDto archiveMarketPlace(@PathVariable Long id) {
        return marketPlaceService.setActive(id, false);
    }

    @PatchMapping("/{id}/restore")
    public MarketPlaceDto restoreMarketPlace(@PathVariable Long id) {
        return marketPlaceService.setActive(id, true);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMarketPlace(@PathVariable Long id) {
        marketPlaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MarketPlaceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(MarketPlaceInUseException.class)
    public ResponseEntity<ErrorDto> handleInUse(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorDto(ex.getMessage()));
    }
}
