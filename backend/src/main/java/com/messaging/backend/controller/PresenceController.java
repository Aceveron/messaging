package com.messaging.backend.controller;

import com.messaging.backend.websocket.WebSocketPresenceListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    @Autowired
    private WebSocketPresenceListener presenceListener;

    @GetMapping("/online")
    public ResponseEntity<Set<String>> onlineUsers() {
        return ResponseEntity.ok(presenceListener.getOnlineUsers());
    }
}
