package com.example.finanzly.models;

import java.util.List;

public class Goal {

     private String id;                  // ID único (Firebase)
    private String userId;              // ID del usuario propietario
    private List<String> sharedUserIds; // IDs de otros usuarios con acceso
    private String title;               // Nombre del objetivo ("Ahorro para vacaciones")
    private double targetAmount;        // Cantidad meta
    private double currentAmount;       // Progreso actual
    private String deadline;            // Fecha límite (opcional)
    private String createdAt;          // Fecha de creación (opcional)

    // 🔹 Constructor vacío (Firebase lo necesita)
    public Goal() {
    }

    // 🔹 Constructor completo (opcional)
    public Goal(String id, String userId, List<String> sharedUserIds, String title,
                double targetAmount, double currentAmount, String deadline, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.sharedUserIds = sharedUserIds;
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.deadline = deadline;
        this.createdAt = createdAt;
    }

    // 🔹 Getters y Setters
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

    public List<String> getSharedUserIds() {
        return sharedUserIds;
    }

    public void setSharedUserIds(List<String> sharedUserIds) {
        this.sharedUserIds = sharedUserIds;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
