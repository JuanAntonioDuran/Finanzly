package com.example.finanzly.models;

import java.util.List;

public class Budget {

    private String id;                // ID autogenerado por Firebase
    private String userId;            // ID del usuario creador del presupuesto
    private List<String> sharedUserIds; // IDs de otros usuarios con acceso
    private String category;
    private double limit;
    private double spent;
    private String createdAt;
    private String updatedAt;

    // 🔹 Constructor vacío (obligatorio para Firebase)
    public Budget() {
    }

    // 🔹 Constructor completo
    public Budget(String id, String userId, List<String> sharedUserIds, String category,
                  double limit, double spent, String createdAt, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.sharedUserIds = sharedUserIds;
        this.category = category;
        this.limit = limit;
        this.spent = spent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
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
