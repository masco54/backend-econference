package com.econference.event.application.port.in;

import com.econference.event.domain.EventRequest;

import java.util.UUID;

public interface GetAdminEventRequestDetailUseCase {

    EventRequest get(UUID requestId);
}