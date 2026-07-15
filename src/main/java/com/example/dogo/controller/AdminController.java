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
import com.example.dogo.repository.Support.InquiryRepository;
import com.example.dogo.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Set<String> LOST_ITEM_STATUSES = Set.of("WAITING", "FOUND");
    private static final Set<String> FOUND_ITEM_STATUSES = Set.of("KEEPING", "RETURNED", "TRANSFERRED");

    private final AnimalReportRepository animalReportRepository;
    private final MissingPersonRepository missingPersonRepository;
    private final LostItemRepository lostItemRepository;
    private final FoundItemRepository foundItemRepository;
    private final UserRepository userRepository;
    private final InquiryRepository inquiryRepository;

    private static boolean emergencyActive = false;
    private static boolean locationWeightEnabled = true;
    private static boolean keywordFilterEnabled = true;

    public static boolean isLocationWeightEnabled() {
        return locationWeightEnabled;
    }

    public static boolean isKeywordFilterEnabled() {
        return keywordFilterEnabled;
    }

    @GetMapping("")
    public String dashboard(Model model) {
        // 실제 데이터베이스에서 건수 조회하여 바인딩
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("lostItems", lostItemRepository.count());
        model.addAttribute("foundItems", foundItemRepository.count());
        model.addAttribute("matchSuccess", animalReportRepository.count() + missingPersonRepository.count());
        model.addAttribute("unansweredInquiriesCount", inquiryRepository.countByStatusNot("ANSWERED"));
        model.addAttribute("emergencyActive", emergencyActive);
        model.addAttribute("locationWeightEnabled", locationWeightEnabled);
        model.addAttribute("keywordFilterEnabled", keywordFilterEnabled);

        // 실시간 대표 유실물 분포 비율 연산
        long totalLostCount = lostItemRepository.countByDeletedFalse();
        double electronicsRatio = 42.0;
        double walletRatio = 35.0;
        double otherRatio = 23.0;

        if (totalLostCount > 0) {
            long electronicsCount = lostItemRepository.countByDeletedFalseAndCategoryMainIn(java.util.List.of("전자기기", "휴대폰"));
            long walletCount = lostItemRepository.countByDeletedFalseAndCategoryMainIn(java.util.List.of("지갑", "카드/증명서"));
            long otherCount = Math.max(0, totalLostCount - (electronicsCount + walletCount));

            electronicsRatio = Math.round((double) electronicsCount / totalLostCount * 100);
            walletRatio = Math.round((double) walletCount / totalLostCount * 100);
            otherRatio = Math.max(0, 100 - (electronicsRatio + walletRatio));
        }

        model.addAttribute("electronicsRatio", (int) electronicsRatio);
        model.addAttribute("walletRatio", (int) walletRatio);
        model.addAttribute("otherRatio", (int) otherRatio);

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
    public String listLostItems(@RequestParam(value = "sourceType", required = false) String sourceType,
                                @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10);
        org.springframework.data.domain.Page<LostItem> itemPage;

        final String finalSourceType = sourceType;
        org.springframework.data.jpa.domain.Specification<LostItem> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));

            if (finalSourceType != null && ("USER".equals(finalSourceType) || "POLICE".equals(finalSourceType))) {
                predicates.add(cb.equal(root.get("sourceType"), finalSourceType));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.trim() + "%";
                if ("title".equals(searchType)) {
                    predicates.add(cb.like(root.get("title"), searchKeyword));
                } else if ("itemName".equals(searchType)) {
                    predicates.add(cb.like(root.get("itemName"), searchKeyword));
                } else if ("place".equals(searchType)) {
                    predicates.add(cb.or(
                        cb.like(root.get("lostArea"), searchKeyword),
                        cb.like(root.get("lostPlace"), searchKeyword)
                    ));
                } else {
                    // 전체 (기본값)
                    predicates.add(cb.or(
                        cb.like(root.get("title"), searchKeyword),
                        cb.like(root.get("itemName"), searchKeyword),
                        cb.like(root.get("lostArea"), searchKeyword),
                        cb.like(root.get("lostPlace"), searchKeyword)
                    ));
                }
            }

            query.orderBy(cb.desc(root.get("regDate")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        itemPage = lostItemRepository.findAll(spec, pageable);

        if (sourceType == null) {
            sourceType = "ALL";
        }

        model.addAttribute("items", itemPage);
        model.addAttribute("page", itemPage);
        model.addAttribute("sourceType", sourceType);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/lost-items";
    }

    @PostMapping("/lost-items/{id}/status")
    @Transactional
    public String updateLostItemStatus(@PathVariable("id") Long id, 
                                       @RequestParam("status") String status,
                                       @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType,
                                       @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                       @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                       @RequestParam(value = "statusFilter", required = false, defaultValue = "") String statusFilter,
                                       @RequestParam(value = "page", defaultValue = "0") int page) {
        validateStatus(status, LOST_ITEM_STATUSES);
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setStatus(status);
        lostItemRepository.save(item);
        return "redirect:/admin/lost-items?sourceType=" + sourceType + "&searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&status=" + statusFilter + "&page=" + page;
    }

    @PostMapping("/lost-items/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteLostItem(@PathVariable("id") Long id,
                                 @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType,
                                 @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                 @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                 @RequestParam(value = "statusFilter", required = false, defaultValue = "") String statusFilter,
                                 @RequestParam(value = "page", defaultValue = "0") int page) {
        LostItem item = lostItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setDeleted(true);
        lostItemRepository.save(item);
        return "redirect:/admin/lost-items?sourceType=" + sourceType + "&searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&status=" + statusFilter + "&page=" + page;
    }

    // --- 습득물 관리 ---
    @GetMapping("/found-items")
    public String listFoundItems(@RequestParam(value = "sourceType", required = false) String sourceType,
                                 @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                 @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                 @RequestParam(value = "status", required = false) String status,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10);
        org.springframework.data.domain.Page<FoundItem> itemPage;

        final String finalSourceType = sourceType;
        org.springframework.data.jpa.domain.Specification<FoundItem> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));

            if (finalSourceType != null && ("USER".equals(finalSourceType) || "POLICE".equals(finalSourceType))) {
                predicates.add(cb.equal(root.get("sourceType"), finalSourceType));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.trim() + "%";
                if ("title".equals(searchType)) {
                    predicates.add(cb.like(root.get("title"), searchKeyword));
                } else if ("itemName".equals(searchType)) {
                    predicates.add(cb.like(root.get("itemName"), searchKeyword));
                } else if ("place".equals(searchType)) {
                    predicates.add(cb.or(
                        cb.like(root.get("foundArea"), searchKeyword),
                        cb.like(root.get("foundPlace"), searchKeyword)
                    ));
                } else {
                    predicates.add(cb.or(
                        cb.like(root.get("title"), searchKeyword),
                        cb.like(root.get("itemName"), searchKeyword),
                        cb.like(root.get("foundArea"), searchKeyword),
                        cb.like(root.get("foundPlace"), searchKeyword)
                    ));
                }
            }

            query.orderBy(cb.desc(root.get("regDate")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        itemPage = foundItemRepository.findAll(spec, pageable);

        if (sourceType == null) {
            sourceType = "ALL";
        }

        model.addAttribute("items", itemPage);
        model.addAttribute("page", itemPage);
        model.addAttribute("sourceType", sourceType);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/found-items";
    }

    @PostMapping("/found-items/{id}/status")
    @Transactional
    public String updateFoundItemStatus(@PathVariable("id") Long id, 
                                        @RequestParam("status") String status,
                                        @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType,
                                        @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                        @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                        @RequestParam(value = "statusFilter", required = false, defaultValue = "") String statusFilter,
                                        @RequestParam(value = "page", defaultValue = "0") int page) {
        validateStatus(status, FOUND_ITEM_STATUSES);
        FoundItem item = foundItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setStatus(status);
        foundItemRepository.save(item);
        return "redirect:/admin/found-items?sourceType=" + sourceType + "&searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&status=" + statusFilter + "&page=" + page;
    }

    private void validateStatus(String status, Set<String> allowedStatuses) {
        if (!allowedStatuses.contains(status)) {
            throw new IllegalArgumentException("Invalid item status: " + status);
        }
    }

    @PostMapping("/found-items/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteFoundItem(@PathVariable("id") Long id,
                                  @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType,
                                  @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                  @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                  @RequestParam(value = "statusFilter", required = false, defaultValue = "") String statusFilter,
                                  @RequestParam(value = "page", defaultValue = "0") int page) {
        FoundItem item = foundItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid item Id:" + id));
        item.setDeleted(true);
        foundItemRepository.save(item);
        return "redirect:/admin/found-items?sourceType=" + sourceType + "&searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&status=" + statusFilter + "&page=" + page;
    }

    // --- 실종동물 관리 ---
    @GetMapping("/animals")
    public String listAnimals(@RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                              @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                              @RequestParam(value = "reportType", required = false) String reportType,
                              @RequestParam(value = "page", defaultValue = "0") int page, 
                              Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10);
        org.springframework.data.domain.Page<AnimalReport> reportPage;

        org.springframework.data.jpa.domain.Specification<AnimalReport> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));

            if (reportType != null && !reportType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("reportType"), reportType));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.trim() + "%";
                if ("title".equals(searchType)) {
                    predicates.add(cb.like(root.get("title"), searchKeyword));
                } else if ("animalType".equals(searchType)) {
                    predicates.add(cb.like(root.get("animalType"), searchKeyword));
                } else if ("place".equals(searchType)) {
                    predicates.add(cb.or(
                        cb.like(root.get("regionName"), searchKeyword),
                        cb.like(root.get("detailPlace"), searchKeyword)
                    ));
                } else {
                    predicates.add(cb.or(
                        cb.like(root.get("title"), searchKeyword),
                        cb.like(root.get("animalType"), searchKeyword),
                        cb.like(root.get("regionName"), searchKeyword),
                        cb.like(root.get("detailPlace"), searchKeyword)
                    ));
                }
            }

            query.orderBy(cb.desc(root.get("regdate")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        reportPage = animalReportRepository.findAll(spec, pageable);
        model.addAttribute("reports", reportPage);
        model.addAttribute("page", reportPage);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("reportType", reportType);
        return "admin/animals";
    }

    @PostMapping("/animals/{id}/status")
    @Transactional
    public String updateAnimalStatus(@PathVariable("id") Long id, 
                                     @RequestParam("reportType") String reportType,
                                     @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                     @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                     @RequestParam(value = "reportTypeFilter", required = false, defaultValue = "") String reportTypeFilter,
                                     @RequestParam(value = "page", defaultValue = "0") int page) {
        AnimalReport report = animalReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setReportType(reportType);
        animalReportRepository.save(report);
        return "redirect:/admin/animals?searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&reportType=" + reportTypeFilter + "&page=" + page;
    }

    @PostMapping("/animals/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteAnimal(@PathVariable("id") Long id,
                               @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                               @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                               @RequestParam(value = "reportTypeFilter", required = false, defaultValue = "") String reportTypeFilter,
                               @RequestParam(value = "page", defaultValue = "0") int page) {
        AnimalReport report = animalReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setDeleted(true);
        animalReportRepository.save(report);
        return "redirect:/admin/animals?searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&reportType=" + reportTypeFilter + "&page=" + page;
    }

    // --- 실종자 관리 ---
    @GetMapping("/missing-persons")
    public String listMissingPersons(@RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                     @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                     @RequestParam(value = "status", required = false) String status,
                                     @RequestParam(value = "page", defaultValue = "0") int page, 
                                     Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10);
        org.springframework.data.domain.Page<MissingPersonReport> reportPage;

        org.springframework.data.jpa.domain.Specification<MissingPersonReport> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.trim() + "%";
                if ("clothing".equals(searchType)) {
                    predicates.add(cb.like(root.get("clothing"), searchKeyword));
                } else if ("place".equals(searchType)) {
                    predicates.add(cb.like(root.get("occurredPlace"), searchKeyword));
                } else {
                    predicates.add(cb.or(
                        cb.like(root.get("clothing"), searchKeyword),
                        cb.like(root.get("occurredPlace"), searchKeyword),
                        cb.like(root.get("nationality"), searchKeyword)
                    ));
                }
            }

            query.orderBy(cb.desc(root.get("regdate")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        reportPage = missingPersonRepository.findAll(spec, pageable);
        model.addAttribute("reports", reportPage);
        model.addAttribute("page", reportPage);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        return "admin/missing-persons";
    }

    @PostMapping("/missing-persons/{id}/status")
    @Transactional
    public String updateMissingPersonStatus(@PathVariable("id") Long id, 
                                            @RequestParam("status") String status,
                                            @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                            @RequestParam(value = "statusFilter", required = false, defaultValue = "") String statusFilter,
                                            @RequestParam(value = "page", defaultValue = "0") int page) {
        MissingPersonReport report = missingPersonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setStatus(status);
        missingPersonRepository.save(report);
        return "redirect:/admin/missing-persons?searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&status=" + statusFilter + "&page=" + page;
    }

    @PostMapping("/missing-persons/{id}/delete")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public String deleteMissingPerson(@PathVariable("id") Long id,
                                      @RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                                      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                                      @RequestParam(value = "statusFilter", required = false, defaultValue = "") String statusFilter,
                                      @RequestParam(value = "page", defaultValue = "0") int page) {
        MissingPersonReport report = missingPersonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid report Id:" + id));
        report.setDeleted(true);
        missingPersonRepository.save(report);
        return "redirect:/admin/missing-persons?searchType=" + searchType + "&keyword=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&status=" + statusFilter + "&page=" + page;
    }

    // --- 회원 관리 ---
    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "searchType", required = false, defaultValue = "") String searchType,
                            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
                            @RequestParam(value = "page", defaultValue = "0") int page, 
                            Model model) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, 10);
        org.springframework.data.domain.Page<User> userPage;

        org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKeyword = "%" + keyword.trim() + "%";
                if ("loginId".equals(searchType)) {
                    predicates.add(cb.like(root.get("loginId"), searchKeyword));
                } else if ("nickname".equals(searchType)) {
                    predicates.add(cb.like(root.get("nickname"), searchKeyword));
                } else if ("email".equals(searchType)) {
                    predicates.add(cb.like(root.get("email"), searchKeyword));
                } else {
                    predicates.add(cb.or(
                        cb.like(root.get("loginId"), searchKeyword),
                        cb.like(root.get("nickname"), searchKeyword),
                        cb.like(root.get("email"), searchKeyword)
                    ));
                }
            }

            query.orderBy(cb.desc(root.get("userNo")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        userPage = userRepository.findAll(spec, pageable);
        model.addAttribute("users", userPage);
        model.addAttribute("page", userPage);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
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

    @PostMapping("/api/emergency/toggle")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, Object> toggleEmergency() {
        emergencyActive = !emergencyActive;
        Map<String, Object> response = new HashMap<>();
        response.put("active", emergencyActive);
        return response;
    }

    @PostMapping("/api/settings/toggle")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public Map<String, Object> toggleSetting(@RequestParam("key") String key, @RequestParam("value") boolean value) {
        if ("location_weight_enabled".equals(key)) {
            locationWeightEnabled = value;
        } else if ("keyword_filter_enabled".equals(key)) {
            keywordFilterEnabled = value;
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("key", key);
        response.put("value", value);
        return response;
    }

    @GetMapping("/api/emergency/status")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> getEmergencyStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("active", emergencyActive);
        if (emergencyActive) {
            List<AnimalReport> animalReports = animalReportRepository.findByDeletedFalseOrderByRegdateDesc();
            AnimalReport latestAnimal = animalReports.stream()
                .filter(r -> "MISSING".equals(r.getReportType()))
                .findFirst()
                .orElse(null);

            List<MissingPersonReport> personReports = missingPersonRepository.findByDeletedFalseOrderByRegdateDesc();
            MissingPersonReport latestPerson = personReports.stream()
                .filter(r -> "OPEN".equals(r.getStatus()))
                .findFirst()
                .orElse(null);

            boolean isPersonMoreRecent = false;
            if (latestPerson != null && latestAnimal != null) {
                isPersonMoreRecent = latestPerson.getRegdate().isAfter(latestAnimal.getRegdate());
            } else if (latestPerson != null) {
                isPersonMoreRecent = true;
            }

            if (isPersonMoreRecent) {
                response.put("type", "PERSON");
                response.put("reportId", latestPerson.getReportId());
                response.put("title", "실종자를 찾습니다!");
                response.put("age", latestPerson.getAge());
                response.put("nationality", latestPerson.getNationality());
                response.put("location", latestPerson.getOccurredPlace());
                response.put("date", latestPerson.getOccurredAt() != null ? latestPerson.getOccurredAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");
                response.put("distinctiveMarks", String.format("의상: %s\n체형: %s, 얼굴형: %s, 헤어: %s (%s)", 
                    latestPerson.getClothing(), latestPerson.getBodyType(), latestPerson.getFaceShape(), 
                    latestPerson.getHairColor(), latestPerson.getHairStyle()));
                response.put("imageUrl", null);
            } else if (latestAnimal != null) {
                response.put("type", "ANIMAL");
                response.put("reportId", latestAnimal.getReportId());
                response.put("title", latestAnimal.getTitle() != null ? latestAnimal.getTitle() : "실종된 반려동물을 찾습니다!");
                response.put("breed", latestAnimal.getBreedName());
                response.put("animalType", latestAnimal.getAnimalType());
                response.put("location", latestAnimal.getRegionName() + " " + latestAnimal.getDetailPlace());
                response.put("date", latestAnimal.getEventDate().toString());
                response.put("distinctiveMarks", latestAnimal.getDistinctiveMarks());
                
                String imageUrl = null;
                if (latestAnimal.getImages() != null && !latestAnimal.getImages().isEmpty()) {
                    imageUrl = latestAnimal.getImages().get(0).getImageUrl();
                }
                response.put("imageUrl", imageUrl);
            } else {
                response.put("type", "ANIMAL");
                response.put("reportId", 0L);
                response.put("title", "실종된 반려동물을 찾습니다!");
                response.put("breed", "리트리버");
                response.put("animalType", "개");
                response.put("location", "서울시 강남구 역삼역 인근");
                response.put("date", "2026-05-18");
                response.put("distinctiveMarks", "순하고 사람을 잘 따르며, 노란색 목줄을 착용하고 있습니다. 발견 시 제보 부탁드립니다.");
                response.put("imageUrl", "/images/logo.png");
            }
        }
        return response;
    }

    @GetMapping("/backup/csv")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void downloadBackupCsv(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"dogo_backup_" + java.time.LocalDate.now() + ".csv\"");
        
        // Write UTF-8 BOM to prevent MS Excel from showing corrupted Korean characters
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        
        java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        
        // CSV Headers
        writer.println("구분,ID,물품명,카테고리,발생장소,상태,출처,등록일");
        
        // Lost items
        List<LostItem> lostItems = lostItemRepository.findByDeletedFalseOrderByRegDateDesc();
        for (LostItem item : lostItems) {
            writer.println(String.format("분실,%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                item.getLostId(),
                escapeCsv(item.getItemName()),
                escapeCsv(item.getCategoryMain()),
                escapeCsv(item.getLostPlace()),
                escapeCsv(item.getStatus()),
                escapeCsv(item.getSourceType()),
                item.getRegDate() != null ? item.getRegDate().toString() : ""
            ));
        }
        
        // Found items
        List<FoundItem> foundItems = foundItemRepository.findByDeletedFalseOrderByRegDateDesc();
        for (FoundItem item : foundItems) {
            writer.println(String.format("습득,%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                item.getFoundId(),
                escapeCsv(item.getItemName()),
                escapeCsv(item.getCategoryMain()),
                escapeCsv(item.getFoundPlace() != null ? item.getFoundPlace() : "경찰 보관소"),
                escapeCsv(item.getStatus()),
                escapeCsv(item.getSourceType()),
                item.getRegDate() != null ? item.getRegDate().toString() : ""
            ));
        }
        
        writer.flush();
        writer.close();
    }
    
    private String escapeCsv(String val) {
        if (val == null) return "";
        return val.replace("\"", "\"\"");
    }
}
