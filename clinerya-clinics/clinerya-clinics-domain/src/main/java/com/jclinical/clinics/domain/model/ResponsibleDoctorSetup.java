package com.jclinical.clinics.domain.model;

import com.jclinical.core.domain.CedulaProfesional;

/**
 * Lo que se pide al dar de alta una clinica: quien es su medico responsable y que papel tiene el
 * titular de la cuenta. Nombre y cedula del responsable siempre son obligatorios.
 *
 * <ul>
 *   <li>El titular es el responsable: su cedula es la del responsable y atiende pacientes.</li>
 *   <li>El responsable es otra persona: el titular indica si atiende pacientes (con su propia
 *       cedula) o si solo gestiona la cuenta.</li>
 * </ul>
 */
public record ResponsibleDoctorSetup(
        boolean ownerIsResponsibleDoctor,
        String responsibleDoctorName,
        String responsibleDoctorProfessionalLicense,
        Boolean ownerAttendsPatients,
        String ownerCedulaProfesional
) {

    public Resolved resolve() {
        String name = responsibleDoctorName == null ? "" : responsibleDoctorName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("El nombre del médico responsable es obligatorio.");
        }
        String responsibleLicense = CedulaProfesional.require(
                responsibleDoctorProfessionalLicense, "La cédula profesional del médico responsable es obligatoria.");
        if (ownerIsResponsibleDoctor) {
            return new Resolved(name, responsibleLicense, true, responsibleLicense);
        }
        if (ownerAttendsPatients == null) {
            throw new IllegalArgumentException("Indica si atenderás pacientes o solo gestionarás la cuenta.");
        }
        String ownerCedula = ownerAttendsPatients ? CedulaProfesional.require(ownerCedulaProfesional) : null;
        return new Resolved(name, responsibleLicense, ownerAttendsPatients, ownerCedula);
    }

    public record Resolved(
            String responsibleDoctorName,
            String responsibleDoctorProfessionalLicense,
            boolean ownerAttendsPatients,
            String ownerCedulaProfesional
    ) {}
}
