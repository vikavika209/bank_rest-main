package com.example.bankcards.controller;

import com.example.bankcards.dto.UserRequestDto;
import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.exception.PasswordIsShortException;
import com.example.bankcards.exception.UserNotFoundCustomException;
import com.example.bankcards.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    UserService service;

    private UserResponseDto resp(long id, String username, Role role){
        UserResponseDto dto = new UserResponseDto();
        dto.setId(id);
        dto.setUsername(username);
        dto.setRoles(new HashSet<>(Set.of(role)));
        return dto;
    }

    @Test
    @DisplayName("POST /api/users/create — успешное создание (form params)")
    void createUser_ok() throws Exception {
        Mockito.when(service.create(any(UserRequestDto.class)))
                .thenReturn(resp(1L, "vika", Role.ROLE_USER));

        mvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"username":"vika","password":"secret123"}
        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("vika"))
                .andExpect(jsonPath("$.roles").value("ROLE_USER"));
    }

    @Test
    @DisplayName("GET /api/users/get/{id} — успешное получение пользователя")
    void getUser_ok() throws Exception {
        Mockito.when(service.getById(10L))
                .thenReturn(resp(10L, "john", Role.ROLE_ADMIN));

        mvc.perform(get("/api/users/get/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.roles").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("GET /api/users/all — постраничная выдача")
    void getAll_ok() throws Exception {
        Page<UserResponseDto> page = new PageImpl<>(List.of(
                resp(1L,"u1",Role.ROLE_USER),
                resp(2L,"u2",Role.ROLE_ADMIN)
        ));
        Mockito.when(service.getAll(0, 2)).thenReturn(page);

        mvc.perform(get("/api/users/all")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].username").value("u1"))
                .andExpect(jsonPath("$.content[1].roles").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @DisplayName("PUT /api/users/update/{id} — успешное обновление (form params)")
    void updateUser_ok() throws Exception {
        Mockito.when(service.update(eq(5L), any(UserRequestDto.class)))
                .thenReturn(resp(5L, "vika_new", Role.ROLE_USER));

        mvc.perform(put("/api/users/update/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"username":"vika","password":"newsecret123"}
        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.username").value("vika_new"));
    }

    @Test
    @DisplayName("PUT /api/users/block/{id} — блокировка пользователя")
    void blockUser_ok() throws Exception {
        Mockito.when(service.makeUnavailable(7L))
                .thenReturn(resp(7L, "block_me", Role.ROLE_USER));

        mvc.perform(put("/api/users/block/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @DisplayName("PUT /api/users/admin/{id} — назначить ADMIN")
    void makeAdmin_ok() throws Exception {
        Mockito.when(service.changeRoleAdmin(9L, true))
                .thenReturn(resp(9L, "boss", Role.ROLE_ADMIN));

        mvc.perform(put("/api/users/admin/{id}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("PUT /api/users/not_admin/{id} — снять ADMIN")
    void depriveAdmin_ok() throws Exception {
        Mockito.when(service.changeRoleAdmin(9L, false))
                .thenReturn(resp(9L, "boss", Role.ROLE_USER));

        mvc.perform(put("/api/users/not_admin/{id}", 9L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("ROLE_USER"));
    }

    @Test
    @DisplayName("DELETE /api/users/delete/{id} — успешное удаление")
    void deleteUser_ok() throws Exception {
        Mockito.doNothing().when(service).delete(3L);

        mvc.perform(delete("/api/users/delete/{id}", 3L))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Пользователь удалён: id = 3")));
    }

    @Test
    @DisplayName("UserNotFoundCustomException -> 404 NOT_FOUND")
    void get_notFound_maps_to_404() throws Exception {
        Mockito.when(service.getById(999L))
                .thenThrow(new UserNotFoundCustomException("id=999"));

        mvc.perform(get("/api/users/get/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Пользователь не найден"))
                .andExpect(jsonPath("$.detailedMessage").value("id=999"))
                .andExpect(jsonPath("$.localDateTime").exists());
    }

    @Test
    @DisplayName("PasswordIsShortException -> 500 Validation Error")
    void update_validation_error_400() throws Exception {
        Mockito.when(service.update(eq(5L), any()))
                .thenThrow(new PasswordIsShortException("min 8 chars"));

        mvc.perform(put("/api/users/update/{id}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"username":"vika","password":"123"}
        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.detailedMessage").exists());
    }

}