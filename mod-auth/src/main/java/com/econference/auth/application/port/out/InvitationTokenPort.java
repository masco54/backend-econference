package com.econference.auth.application.port.out;

public interface InvitationTokenPort {

    boolean validateReviewerToken(String token);

    boolean validateCoOrganizerToken(String token);
}
