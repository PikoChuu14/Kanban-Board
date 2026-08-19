package com.company.kanban.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("prod")
public class SpaFallbackController {
    @GetMapping({
            "/",
            "/login",
            "/dashboard",
            "/projects",
            "/reports",
            "/history",
            "/admin",
            "/admin/users",
            "/admin/settings/data-management",
            "/activate",
            "/manager",
            "/staff"
    })
    public String index() {
        return "forward:/index.html";
    }
}
