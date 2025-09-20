package com.example.SP.senior_project.api.v1.controller;

import com.example.SP.senior_project.dto.push.RegisterTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "staybuddies.legacy-push",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false // default: disabled
)
@Deprecated
public class PushAliasController {

    private final NotificationController delegate;

    @PostMapping("/token")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> legacyRegisterToken(
            @AuthenticationPrincipal UserDetails ud,
            @RequestBody RegisterTokenRequest req) {
        return delegate.register(ud, req);
    }
}
