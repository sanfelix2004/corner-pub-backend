package com.corner.pub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendForwardController {

    @GetMapping("/cameriere")
    public String forwardCameriere() {
        return "forward:/cameriere.html";
    }

    @GetMapping("/cucina")
    public String forwardCucina() {
        return "forward:/cucina.html";
    }
}
