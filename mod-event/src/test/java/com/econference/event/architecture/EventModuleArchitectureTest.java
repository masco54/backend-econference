package com.econference.event.architecture;

import com.econference.event.domain.Event;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.event.domain.EventRequestStatusHistory;
import com.econference.event.domain.EventStatus;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class EventModuleArchitectureTest {

    @Test
    void domainTypesShouldNotDependOnSpringFrameworkAnnotationsOrTypes() {
        List<Class<?>> domainTypes = List.of(
                Event.class,
                EventStatus.class,
                EventRequest.class,
                EventRequestStatus.class,
                EventRequestStatusHistory.class
        );

        for (Class<?> domainType : domainTypes) {
            for (Annotation annotation : domainType.getAnnotations()) {
                assertFalse(annotation.annotationType().getPackageName().startsWith("org.springframework"),
                        () -> domainType.getName() + " should not depend on Spring annotations");
            }

            for (Field field : domainType.getDeclaredFields()) {
                assertFalse(field.getType().getPackageName().startsWith("org.springframework"),
                        () -> domainType.getName() + " should not reference Spring types in fields");
            }
        }
    }
}