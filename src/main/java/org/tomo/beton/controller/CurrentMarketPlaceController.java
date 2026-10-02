package org.tomo.beton.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tomo.beton.dtos.ErrorDto;
import org.tomo.beton.dtos.MarketPlaceDto;
import org.tomo.beton.dtos.SetCurrentMarketPlaceRequest;
import org.tomo.beton.excetions.MarketPlaceArchivedException;
import org.tomo.beton.excetions.MarketPlaceNotFoundException;
import org.tomo.beton.service.MarketPlaceService;

@RestController
@AllArgsConstructor
@RequestMapping("/users/me/marketplace")
public class CurrentMarketPlaceController {
    private final MarketPlaceService marketPlaceService;

    @GetMapping
    public ResponseEntity<MarketPlaceDto> getCurrentMarketPlace() {
        return marketPlaceService.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<MarketPlaceDto> setCurrentMarketPlace(@RequestBody SetCurrentMarketPlaceRequest request) {
        return marketPlaceService.setCurrent(request.getMarketPlaceId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @ExceptionHandler(MarketPlaceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(MarketPlaceArchivedException.class)
    public ResponseEntity<ErrorDto> handleArchived(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }
}
