package com.econference.event.adapter.in.web;

import com.econference.event.application.port.in.AddEventRequestNoteUseCase;
import com.econference.event.application.port.in.ApproveEventRequestUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestDetailUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestsUseCase;
import com.econference.event.application.port.in.ReissueActivationTokenUseCase;
import com.econference.event.application.port.in.RejectEventRequestUseCase;
import com.econference.event.config.EventExceptionHandler;
import com.econference.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventRequestAdminController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({
        EventRequestAdminSecurityIT.TestSecurityConfig.class,
        GlobalExceptionHandler.class,
        EventExceptionHandler.class
})
class EventRequestAdminSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetAdminEventRequestsUseCase getAdminEventRequestsUseCase;

    @MockBean
    private GetAdminEventRequestDetailUseCase getAdminEventRequestDetailUseCase;

    @MockBean
    private AddEventRequestNoteUseCase addEventRequestNoteUseCase;

    @MockBean
    private ApproveEventRequestUseCase approveEventRequestUseCase;

    @MockBean
    private RejectEventRequestUseCase rejectEventRequestUseCase;

    @MockBean
    private ReissueActivationTokenUseCase reissueActivationTokenUseCase;

    @Test
    void shouldRejectUnauthenticatedAccessToAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/event-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAuthenticatedUserWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/admin/event-requests")
                        .with(user(User.withUsername("user@example.com").password("pw").roles("PARTICIPANT").build())))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAuthenticatedAdminAccessToAdminEndpoints() throws Exception {
        when(getAdminEventRequestsUseCase.get(any())).thenReturn(new GetAdminEventRequestsUseCase.AdminListResult(
                java.util.List.of(),
                new GetAdminEventRequestsUseCase.Pagination(1, 10, 0, 0)
        ));

        mockMvc.perform(get("/admin/event-requests")
                        .with(user(User.withUsername("admin@example.com").password("pw").roles("ADMIN").build())))
                .andExpect(status().isOk());
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