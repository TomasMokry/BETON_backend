package org.tomo.beton.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.tomo.beton.dtos.*;
import org.tomo.beton.entities.User;
import org.tomo.beton.excetions.DuplicateUserException;
import org.tomo.beton.excetions.UserNotFoundException;
import org.tomo.beton.excetions.WrongPasswordException;
import org.tomo.beton.mappers.UserMapper;
import org.tomo.beton.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private UserService userService;

    private static User user() {
        return User.builder().id(1L).name("Tom").email("tom@mail.com").password("hash").role(Role.USER).build();
    }

    @ParameterizedTest
    @CsvSource({"name,name", "email,email", "password,name", "'',name"})
    void getAllUsers_sortsByAllowedFieldOrFallsBackToName(String requested, String expected) {
        var user = user();
        var dto = new UserDto(1L, "Tom", "tom@mail.com");
        when(userRepository.findAll(Sort.by(expected))).thenReturn(List.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        var result = userService.getAllUsers(requested);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getUser_found_returnsDto() {
        var user = user();
        var dto = new UserDto(1L, "Tom", "tom@mail.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        assertThat(userService.getUser(1L)).isSameAs(dto);
    }

    @Test
    void getUser_notFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(1L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void registerUser_duplicateEmail_throws() {
        var request = new RegisterUserRequest();
        request.setEmail("tom@mail.com");
        when(userRepository.existsByEmail("tom@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request)).isInstanceOf(DuplicateUserException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_hashesPasswordAndSetsUserRole() {
        var request = new RegisterUserRequest();
        request.setName("Tom");
        request.setEmail("tom@mail.com");
        request.setPassword("secret");
        var entity = User.builder().name("Tom").email("tom@mail.com").password("secret").build();
        when(userRepository.existsByEmail("tom@mail.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        userService.registerUser(request);

        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void updateUser_appliesRequestAndSaves() {
        var user = user();
        var request = new UpdateUserRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUser(1L, request);

        verify(userMapper).update(request, user);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_notFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deleteUser_deletes() {
        var user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(1L)).isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void changePassword_correctOldPassword_storesEncodedNewPassword() {
        var user = user();
        var request = new ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("new");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newHash");

        userService.changePassword(1L, request);

        assertThat(user.getPassword()).isEqualTo("newHash");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        var user = user();
        var request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("new");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(WrongPasswordException.class);
        assertThat(user.getPassword()).isEqualTo("hash");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_userNotFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changePassword(1L, new ChangePasswordRequest()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
