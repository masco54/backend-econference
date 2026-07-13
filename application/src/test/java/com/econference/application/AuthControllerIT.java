package com.econference.application;

import com.econference.EConferenceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EConferenceApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(statements = "DELETE FROM user_sessions", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CorsConfigurationSource authCorsConfigurationSource;

    @Value("${app.cors.allowed-origins}")
    private String configuredAllowedOrigins;

    // == US1: Login emite cookie HttpOnly ==

    @Test
    void login_withValidCredentials_setsHttpOnlyCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@econference.local\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(cookie().path("auth_token", "/"))
                .andExpect(jsonPath("$.email").value("admin@econference.local"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void login_withInvalidCredentials_returns401WithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@econference.local\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().doesNotExist("auth_token"));
    }

    // == US2: GET /auth/me autentica via cookie ==

    @Test
    void getMe_withValidCookie_returns200WithUserData() throws Exception {
        String jwt = obtainJwtCookie();
        mockMvc.perform(get("/api/v1/auth/me")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@econference.local"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void getMe_withoutCookie_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().is(org.hamcrest.Matchers.either(
                        org.hamcrest.Matchers.is(401)).or(org.hamcrest.Matchers.is(403))));
    }

    // == US3: Logout remove o cookie ==

    @Test
    void logout_withValidCookie_returns204AndClearsCookie() throws Exception {
        String jwt = obtainJwtCookie();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("auth_token", jwt)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().maxAge("auth_token", 0));
    }

    // == US4: CORS configurado com allowCredentials ==

    @Test
    void corsConfig_allowsLocalhostWithCredentials() {
        var config = authCorsConfigurationSource.getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest());
        assertThat(config).isNotNull();
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedOrigins()).containsExactlyInAnyOrderElementsOf(
                java.util.Arrays.asList(configuredAllowedOrigins.split(",")));
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE");
    }

    // == Helpers ==

    private String obtainJwtCookie() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@econference.local\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var cookie = result.getResponse().getCookie("auth_token");
        if (cookie == null) throw new AssertionError("auth_token cookie not present after login");
        return cookie.getValue();
    }
}