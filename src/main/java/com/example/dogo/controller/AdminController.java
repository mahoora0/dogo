package com.example.dogo.controller;

import com.example.dogo.entity.animal.AnimalReport;
import com.example.dogo.entity.missing.MissingPersonReport;
import com.example.dogo.entity.item.LostItem;
import com.example.dogo.entity.item.FoundItem;
import com.example.dogo.repository.animal.AnimalReportRepository;
import com.example.dogo.repository.missing.MissingPersonRepository;
import com.example.dogo.repository.item.LostItemRepository;
import com.example.dogo.repository.item.FoundItemRepository;
import com.example.dogo.repository.user.UserRepository;
import com.example.dogo.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnimalReportRepository animalReportRepository;
    private final MissingPersonRepository missingPersonRepository;
    private final LostItemRepository lostItemRepository;
    private final FoundItemRepository foundItemRepository;
    private final UserRepository userRepository;

    @GetMapping("")
    public String dashboard(Model model) {
        // 실제 데이터베이스에서 건수 조회하여 바인딩
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("lostItems", lostItemRepository.count());
        model.addAttribute("foundItems", foundItemRepository.count());
        model.addAttribute("matchSuccess", animalReportRepository.count() + missingPersonRepository.count());

        // 최근 활동 내역 (실제 데이터베이스 실시간 조회)
        List<Map<String, String>> recentActivities = new ArrayList<>();
        
        List<LostItem> latestLost = lostItemRepository.findByDeletedFalseOrderByRegDateDesc();
        List<FoundItem> latestFound = foundItemRepository.findByDeletedFalseOrderByRegDateDesc();
        
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        
        // 각각 최대 3개씩 조합하여 리스트업
        int lostLimit = Math.min(3, latestLost.size());
        for (int i = 0; i < lostLimit; i++) {
            LostItem item = latestLost.get(i);
            recentActivities.add(createActivity("분실", item.getItemName(), item.getLostPlace(), 
                item.getRegDate() != null ? item.getRegDate().format(formatter) : ""));
        }
        
        int foundLimit = Math.min(3, latestFound.size());
        for (int i = 0; i < foundLimit; i++) {
            FoundItem item = latestFound.get(i);
            recentActivities.add(createActivity("습득", item.getItemName(), item.getFoundPlace() != null ? item.getFoundPlace() : "경찰 보관소", 
                item.getRegDate() != null ? item.getRegDate().format(formatter) : ""));
        }
        
        // 등록일 내림차순(최신순) 정렬
        recentActivities.sort((a, b) -> b.get("date").compareTo(a.get("date")));
        
        // 최대 5개까지만 노출
        if (recentActivities.size() > 5) {
            recentActivities = recentActivities.subList(0, 5);
        }
        
        model.addAttribute("activities", recentActivities);
        return "admin/index";
    }

    // --- 분실물 관리 ---
    @GetMapping("/lost-items")
    public String listLostItems(@RequestParam(value = "sourceType", required = false) String sourceType, Model model) {
        List<LostItem> items;
        if (sourceType != null && ("USER".equals(sourceType) || "POLICE".equals(sourceType))) {
            items = lostItemRepository.findBySourceTypeAndDeletedFalseOrderByRegDateDesc(sourceType);
        } else {
            items = lostItemRepository.findByDeletedFalseOrderByRegDateDesc();
            sourceType = "ALL";
        }
        model.addAttribute("items", items);
        model.addAttribute("sourceType", sourceType);
        return "admin/lost-items";
    }

    @PostMapping("/lost-items/{id}/status")
    @Transactional
    public String updateLostItemStatus(@PathVariable("id") Long id, 
                                       @RequestParam("status") String status,
                                       @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setStatus(status);
        lostItemRepository.save(item);
        return "redirect:/admin/lost-items?sourceType=" + sourceType;
    }

    @PostMapping("/lost-items/{id}/delete")
    @Transactional
    public String deleteLostItem(@PathVariable("id") Long id,
                                 @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setDeleted(true);
        lostItemRepository.save(item);
        return "redirect:/admin/lost-items?sourceType=" + sourceType;
    }

    // --- 습득물 관리 ---
    @GetMapping("/found-items")
    public String listFoundItems(@RequestParam(value = "sourceType", required = false) String sourceType, Model model) {
        List<FoundItem> items;
        if (sourceType != null && ("USER".equals(sourceType) || "POLICE".equals(sourceType))) {
            items = foundItemRepository.findBySourceTypeAndDeletedFalseOrderByRegDateDesc(sourceType);
        } else {
            items = foundItemRepository.findByDeletedFalseOrderByRegDateDesc();
            sourceType = "ALL";
        }
        model.addAttribute("items", items);
        model.addAttribute("sourceType", sourceType);
        return "admin/found-items";
    }

    @PostMapping("/found-items/{id}/status")
    @Transactional
    public String updateFoundItemStatus(@PathVariable("id") Long id, 
                                        @RequestParam("status") String status,
                                        @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType) {
        FoundItem item = foundItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setStatus(status);
        foundItemRepository.save(item);
        return "redirect:/admin/found-items?sourceType=" + sourceType;
    }

    @PostMapping("/found-items/{id}/delete")
    @Transactional
    public String deleteFoundItem(@PathVariable("id") Long id,
                                  @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType) {
        FoundItem item = foundItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setDeleted(true);
        foundItemRepository.save(item);
        return "redirect:/admin/found-items?sourceType=" + sourceType;
    }

    // --- 실종동물 관리 ---
    @GetMapping("/animals")
    public String listAnimals(Model model) {
        List<AnimalReport> reports = animalReportRepository.findByDeletedFalseOrderByRegdateDesc();
        model.addAttribute("reports", reports);
        return "admin/animals";
    }

    @PostMapping("/animals/{id}/status")
    @Transactional
    public String updateAnimalStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        AnimalReport report = animalReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setStatus(status);
        animalReportRepository.save(report);
        return "redirect:/admin/animals";
    }

    @PostMapping("/animals/{id}/delete")
    @Transactional
    public String deleteAnimal(@PathVariable("id") Long id) {
        AnimalReport report = animalReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setDeleted(true);
        animalReportRepository.save(report);
        return "redirect:/admin/animals";
    }

    // --- 실종자 관리 ---
    @GetMapping("/missing-persons")
    public String listMissingPersons(Model model) {
        List<MissingPersonReport> reports = missingPersonRepository.findByDeletedFalseOrderByRegdateDesc();
        model.addAttribute("reports", reports);
        return "admin/missing-persons";
    }

    @PostMapping("/missing-persons/{id}/status")
    @Transactional
    public String updateMissingPersonStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        MissingPersonReport report = missingPersonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setStatus(status);
        missingPersonRepository.save(report);
        return "redirect:/admin/missing-persons";
    }

    @PostMapping("/missing-persons/{id}/delete")
    @Transactional
    public String deleteMissingPerson(@PathVariable("id") Long id) {
        MissingPersonReport report = missingPersonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setDeleted(true);
        missingPersonRepository.save(report);
        return "redirect:/admin/missing-persons";
    }

    // --- 회원 관리 ---
    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAllByOrderByUserNoDesc();
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    @Transactional
    public String updateUserRole(@PathVariable("id") Long id, @RequestParam("role") String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        user.setRole(role);
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/status")
    @Transactional
    public String updateUserStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id));
        user.setStatus(status);
        userRepository.save(user);
        return "redirect:/admin/users";
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
