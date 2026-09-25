package com.jclinical.app.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/info")
public class AppInfoController {

    @GetMapping
    public AppInfoResponse getInfo() {
        return new AppInfoResponse("Clinerya API", "0.10.0-beta.1", "beta", "UP");
    }

    public record AppInfoResponse(
        String name,
        String version,
        String environment,
        String status
    ) {}
}
