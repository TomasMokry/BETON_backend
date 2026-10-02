package org.tomo.beton.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.tomo.beton.dtos.MarketPlaceDto;
import org.tomo.beton.dtos.MarketPlaceRequest;
import org.tomo.beton.entities.MarketPlace;
import org.tomo.beton.excetions.MarketPlaceArchivedException;
import org.tomo.beton.excetions.MarketPlaceInUseException;
import org.tomo.beton.excetions.MarketPlaceNotFoundException;
import org.tomo.beton.mappers.MarketPlaceMapper;
import org.tomo.beton.repositories.MarketPlaceRepository;
import org.tomo.beton.repositories.OrderRepository;
import org.tomo.beton.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class MarketPlaceService {
    private final MarketPlaceRepository marketPlaceRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MarketPlaceMapper marketPlaceMapper;
    private final AuthService authService;

    public List<MarketPlaceDto> getMarketPlaces(boolean includeArchived) {
        var marketPlaces = includeArchived
                ? marketPlaceRepository.findAllSorted()
                : marketPlaceRepository.findAllActive();
        return marketPlaces.stream().map(marketPlaceMapper::toDto).toList();
    }

    public MarketPlaceDto getMarketPlace(Long id) {
        return marketPlaceMapper.toDto(findById(id));
    }

    public MarketPlaceDto create(MarketPlaceRequest request) {
        var marketPlace = marketPlaceMapper.toEntity(request);
        marketPlaceRepository.save(marketPlace);
        return marketPlaceMapper.toDto(marketPlace);
    }

    @Transactional
    public MarketPlaceDto update(Long id, MarketPlaceRequest request) {
        var marketPlace = findById(id);
        marketPlaceMapper.update(request, marketPlace);
        marketPlaceRepository.save(marketPlace);
        if (!marketPlace.isActive()) {
            marketPlaceRepository.clearFromUsers(marketPlace);
        }
        return marketPlaceMapper.toDto(marketPlace);
    }

    @Transactional
    public MarketPlaceDto setActive(Long id, boolean active) {
        var marketPlace = findById(id);
        marketPlace.setActive(active);
        marketPlaceRepository.save(marketPlace);
        if (!active) {
            // an archived market must never be stamped on new orders
            marketPlaceRepository.clearFromUsers(marketPlace);
        }
        return marketPlaceMapper.toDto(marketPlace);
    }

    @Transactional
    public void delete(Long id) {
        var marketPlace = findById(id);
        if (orderRepository.existsByMarketPlace(marketPlace)) {
            throw new MarketPlaceInUseException();
        }
        marketPlaceRepository.clearFromUsers(marketPlace);
        marketPlaceRepository.delete(marketPlace);
    }

    public Optional<MarketPlaceDto> getCurrent() {
        return Optional.ofNullable(authService.getCurrentUser().getCurrentMarketPlace())
                .map(marketPlaceMapper::toDto);
    }

    @Transactional
    public Optional<MarketPlaceDto> setCurrent(Long marketPlaceId) {
        var user = authService.getCurrentUser();
        MarketPlace marketPlace = null;
        if (marketPlaceId != null) {
            marketPlace = findById(marketPlaceId);
            if (!marketPlace.isActive()) {
                throw new MarketPlaceArchivedException();
            }
        }
        user.setCurrentMarketPlace(marketPlace);
        userRepository.save(user);
        return Optional.ofNullable(marketPlace).map(marketPlaceMapper::toDto);
    }

    private MarketPlace findById(Long id) {
        return marketPlaceRepository.findById(id).orElseThrow(MarketPlaceNotFoundException::new);
    }
}
