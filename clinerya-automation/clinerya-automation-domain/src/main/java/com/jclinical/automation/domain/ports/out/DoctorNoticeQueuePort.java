package com.jclinical.automation.domain.ports.out;

import com.jclinical.automation.domain.model.DoctorNotice;

/** Cola de salida para avisos al medico; el despachador los envia con la plantilla de medicos. */
public interface DoctorNoticeQueuePort {

    void enqueue(DoctorNotice notice);
}
