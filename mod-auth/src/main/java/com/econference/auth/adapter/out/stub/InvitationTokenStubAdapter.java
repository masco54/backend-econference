package com.econference.auth.adapter.out.stub;

import com.econference.auth.application.port.out.InvitationTokenPort;
import org.springframework.stereotype.Component;

@Component
public class InvitationTokenStubAdapter implements InvitationTokenPort {

    @Override
    public boolean validateReviewerToken(String token) {
        return false;
    }

    @Override
    public boolean validateCoOrganizerToken(String token) {
        return false;
    }
}
