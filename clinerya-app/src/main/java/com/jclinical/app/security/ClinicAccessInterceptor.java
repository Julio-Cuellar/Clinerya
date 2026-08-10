package com.jclinical.app.security;

import com.jclinical.core.security.ClinicAccessDeniedException;
import com.jclinical.core.security.ClinicMembershipPort;
import com.jclinical.users.infra.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClinicAccessInterceptor implements HandlerInterceptor {

    private final CurrentUserResolver currentUserResolver;
    private final ClinicMembershipPort clinicMembershipPort;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> rawVars)) {
            return true;
        }
        Map<String, String> pathVariables = (Map<String, String>) rawVars;
        String clinicIdValue = pathVariables.get("clinicId");
        if (clinicIdValue == null) {
            return true;
        }

        UUID clinicId = UUID.fromString(clinicIdValue);
        UUID userId = currentUserResolver.getCurrentUserId();

        if (!clinicMembershipPort.isActiveStaffMember(userId, clinicId)) {
            throw new ClinicAccessDeniedException("No perteneces al personal de esta clínica.");
        }

        return true;
    }
}
