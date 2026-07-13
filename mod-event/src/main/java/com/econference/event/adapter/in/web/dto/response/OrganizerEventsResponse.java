package com.econference.event.adapter.in.web.dto.response;

import java.util.List;

public record OrganizerEventsResponse(
        List<EventSummaryResponse> data,
        PaginationResponse pagination
) {

    public record PaginationResponse(
            int page,
            int limit,
            long total,
            int totalPages
    ) {
    }
}
