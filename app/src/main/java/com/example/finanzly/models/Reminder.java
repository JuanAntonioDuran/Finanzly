package com.example.finanzly.models;

import java.util.Map;
import java.util.List;

public class Reminder {

    private String id;                 // ID único (Firebase)
    private String userId;             // ID del creador del recordatorio
    private String title;              // Título del recordatorio
    private String description;        // Descripción opcional
    private String date;               // Fecha (YYYY-MM-DD)
    private String time;               // Hora (HH:mm)

    // 🔹 Tipo general del recordatorio: "pago", "meta", "presupuesto" o "otro"
    private String type;

    // 🔹 Estado general
    private boolean isCompleted;       // Si el creador lo completó
    private Boolean isExpired;         // Si la fecha límite ya pasó (opcional)

    // 🔗 Enlaces opcionales
    private String linkedGoalId;       // Enlace a una meta (opcional)
    private String linkedBudgetId;     // Enlace a un presupuesto (opcional)

    // 💰 Integración con movimientos
    private String movementType;       // "income", "expense" o null (opcional)

    // 👥 Usuarios compartidos
    private List<String> sharedUserIds;       // IDs de usuarios que comparten el recordatorio
    private Map<String, Boolean> sharedUsersStatus;  // Estado de cada usuario (quién lo completó)

    // 🕒 Control temporal
    private String createdAt;          // Fecha de creación (opcional)
    private String updatedAt;          // Última actualización (opcional)

    // 🔹 Constructor vacío requerido por Firebase
    public Reminder() {}

    // 🔹 Getters y Setters
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

    public Boolean getIsExpired() { return isExpired; }
    public void setIsExpired(Boolean isExpired) { this.isExpired = isExpired; }

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
