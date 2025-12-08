package com.kospot.infrastructure.doc.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.infrastructure.doc.annotation.WebSocketDoc;
import com.kospot.infrastructure.doc.dto.WebSocketEndpointDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class WebSocketDocCollector implements ApplicationListener<ContextRefreshedEvent> {

    private final List<WebSocketEndpointDoc> endpoints = new ArrayList<>();
    private final ApplicationContext context;
    private final ObjectMapper objectMapper;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        collectEndpoints();
    }

    private void collectEndpoints() {
        Map<String, WebSocketEndpointDoc> endpointMap = new HashMap<>();

        // MessageMapping 수집
        collectMessageMappings(endpointMap);

        // ServerBroadcast 수집
        collectWebSocketDocs(endpointMap);

        endpoints.addAll(endpointMap.values());
    }

    private void collectMessageMappings(Map<String, WebSocketEndpointDoc> endpointMap) {
        Map<String, Object> controllers = context.getBeansWithAnnotation(Controller.class);

        for (Object controller : controllers.values()) {
            Method[] methods = controller.getClass().getDeclaredMethods();

            for (Method method : methods) {
                MessageMapping messageMapping = method.getAnnotation(MessageMapping.class);
                SendTo sendTo = method.getAnnotation(SendTo.class);

                if (messageMapping != null) {
                    String[] clientDests = messageMapping.value();
                    Class<?> payloadType = getPayloadType(method);

                    // Client → Server
                    for (String dest : clientDests) {
                        String fullDest = "/app" + dest;
                        endpointMap.putIfAbsent(fullDest, WebSocketEndpointDoc.builder()
                                .destination(fullDest)
                                .build());

                        endpointMap.get(fullDest).setClientToServer(
                                WebSocketEndpointDoc.ClientToServerInfo.builder()
                                        .payloadType(payloadType.getSimpleName())
                                        .payloadExample(createExample(payloadType))
                                        .build()
                        );
                    }

                    // Server → Client (SendTo)
                    if (sendTo != null) {
                        for (String dest : sendTo.value()) {
                            endpointMap.putIfAbsent(dest, WebSocketEndpointDoc.builder()
                                    .destination(dest)
                                    .build());

                            endpointMap.get(dest).setServerToClient(
                                    WebSocketEndpointDoc.ServerToClientInfo.builder()
                                            .payloadType(payloadType.getSimpleName())
                                            .payloadExample(createExample(payloadType))
                                            .trigger("Response to: " + String.join(", ", clientDests))
                                            .build()
                            );
                        }
                    }
                }
            }
        }
    }

    private void collectWebSocketDocs(Map<String, WebSocketEndpointDoc> endpointMap) {
        Map<String, Object> services = context.getBeansOfType(Object.class);

        for (Object service : services.values()) {
            Method[] methods = service.getClass().getDeclaredMethods();

            for (Method method : methods) {
                WebSocketDoc broadcast = method.getAnnotation(WebSocketDoc.class);

                if (broadcast != null) {
                    String dest = broadcast.destination();
                    endpointMap.putIfAbsent(dest, WebSocketEndpointDoc.builder()
                            .destination(dest)
                            .description(broadcast.description())
                            .build());

                    endpointMap.get(dest).setServerToClient(
                            WebSocketEndpointDoc.ServerToClientInfo.builder()
                                    .payloadType(broadcast.payloadType().getSimpleName())
                                    .payloadExample(createExample(broadcast.payloadType()))
                                    .trigger(broadcast.trigger())
                                    .description(broadcast.description())
                                    .build()
                    );
                }
            }
        }
    }

    private Class<?> getPayloadType(Method method) {
        for (Parameter param : method.getParameters()) {
            if (param.isAnnotationPresent(Payload.class)) {
                return param.getType();
            }
        }
        return Object.class;
    }

    private Object createExample(Class<?> type) {
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            return objectMapper.readTree(objectMapper.writeValueAsString(instance));
        } catch (Exception e) {
            return null;
        }
    }

    public List<WebSocketEndpointDoc> getEndpoints() {
        return new ArrayList<>(endpoints);
    }
}

