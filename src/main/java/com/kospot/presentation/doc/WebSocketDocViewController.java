package com.kospot.presentation.doc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebSocketDocViewController {

    @GetMapping("/docs/websocket")
    public String websocketDocs() {
        return "websocket-docs";
    }
}
