package com.econference.event.adapter.in.web;

import com.econference.event.application.port.in.GetOrganizerEventsUseCase;
import com.econference.event.config.EventExceptionHandler;
import com.econference.event.domain.Event;
import com.econference.event.domain.EventStatus;
import com.econference.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerIT {

    private MockMvc mockMvc;

    @Mock
    private GetOrganizerEventsUseCase getOrganizerEventsUseCase;

    @InjectMocks
    private EventController eventController;

    private final UUID organizerId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private record TestPrincipal(UUID userId) {}

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler(), new EventExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private UsernamePasswordAuthenticationToken principal() {
        return new UsernamePasswordAuthenticationToken(new TestPrincipal(organizerId), null);
    }

    @Test
    void shouldReturnEventsForOrganizer() throws Exception {
        Event event = Event.builder()
                .name("SBBD 2025")
                .status(EventStatus.PUBLISHED)
                .startDate(LocalDate.of(2025, 9, 15))
                .endDate(LocalDate.of(2025, 9, 18))
                .location("São Paulo, SP")
                .organizerId(organizerId)
                .build();

        when(getOrganizerEventsUseCase.get(any())).thenReturn(new GetOrganizerEventsUseCase.OrganizerListResult(
                List.of(event),
                new GetOrganizerEventsUseCase.Pagination(1, 20, 1, 1)
        ));

        mockMvc.perform(get("/api/v1/events")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("SBBD 2025"))
                .andExpect(jsonPath("$.data[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.total").value(1))
                .andExpect(jsonPath("$.pagination.totalPages").value(1));
    }

    @Test
    void shouldReturnEmptyListWhenNoEvents() throws Exception {
        when(getOrganizerEventsUseCase.get(any())).thenReturn(new GetOrganizerEventsUseCase.OrganizerListResult(
                List.of(),
                new GetOrganizerEventsUseCase.Pagination(1, 20, 0, 0)
        ));

        mockMvc.perform(get("/api/v1/events")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.pagination.total").value(0));
    }

    @Test
    void shouldAcceptStatusFilter() throws Exception {
        when(getOrganizerEventsUseCase.get(any())).thenReturn(new GetOrganizerEventsUseCase.OrganizerListResult(
                List.of(),
                new GetOrganizerEventsUseCase.Pagination(1, 20, 0, 0)
        ));

        mockMvc.perform(get("/api/v1/events")
                        .param("status", "PUBLISHED")
                        .principal(principal()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnBadRequestForInvalidStatus() throws Exception {
        when(getOrganizerEventsUseCase.get(any()))
                .thenThrow(new com.econference.shared.exception.BusinessException("Invalid status value: 'INVALIDO'"));

        mockMvc.perform(get("/api/v1/events")
                        .param("status", "INVALIDO")
                        .principal(principal()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnResponseShapeWithDataAndPaginationFields() throws Exception {
        when(getOrganizerEventsUseCase.get(any())).thenReturn(new GetOrganizerEventsUseCase.OrganizerListResult(
                List.of(),
                new GetOrganizerEventsUseCase.Pagination(1, 20, 0, 0)
        ));

        mockMvc.perform(get("/api/v1/events")
                        .principal(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.pagination").exists())
                .andExpect(jsonPath("$.pagination.page").exists())
                .andExpect(jsonPath("$.pagination.limit").exists())
                .andExpect(jsonPath("$.pagination.total").exists())
                .andExpect(jsonPath("$.pagination.totalPages").exists());
    }
}
