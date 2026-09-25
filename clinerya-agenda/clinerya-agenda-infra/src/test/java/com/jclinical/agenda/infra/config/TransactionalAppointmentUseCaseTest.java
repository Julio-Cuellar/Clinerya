package com.jclinical.agenda.infra.config;

import com.jclinical.agenda.domain.model.Appointment;
import com.jclinical.agenda.domain.model.AppointmentStatus;
import com.jclinical.agenda.domain.ports.in.ManageAppointmentsUseCase;
import com.jclinical.agenda.domain.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La agenda publica sus eventos en el outbox. Para que la cita y su evento se confirmen o se
 * reviertan juntos, cada caso de uso debe correr en una sola transaccion; sin esta envoltura cada
 * save confirmaba por su cuenta y un fallo posterior dejaba la cita sin evento, o al reves.
 */
class TransactionalAppointmentUseCaseTest {

    private static final List<String> READ_ONLY = List.of(
            "getAppointment", "listByClinicRange", "listByQuotation", "listByPatient",
            "listCompletedByClinic", "listDoctors", "listWithoutPatient");

    @Test
    void everyUseCaseRunsInsideATransaction() {
        for (Method contract : abstractMethods()) {
            Method implementation = implementationOf(contract);
            Transactional transactional = implementation.getAnnotation(Transactional.class);

            assertNotNull(transactional, contract.getName() + " debe ser @Transactional");
            assertEquals(READ_ONLY.contains(contract.getName()), transactional.readOnly(),
                    contract.getName() + ": solo las consultas son de solo lectura");
        }
    }

    @Test
    void replacesTheBareServiceForEveryConsumer() {
        assertTrue(TransactionalAppointmentUseCase.class.isAnnotationPresent(Primary.class),
                "controladores, caja e integraciones inyectan ManageAppointmentsUseCase y deben recibir la envoltura");
    }

    @Test
    void delegatesToTheDomainService() {
        AppointmentService service = mock(AppointmentService.class);
        TransactionalAppointmentUseCase useCase = new TransactionalAppointmentUseCase(service);
        UUID actingUserId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID clinicId = UUID.randomUUID();
        Appointment cancelled = new Appointment();
        when(service.transitionStatus(actingUserId, appointmentId, clinicId, AppointmentStatus.CANCELLED, "viaje", actingUserId))
                .thenReturn(cancelled);

        Appointment result = useCase.transitionStatus(actingUserId, appointmentId, clinicId,
                AppointmentStatus.CANCELLED, "viaje", actingUserId);

        assertSame(cancelled, result);
        verify(service).transitionStatus(actingUserId, appointmentId, clinicId, AppointmentStatus.CANCELLED, "viaje", actingUserId);
    }

    private static List<Method> abstractMethods() {
        List<Method> methods = Arrays.stream(ManageAppointmentsUseCase.class.getDeclaredMethods())
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .toList();
        assertFalse(methods.isEmpty());
        return methods;
    }

    private static Method implementationOf(Method contract) {
        try {
            return TransactionalAppointmentUseCase.class.getDeclaredMethod(contract.getName(), contract.getParameterTypes());
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(contract.getName() + " no esta implementado en la envoltura transaccional", exception);
        }
    }
}
