package com.sirh.notification.dto;

import com.sirh.notification.entity.Notification;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationDto {
    private final Long id;
    private final String message;
    private final String type;
    private final String lien;
    private final boolean lu;
    private final LocalDateTime dateCreation;

    public NotificationDto(Notification n) {
        this.id = n.getId();
        this.message = n.getMessage();
        this.type = n.getType() == null ? null : n.getType().name();
        this.lien = n.getLien();
        this.lu = n.isLu();
        this.dateCreation = n.getDateCreation();
    }
}
