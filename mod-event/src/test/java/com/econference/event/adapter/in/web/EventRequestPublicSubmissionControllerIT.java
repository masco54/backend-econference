package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.request.SubmitEventRequestRequest;
import com.econference.event.application.exception.DuplicateEventRequestException;
import com.econference.event.application.port.in.SubmitEventRequestUseCase;
import com.econference.event.config.EventExceptionHandler;
import com.econference.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventRequestPublicSubmissionControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SubmitEventRequestUseCase submitEventRequestUseCase;

    @InjectMocks
    private EventRequestPublicController eventRequestPublicController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(eventRequestPublicController)
                .setControllerAdvice(new GlobalExceptionHandler(), new EventExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnCreatedWhenSubmittingAValidEventRequest() throws Exception {
        when(submitEventRequestUseCase.submit(any())).thenReturn(
                new SubmitEventRequestUseCase.SubmitResult(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "SOL-2026-A3F8",
                        "Solicitação registrada com sucesso."
                )
        );

        mockMvc.perform(post("/public/event-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.trackingCode").value("SOL-2026-A3F8"))
                .andExpect(jsonPath("$.message").value("Solicitação registrada com sucesso."));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        var payload = new SubmitEventRequestRequest(
                "Ana",
                "Silva",
                null,
                "ana.silva@example.com",
                "+5511999999999",
                "UFSC",
                "",
                "Aplicações de IA na pesquisa científica",
                java.time.LocalDate.of(2026, 9, 10),
                java.time.LocalDate.of(2026, 9, 12),
                "Florianópolis",
                "conference",
                true,
                true,
                true
        );

        mockMvc.perform(post("/public/event-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturnConflictWhenAnOpenRequestAlreadyExists() throws Exception {
        when(submitEventRequestUseCase.submit(any())).thenThrow(
                new DuplicateEventRequestException("ana.silva@example.com", "Congresso Brasileiro de IA")
        );

        mockMvc.perform(post("/public/event-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_REQUEST_ALREADY_EXISTS"));
    }

    private SubmitEventRequestRequest validPayload() {
        return new SubmitEventRequestRequest(
                "Ana",
                "Silva",
                null,
                "ana.silva@example.com",
                "+5511999999999",
                "UFSC",
                "Congresso Brasileiro de IA",
                "Aplicações de IA na pesquisa científica",
                java.time.LocalDate.of(2026, 9, 10),
                java.time.LocalDate.of(2026, 9, 12),
                "Florianópolis",
                "conference",
                true,
                true,
                true
        );
    }
}