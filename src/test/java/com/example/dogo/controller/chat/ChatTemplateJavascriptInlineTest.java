package com.example.dogo.controller.chat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTemplateJavascriptInlineTest {

    private static final Path TEMPLATE = Path.of("src/main/resources/templates/chat/index.html");
    private static final Path CSS = Path.of("src/main/resources/static/css/chat.css");

    @Test
    void chatTemplateUsesExistingItemPlaceholder() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html).doesNotContain("/images/placeholder.png");
        assertThat(html).contains("/images/noImageSize.png");
    }

    @Test
    void chatTemplateAppendsSentMessageWhenBrokerEchoIsDelayed() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("clientMessageId");
        assertThat(html).contains("appendMessage(pendingMessage)");
        assertThat(html).contains("renderedMessageIds");
    }

    @Test
    void chatCssPreventsNarrowVerticalText() throws Exception {
        String css = Files.readString(CSS);

        assertThat(css).contains(".chat-room-row");
        assertThat(css).contains("white-space: nowrap");
        assertThat(css).contains("word-break: keep-all");
    }

    @Test
    void chatPageUsesFullWidthLayoutWithoutGlobalSidebar() throws Exception {
        String html = Files.readString(TEMPLATE);
        String css = Files.readString(CSS);

        assertThat(html).contains("chat-page");
        assertThat(css).contains(".content:has(.chat-page)");
        assertThat(css).contains(".main-layout:has(.chat-page) .sidebar");
        assertThat(css).contains("display: none");
    }

    @Test
    void chatTemplateSubscribesToPersonalMessageChannel() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("/sub/users/");
        assertThat(html).contains("handleIncomingMessage");
    }

    @Test
    void chatTemplateSendsMessagesThroughPersistentHttpEndpoint() throws Exception {
        String html = Files.readString(TEMPLATE);

        assertThat(html).contains("fetch(`/chat/room/${currentRoomId}/messages`");
        assertThat(html).contains("method: 'POST'");
        assertThat(html).contains("savedMessage");
    }
}
