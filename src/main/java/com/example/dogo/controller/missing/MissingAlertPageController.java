package com.example.dogo.controller.missing;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MissingAlertPageController {

	@GetMapping("/missing-alerts")
	public String alerts(Model model) {
		model.addAttribute("currentUri", "/missing-alerts");
		return "missing-persons/alerts";
	}
}
