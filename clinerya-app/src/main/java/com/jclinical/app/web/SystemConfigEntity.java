package com.jclinical.app.web;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_configs", schema = "app")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfigEntity {

    @Id
    private String key;

    @Column(nullable = false)
    private String value;

    private String description;
}
