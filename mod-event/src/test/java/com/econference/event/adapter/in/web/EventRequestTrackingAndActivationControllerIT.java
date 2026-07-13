package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.request.ActivateEventRequestRequest;
import com.econference.event.adapter.in.web.dto.request.TrackEventRequestRequest;
import com.econference.event.application.port.in.ActivateEventRequestUseCase;
import com.econference.event.application.port.in.SubmitEventRequestUseCase;
import com.econference.event.application.port.in.TrackEventRequestUseCase;
import com.econference.event.application.port.in.ValidateActivationTokenUseCase;
import com.econference.event.config.EventExceptionHandler;
import com.econference.shared.exception.GlobalExceptionHandler;
import com.econference.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventRequestTrackingAndActivationControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SubmitEventRequestUseCase submitEventRequestUseCase;

    @Mock
    private TrackEventRequestUseCase trackEventRequestUseCase;

    @Mock
    private ValidateActivationTokenUseCase validateActivationTokenUseCase;

    @Mock
    private ActivateEventRequestUseCase activateEventRequestUseCase;

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
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void shouldTrackAnExistingApprovedRequest() throws Exception {
        when(trackEventRequestUseCase.track(any())).thenReturn(new TrackEventRequestUseCase.TrackResult(
                "SOL-2026-A3F8",
                "approved",
                "Congresso Brasileiro de IA",
                List.of(
                        new TrackEventRequestUseCase.HistoryItem("requested", Instant.parse("2026-04-01T10:15:30Z"), "Event request submitted"),
                        new TrackEventRequestUseCase.HistoryItem("approved", Instant.parse("2026-04-02T11:15:30Z"), "Approved by admin")
                ),
                "http://localhost:4200/register?token=11111111-1111-1111-1111-111111111111",
                Instant.parse("2026-04-01T10:15:30Z")
        ));

        mockMvc.perform(post("/api/v1/public/event-requests/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrackEventRequestRequest("SOL-2026-A3F8", "ana.silva@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value("SOL-2026-A3F8"))
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.statusHistory.length()").value(2))
                .andExpect(jsonPath("$.activationUrl").value("http://localhost:4200/register?token=11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void shouldReturnNotFoundWhenTrackingCombinationDoesNotExist() throws Exception {
        when(trackEventRequestUseCase.track(any())).thenThrow(new ResourceNotFoundException("Event request", "tracking details"));

        mockMvc.perform(post("/api/v1/public/event-requests/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TrackEventRequestRequest("SOL-2026-XXXX", "missing@example.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldValidateActivationToken() throws Exception {
        UUID requestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID token = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(validateActivationTokenUseCase.validate(token)).thenReturn(
                new ValidateActivationTokenUseCase.ValidateResult(true, "ana.silva@example.com", "Congresso Brasileiro de IA", requestId, true)
        );

        mockMvc.perform(get("/api/v1/public/event-requests/activate").param("token", token.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("ana.silva@example.com"))
                .andExpect(jsonPath("$.userExists").value(true));
    }

    @Test
    void shouldActivateApprovedRequest() throws Exception {
        UUID token = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID eventId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        when(activateEventRequestUseCase.activate(any())).thenReturn(
                new ActivateEventRequestUseCase.ActivateResult(eventId, userId, "Ativação concluída com sucesso.", false)
        );

        mockMvc.perform(post("/api/v1/public/event-requests/activate")
                        .principal(new UsernamePasswordAuthenticationToken(new TestPrincipal(userId), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateEventRequestRequest(token, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("44444444-4444-4444-4444-444444444444"))
                .andExpect(jsonPath("$.userId").value("33333333-3333-3333-3333-333333333333"))
                .andExpect(jsonPath("$.userCreated").value(false));
    }

    private record TestPrincipal(UUID userId) {
    }
}