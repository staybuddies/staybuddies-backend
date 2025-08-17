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

    /** 1) My conversation list (UI: GET /messages/threads) */
    @GetMapping("/threads")
    public List<ConversationDto> threads(@AuthenticationPrincipal UserDetails ud) {
        return svc.threadsFor(ud.getUsername());
    }

    /** 2) Ensure (or get) the canonical thread with a specific user
     *  (UI: POST /messages/thread-with/{otherId}) */
    @PostMapping("/thread-with/{otherId}")
    public Map<String, Long> ensure(@AuthenticationPrincipal UserDetails ud,
                                    @PathVariable Long otherId) {
        Long threadId = svc.ensureThreadByEmail(ud.getUsername(), otherId);
        return Map.of("threadId", threadId);
    }

    /** 3) Messages in a thread I own (UI: GET /messages/threads/{id}/messages) */
    @GetMapping("/threads/{id}/messages")
    public List<MessageDto> messages(@AuthenticationPrincipal UserDetails ud,
                                     @PathVariable Long id) {
        return svc.messages(ud.getUsername(), id);
    }

    /** 4) Send a message (UI: POST /messages/threads/{id} with {content}) */
    @PostMapping("/threads/{id}")
    public ResponseEntity<?> send(@AuthenticationPrincipal UserDetails ud,
                                  @PathVariable Long id,
                                  @RequestBody SendMessageRequest req) {
        String text = (req == null) ? null : req.resolved();
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is empty");
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(svc.send(ud.getUsername(), id, text.trim()));
        } catch (MessageService.ThreadNotOwned ex) {
            // lets the UI reconcile to the canonical thread
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("code", "THREAD_NOT_OWNED", "withUserId", ex.getOtherUserId()));
        }
    }

    /** 5) Mark messages read (UI: PUT /messages/threads/{id}/read) */
    @PutMapping("/threads/{id}/read")
    public Map<String, Integer> markRead(@AuthenticationPrincipal UserDetails ud,
                                         @PathVariable Long id) {
        int n = svc.markRead(ud.getUsername(), id);
        return Map.of("read", n);
    }
}
