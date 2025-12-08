package com.kospot.infrastructure.doc.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.infrastructure.doc.annotation.WebSocketDoc;
import com.kospot.infrastructure.doc.dto.WebSocketEndpointDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@Slf4j
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

        // WebSocketDocs 수집
        collectWebSocketDocs(endpointMap);

        endpoints.addAll(endpointMap.values());
    }

    private void collectMessageMappings(Map<String, WebSocketEndpointDoc> endpointMap) {
        String[] beanNames = context.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean = context.getBean(beanName);
            Class<?> clazz = bean.getClass();

            // CGLIB 프록시인 경우 실제 클래스 가져오기
            if (clazz.getName().contains("$")) {
                clazz = clazz.getSuperclass();
            }

            Method[] methods = clazz.getDeclaredMethods();

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

    private void collectWebSocketDocs(Map<String, WebSocketEndpointDoc> endpointMap) {
        Map<String, Object> services = context.getBeansOfType(Object.class);

        for (Object service : services.values()) {
            Method[] methods = service.getClass().getDeclaredMethods();

            for (Method method : methods) {
                WebSocketDoc webSocketDoc = method.getAnnotation(WebSocketDoc.class);

                if (webSocketDoc != null) {
                    String dest = webSocketDoc.destination();
                    endpointMap.putIfAbsent(dest, WebSocketEndpointDoc.builder()
                            .destination(dest)
                            .description(webSocketDoc.description())
                            .build());

                    endpointMap.get(dest).setServerToClient(
                            WebSocketEndpointDoc.ServerToClientInfo.builder()
                                    .payloadType(webSocketDoc.payloadType().getSimpleName())
                                    .payloadExample(createExample(webSocketDoc.payloadType()))
                                    .trigger(webSocketDoc.trigger())
                                    .description(webSocketDoc.description())
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
            // 1. 기본 생성자로 시도
            try {
                Object instance = type.getDeclaredConstructor().newInstance();
                String json = objectMapper.writeValueAsString(instance);
                return objectMapper.readTree(json);
            } catch (NoSuchMethodException e) {
                // 기본 생성자 없음 - 빌더 패턴 시도
            }

            // 2. 빌더 패턴 시도
            try {
                Method builderMethod = type.getDeclaredMethod("builder");
                Object builder = builderMethod.invoke(null);

                // 빌더의 build() 메서드 호출
                Method buildMethod = builder.getClass().getDeclaredMethod("build");
                Object instance = buildMethod.invoke(builder);

                String json = objectMapper.writeValueAsString(instance);
                return objectMapper.readTree(json);
            } catch (NoSuchMethodException e) {
                // 빌더 패턴도 없음
            }

            // 3. 필드 정보로 스키마 생성
            return createSchemaFromFields(type);

        } catch (Exception e) {
            log.warn("Failed to create example for type: {}", type.getName(), e);
            return createSchemaFromFields(type);
        }
    }

    private Object createSchemaFromFields(Class<?> type) {
        try {
            Map<String, Object> schema = new LinkedHashMap<>();

            // 모든 필드 가져오기
            Field[] fields = type.getDeclaredFields();

            for (Field field : fields) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();

                // static 필드 제외
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                schema.put(fieldName, getExampleValue(fieldType, fieldName));
            }

            return objectMapper.readTree(objectMapper.writeValueAsString(schema));
        } catch (Exception e) {
            log.warn("Failed to create schema for type: {}", type.getName(), e);
            return null;
        }
    }

    private Object getExampleValue(Class<?> type, String fieldName) {
        // 기본 타입
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class) return 0;
        if (type == long.class || type == Long.class) return 0L;
        if (type == boolean.class || type == Boolean.class) return false;
        if (type == double.class || type == Double.class) return 0.0;
        if (type == float.class || type == Float.class) return 0.0f;

        // 날짜/시간
        if (type == java.time.LocalDateTime.class) return "2024-01-01T00:00:00";
        if (type == java.time.LocalDate.class) return "2024-01-01";
        if (type == java.time.LocalTime.class) return "00:00:00";
        if (type == java.util.Date.class) return "2024-01-01T00:00:00Z";

        // 컬렉션
        if (List.class.isAssignableFrom(type)) {
            return List.of(); // 빈 배열
        }
        if (Set.class.isAssignableFrom(type)) {
            return Set.of();
        }
        if (Map.class.isAssignableFrom(type)) {
            return Map.of();
        }

        // 배열
        if (type.isArray()) {
            return new Object[0];
        }

        // Enum
        if (type.isEnum()) {
            Object[] constants = type.getEnumConstants();
            return constants.length > 0 ? constants[0].toString() : "ENUM_VALUE";
        }

        // 중첩 객체 (재귀 방지를 위해 간단한 표시)
        return "{}";
    }

    public List<WebSocketEndpointDoc> getEndpoints() {
        return new ArrayList<>(endpoints);
    }
}

