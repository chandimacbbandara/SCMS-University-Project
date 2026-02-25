package Project._6.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String showForm() {
        return "submit-concern";
    }

    @GetMapping("/submit")
    public String showFormAgain() {
        return "submit-concern";
    }
}
