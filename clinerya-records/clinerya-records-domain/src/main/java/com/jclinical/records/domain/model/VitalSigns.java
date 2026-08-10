package com.jclinical.records.domain.model;

public record VitalSigns(
    Double temperature,
    String bloodPressure,
    Integer heartRate,
    Integer respiratoryRate,
    Double weight,
    Double height,
    Double bmi,
    Integer oxygenSaturation
) {
    public static VitalSigns create(Double temperature, String bloodPressure, Integer heartRate,
                                   Integer respiratoryRate, Double weight, Double height, Integer oxygenSaturation) {
        Double bmi = null;
        if (weight != null && height != null && height > 0) {
            double rawBmi = weight / (height * height);
            bmi = Math.round(rawBmi * 100.0) / 100.0;
        }
        return new VitalSigns(temperature, bloodPressure, heartRate, respiratoryRate, weight, height, bmi, oxygenSaturation);
    }
}
