// Reminder.java
package com.example.finanzly.models;

import java.util.List;
import java.util.Map;

public class Reminder {
    private String id;
    private String userId;
    private String title;
    private String description;
    private String date; // YYYY-MM-DD
    private String time; // HH:mm
    private String type; // 'pago' | 'meta' | 'presupuesto' | 'otro'
    private boolean isCompleted;
    private Boolean isExpired;
    private String linkedGoalId;
    private String linkedBudgetId;
    private String movementType; // 'income' | 'expense' | null
    private List<String> sharedUserIds;
    private Map<String, Boolean> sharedUsersStatus;
    private String createdAt;
    private String updatedAt;

    public Reminder() {}

    // Constructor simplificado
    public Reminder(String id, String userId, String title, String description,
                    String date, String time, String type) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.date = date;
        this.time = time;
        this.type = type;
        this.isCompleted = false;
        this.isExpired = false;
    }

    // Getters & Setters (genera con tu IDE si quieres)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Boolean getExpired() { return isExpired; }
    public void setExpired(Boolean expired) { isExpired = expired; }

    public String getLinkedGoalId() { return linkedGoalId; }
    public void setLinkedGoalId(String linkedGoalId) { this.linkedGoalId = linkedGoalId; }

    public String getLinkedBudgetId() { return linkedBudgetId; }
    public void setLinkedBudgetId(String linkedBudgetId) { this.linkedBudgetId = linkedBudgetId; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public List<String> getSharedUserIds() { return sharedUserIds; }
    public void setSharedUserIds(List<String> sharedUserIds) { this.sharedUserIds = sharedUserIds; }

    public Map<String, Boolean> getSharedUsersStatus() { return sharedUsersStatus; }
    public void setSharedUsersStatus(Map<String, Boolean> sharedUsersStatus) { this.sharedUsersStatus = sharedUsersStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
