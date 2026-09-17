package com.amar.slackclone.notification.dto;

public record NotificationEvent(String type, NotificationResponse notification, long unreadCount) {}
