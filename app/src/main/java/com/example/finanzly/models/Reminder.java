package com.example.finanzly.models;

import java.util.List;
import java.util.Map;

public class Reminder {

    private String id;
    private String userId;

    private String title;
    private String description;

    private String date;         // "2025-11-30"
    private String time;         // "17:00"
    private String type;         // "otro" | "presupuesto" | "meta" | "pago"

    private boolean isCompleted;
    private boolean isExpired;

    // Para metas
    private String linkedGoalId;

    // Para presupuestos
    private String linkedBudgetId;

    // Sistema de usuarios compartidos
    private List<String> sharedUserIds;               // array
    private Map<String, Boolean> sharedUsersStatus;   // { userId: true/false }

    private String createdAt;    // ISO
    private String updatedAt;    // ISO

    // Requerido por Firebase
    public Reminder() { }

    // -------- GETTERS & SETTERS -------- //

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(boolean completed) { isCompleted = completed; }

    public boolean getIsExpired() { return isExpired; }
    public void setIsExpired(boolean expired) { isExpired = expired; }


    public String getLinkedGoalId() {
        return linkedGoalId;
    }

    public void setLinkedGoalId(String linkedGoalId) {
        this.linkedGoalId = linkedGoalId;
    }

    public String getLinkedBudgetId() {
        return linkedBudgetId;
    }

    public void setLinkedBudgetId(String linkedBudgetId) {
        this.linkedBudgetId = linkedBudgetId;
    }

    public List<String> getSharedUserIds() {
        return sharedUserIds;
    }

    public void setSharedUserIds(List<String> sharedUserIds) {
        this.sharedUserIds = sharedUserIds;
    }

    public Map<String, Boolean> getSharedUsersStatus() {
        return sharedUsersStatus;
    }

    public void setSharedUsersStatus(Map<String, Boolean> sharedUsersStatus) {
        this.sharedUsersStatus = sharedUsersStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
