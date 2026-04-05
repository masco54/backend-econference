package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.request.AddEventRequestNoteRequest;
import com.econference.event.adapter.in.web.dto.request.ApproveEventRequestRequest;
import com.econference.event.adapter.in.web.dto.request.RejectEventRequestRequest;
import com.econference.event.application.port.in.AddEventRequestNoteUseCase;
import com.econference.event.application.port.in.ApproveEventRequestUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestDetailUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestsUseCase;
import com.econference.event.application.port.in.ReissueActivationTokenUseCase;
import com.econference.event.application.port.in.RejectEventRequestUseCase;
import com.econference.event.config.EventExceptionHandler;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.shared.exception.GlobalExceptionHandler;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventRequestAdminControllerIT {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private GetAdminEventRequestsUseCase getAdminEventRequestsUseCase;

    @Mock
    private GetAdminEventRequestDetailUseCase getAdminEventRequestDetailUseCase;

    @Mock
    private AddEventRequestNoteUseCase addEventRequestNoteUseCase;

    @Mock
    private ApproveEventRequestUseCase approveEventRequestUseCase;

    @Mock
    private RejectEventRequestUseCase rejectEventRequestUseCase;

    @Mock
    private ReissueActivationTokenUseCase reissueActivationTokenUseCase;

    @InjectMocks
    private EventRequestAdminController eventRequestAdminController;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(eventRequestAdminController)
                .setControllerAdvice(new GlobalExceptionHandler(), new EventExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void shouldListAdminRequests() throws Exception {
        EventRequest request = approvedRequest();
        when(getAdminEventRequestsUseCase.get(any())).thenReturn(new GetAdminEventRequestsUseCase.AdminListResult(
                List.of(request),
                new GetAdminEventRequestsUseCase.Pagination(1, 10, 1, 1)
        ));

        mockMvc.perform(get("/admin/event-requests")
                        .param("status", "approved")
                        .param("search", "ana")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].trackingCode").value("SOL-2026-A3F8"))
                .andExpect(jsonPath("$.data[0].status").value("approved"))
                .andExpect(jsonPath("$.pagination.page").value(1))
                .andExpect(jsonPath("$.pagination.total").value(1));
    }

    @Test
    void shouldGetAdminRequestDetail() throws Exception {
        EventRequest request = approvedRequest();
        when(getAdminEventRequestDetailUseCase.get(request.getId())).thenReturn(request);

        mockMvc.perform(get("/admin/event-requests/{requestId}", request.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(request.getId().toString()))
                .andExpect(jsonPath("$.statusHistory.length()").value(2))
                .andExpect(jsonPath("$.linkedEventId").value(request.getLinkedEventId().toString()));
    }

    @Test
    void shouldAddAdministrativeNote() throws Exception {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = underReviewRequest();
        when(addEventRequestNoteUseCase.addNote(any())).thenReturn(request);

        mockMvc.perform(post("/admin/event-requests/{requestId}/add-note", request.getId())
                        .principal(new UsernamePasswordAuthenticationToken(new TestPrincipal(adminUserId), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddEventRequestNoteRequest("Precisamos confirmar o número de trilhas."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("under_review"));
    }

    @Test
    void shouldApproveRequest() throws Exception {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = approvedRequest();
        when(approveEventRequestUseCase.approve(any())).thenReturn(request);

        mockMvc.perform(post("/admin/event-requests/{requestId}/approve", request.getId())
                        .principal(new UsernamePasswordAuthenticationToken(new TestPrincipal(adminUserId), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveEventRequestRequest("Solicitação aderente ao calendário institucional."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.activationToken").value(request.getActivationToken().toString()));
    }

    @Test
    void shouldRejectRequest() throws Exception {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = rejectedRequest();
        when(rejectEventRequestUseCase.reject(any())).thenReturn(request);

        mockMvc.perform(post("/admin/event-requests/{requestId}/reject", request.getId())
                        .principal(new UsernamePasswordAuthenticationToken(new TestPrincipal(adminUserId), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectEventRequestRequest("O período informado conflita com eventos já homologados."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.rejectionReason").value("O período informado conflita com eventos já homologados."));
    }

    @Test
    void shouldReissueActivationToken() throws Exception {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = approvedRequest();
        when(reissueActivationTokenUseCase.reissue(any())).thenReturn(request);

        mockMvc.perform(post("/admin/event-requests/{requestId}/reissue-token", request.getId())
                        .principal(new UsernamePasswordAuthenticationToken(new TestPrincipal(adminUserId), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activationToken").value(request.getActivationToken().toString()));
    }

    private EventRequest underReviewRequest() {
        EventRequest request = baseRequest(EventRequestStatus.UNDER_REVIEW);
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        request.addHistoryEntry(EventRequestStatus.UNDER_REVIEW, "Solicitação em análise", UUID.fromString("88888888-8888-8888-8888-888888888888"));
        return request;
    }

    private EventRequest approvedRequest() {
        EventRequest request = baseRequest(EventRequestStatus.APPROVED);
        request.setAdminNotes("Solicitação aderente ao calendário institucional.");
        request.setActivationToken(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        request.setActivationTokenExpiresAt(Instant.parse("2026-04-10T12:00:00Z"));
        request.setLinkedEventId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        request.addHistoryEntry(EventRequestStatus.APPROVED, "Approved by admin", UUID.fromString("88888888-8888-8888-8888-888888888888"));
        return request;
    }

    private EventRequest rejectedRequest() {
        EventRequest request = baseRequest(EventRequestStatus.REJECTED);
        request.setRejectionReason("O período informado conflita com eventos já homologados.");
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        request.addHistoryEntry(EventRequestStatus.REJECTED, request.getRejectionReason(), UUID.fromString("88888888-8888-8888-8888-888888888888"));
        return request;
    }

    private EventRequest baseRequest(EventRequestStatus status) {
        EventRequest request = EventRequest.builder()
                .trackingCode("SOL-2026-A3F8")
                .status(status)
                .requesterFirstName("Ana")
                .requesterLastName("Silva")
                .requesterName("Ana Silva")
                .requesterEmail("ana.silva@example.com")
                .requesterPhone("+5511999999999")
                .requesterInstitution("UFSC")
                .eventName("Congresso Brasileiro de IA")
                .eventTheme("Aplicações de IA na pesquisa científica")
                .eventStartDate(LocalDate.of(2026, 9, 10))
                .eventEndDate(LocalDate.of(2026, 9, 12))
                .eventLocation("Florianópolis")
                .eventType("conference")
                .hasRegistration(true)
                .hasSubmission(true)
                .hasSite(true)
                .build();
        setEntityField(request, "id", UUID.fromString("99999999-9999-9999-9999-999999999999"));
        setEntityField(request, "createdAt", Instant.parse("2026-04-01T10:15:30Z"));
        setEntityField(request, "updatedAt", Instant.parse("2026-04-02T10:15:30Z"));
        return request;
    }

    private void setEntityField(Object target, String fieldName, Object value) {
        try {
            Class<?> current = target.getClass();
            while (current != null) {
                try {
                    var field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }

    private record TestPrincipal(UUID userId) {
    }
}