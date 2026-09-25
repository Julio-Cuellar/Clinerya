package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.AppointmentRequest;

/** Avisa al medico que tiene una solicitud nueva en su bandeja. */
public interface DoctorAlertPort {

    void newRequest(AppointmentRequest request);
}
