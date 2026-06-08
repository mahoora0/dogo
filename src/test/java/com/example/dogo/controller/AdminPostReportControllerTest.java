package com.example.dogo.controller;

import com.example.dogo.dto.AdminChatView;
import com.example.dogo.service.ChatService;
import com.example.dogo.service.chat.ChatUnavailableException;
import com.example.dogo.service.report.PostReportService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminPostReportControllerTest {

    private final PostReportService postReportService = mock(PostReportService.class);
    private final ChatService chatService = mock(ChatService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new AdminPostReportController(postReportService, chatService))
            .build();

    @Test
    void chatViewRendersConversationForAdmin() throws Exception {
        AdminChatView view = new AdminChatView(77L, 2L, "작성자", "owner1", 1L, "신청자", "inquirer1", List.of());
        when(chatService.getAdminChatView(77L)).thenReturn(view);

        mockMvc.perform(get("/admin/reports/chat/77"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/chat-view"))
                .andExpect(model().attributeExists("chat"));
    }

    @Test
    void chatViewRedirectsWhenRoomMissing() throws Exception {
        when(chatService.getAdminChatView(anyLong()))
                .thenThrow(new ChatUnavailableException("채팅방을 찾을 수 없습니다."));

        mockMvc.perform(get("/admin/reports/chat/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reports"))
                .andExpect(flash().attributeExists("adminReportError"));
    }
}
