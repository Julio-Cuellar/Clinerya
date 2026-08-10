package com.jclinical.patients.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmergencyContact {
    String fullName;
    String relationship;
    String phone;
}
