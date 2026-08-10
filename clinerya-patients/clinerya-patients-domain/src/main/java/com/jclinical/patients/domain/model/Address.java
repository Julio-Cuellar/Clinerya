package com.jclinical.patients.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Address {
    String street;
    String outdoorNumber;
    String indoorNumber;
    String colonia;
    String municipality;
    String state;
    String zipCode;
}
