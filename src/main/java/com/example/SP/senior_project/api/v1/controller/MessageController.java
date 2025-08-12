package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.message.ConversationDto;
import com.example.SP.senior_project.dto.message.MessageDto;
import com.example.SP.senior_project.dto.message.SendMessageRequest;
import com.example.SP.senior_project.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {
    private final MessageService svc;

    // 1) My conversation list
    @GetMapping("/threads")
    public List<ConversationDto> threads(@AuthenticationPrincipal UserDetails ud) {
        return svc.threadsFor(ud.getUsername());
    }

    // 2) Ensure (or get) the canonical thread with a specific user
    @PostMapping("/thread-with/{otherId}")
    public Map<String, Long> ensure(@AuthenticationPrincipal UserDetails ud,
                                    @PathVariable Long otherId) {
        Long threadId = svc.ensureThreadByEmail(ud.getUsername(), otherId);
        return Map.of("threadId", threadId);
    }

    // 3) Messages in a thread I own
    @GetMapping("/threads/{id}/messages")
    public List<MessageDto> messages(@AuthenticationPrincipal UserDetails ud,
                                     @PathVariable Long id) {
        return svc.messages(ud.getUsername(), id);
    }

    @PostMapping("/threads/{id}")
    public ResponseEntity<?> send(@AuthenticationPrincipal UserDetails ud,
                                  @PathVariable Long id,
                                  @RequestBody SendMessageRequest req) {
        String text = req.resolved();
        if (text == null || text.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty");
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(svc.send(ud.getUsername(), id, text.trim()));
        } catch (MessageService.ThreadNotOwned ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "THREAD_NOT_OWNED", "withUserId", ex.getOtherUserId()));
        }
    }


}
