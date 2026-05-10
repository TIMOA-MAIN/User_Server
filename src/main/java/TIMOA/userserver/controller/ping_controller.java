package TIMOA.userserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ping_controller {
    @GetMapping("/health")
    public ResponseEntity<String> health_check() {
        return ResponseEntity.ok("OK");
    }
}
