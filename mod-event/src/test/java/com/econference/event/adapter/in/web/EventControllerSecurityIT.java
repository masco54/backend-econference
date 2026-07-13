package com.econference.event.adapter.in.web;

import com.econference.event.application.port.in.GetOrganizerEventsUseCase;
import com.econference.event.config.EventExceptionHandler;
import com.econference.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        EventControllerSecurityIT.TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        EventExceptionHandler.class
})
class EventControllerSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetOrganizerEventsUseCase getOrganizerEventsUseCase;

    private record TestPrincipal(UUID userId) {}

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken(
                new TestPrincipal(UUID.randomUUID()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAuthenticatedUserWithoutOrganizerRole() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .with(authentication(auth("PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAuthenticatedOrganizerAccess() throws Exception {
        when(getOrganizerEventsUseCase.get(any())).thenReturn(new GetOrganizerEventsUseCase.OrganizerListResult(
                List.of(),
                new GetOrganizerEventsUseCase.Pagination(1, 20, 0, 0)
        ));

        mockMvc.perform(get("/api/v1/events")
                        .with(authentication(auth("ORGANIZER"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectReviewerRole() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .with(authentication(auth("REVIEWER"))))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .httpBasic(httpBasic -> {
                    })
                    .build();
        }
    }

    @SpringBootApplication
    static class TestApplication {
    }
}
