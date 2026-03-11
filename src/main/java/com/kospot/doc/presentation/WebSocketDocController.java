package com.kospot.doc.presentation;

import com.kospot.doc.infrastructure.collector.WebSocketDocCollector;
import com.kospot.doc.infrastructure.dto.WebSocketEndpointDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class WebSocketDocController {

    private final WebSocketDocCollector docCollector;

    @GetMapping("/websocket")
    public List<WebSocketEndpointDoc> getAllEndpoints() {
        return docCollector.getEndpoints();
    }
}