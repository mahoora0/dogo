package com.example.dogo.controller.chat;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

	@GetMapping("/chat")
	public String chat(Model model) {
		model.addAttribute("currentUri", "/chat");
		return "chat/index";
	}
}
