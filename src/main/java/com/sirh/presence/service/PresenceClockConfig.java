package com.sirh.presence.service;
import java.time.*;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
@Configuration
public class PresenceClockConfig {
    @Bean public Clock horlogePresence(@Value("${app.presence.zone:Africa/Nairobi}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
