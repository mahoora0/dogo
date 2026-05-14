package com.example.dogo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    @GetMapping("")
    public String dashboard(Model model) {
        // 통계 데이터 (실제 서비스에서는 DB에서 조회해야 함)
        model.addAttribute("totalUsers", 1250);
        model.addAttribute("lostItems", 45);
        model.addAttribute("foundItems", 32);
        model.addAttribute("matchSuccess", 12);

        // 최근 활동 내역 (Mock 데이터)
        List<Map<String, String>> recentActivities = new ArrayList<>();
        recentActivities.add(createActivity("분실", "아이폰 15 프로", "강남역", "2024-05-14"));
        recentActivities.add(createActivity("습득", "갈색 가죽 지갑", "홍대입구역", "2024-05-14"));
        recentActivities.add(createActivity("매칭", "에어팟 프로 2", "잠실역", "2024-05-13"));
        
        model.addAttribute("activities", recentActivities);

        return "admin/index";
    }

    private Map<String, String> createActivity(String type, String item, String location, String date) {
        Map<String, String> activity = new HashMap<>();
        activity.put("type", type);
        activity.put("item", item);
        activity.put("location", location);
        activity.put("date", date);
        return activity;
    }
}
