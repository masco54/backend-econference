package com.econference.event.application.port.in;

import com.econference.event.domain.EventRequest;

import java.util.List;

public interface GetAdminEventRequestsUseCase {

    AdminListResult get(AdminListCommand command);

    record AdminListCommand(String status, String search, int page, int limit) {
    }

    record AdminListResult(List<EventRequest> data, Pagination pagination) {
    }

    record Pagination(int page, int limit, long total, int totalPages) {
    }
}