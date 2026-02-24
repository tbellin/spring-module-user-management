package org.jbelt.module.shared.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final Environment environment;

    @Autowired
    public HomeController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/")
    public String home(Model model) {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfile = activeProfiles.length > 0 ? activeProfiles[0] : "default";
        model.addAttribute("activeProfile", activeProfile);
        return "index";
    }
}
