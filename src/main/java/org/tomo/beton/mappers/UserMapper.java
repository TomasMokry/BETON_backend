package org.tomo.beton.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.tomo.beton.dtos.RegisterUserRequest;
import org.tomo.beton.dtos.UpdateUserRequest;
import org.tomo.beton.dtos.UserDto;
import org.tomo.beton.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
    User toEntity(RegisterUserRequest request);
    void update(UpdateUserRequest request, @MappingTarget User user);
}
