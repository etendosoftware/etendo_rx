package com.etendorx.auth.controller;

import com.etendorx.auth.auth.AuthService;
import com.etendorx.auth.auth.jwt.JwtRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerInvalidJwtRequestTest {

    @Autowired
    private MockMvc mockMvc;

    public static Stream<Arguments> invalidJwtRequestParams() {
        return Stream.of(
                // Undefined username
                Arguments.of(null, null, AuthService.UNDEFINED_USERNAME_MESSAGE),
                Arguments.of("", "", AuthService.UNDEFINED_USERNAME_MESSAGE),
                Arguments.of("", null, AuthService.UNDEFINED_USERNAME_MESSAGE),
                Arguments.of("", "pass123", AuthService.UNDEFINED_USERNAME_MESSAGE),
                // Undefined password
                Arguments.of("user123", null, AuthService.UNDEFINED_PASSWORD_MESSAGE),
                Arguments.of("user123", "", AuthService.UNDEFINED_PASSWORD_MESSAGE)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidJwtRequestParams")
    public void invalidJwtRequest(String username, String password, String errorMessage) throws Exception {
        JwtRequest request = new JwtRequest();
        request.setUsername(username);
        request.setPassword(password);

        ResultActions resultActions = this.mockMvc.perform(MockMvcRequestBuilders
                .post("/api/authenticate")
                .content(AuthControllerUtils.asJsonString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        resultActions.andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ResponseStatusException))
                .andExpect(result -> {
                    assertEquals(
                            Objects.requireNonNull((ResponseStatusException) result.getResolvedException()).getReason(),
                            errorMessage);
                });
    }

}
