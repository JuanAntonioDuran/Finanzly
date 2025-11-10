package com.example.finanzly.models;

public class Movement {

    private String id;                 // ID autogenerado por Firebase
    private String userId;             // Relación con el usuario
    private String type;               // "income" o "expense"
    private String category;           // Categoría del movimiento (ej: comida, transporte)
    private double amount;             // Cantidad de dinero
    private String date;               // Fecha en formato YYYY-MM-DD
    private String description;        // Descripción opcional
    private String linkedBudgetId;     // ID de presupuesto relacionado (opcional)
    private String linkedGoalId;       // ID de meta financiera relacionada (opcional)

    // 🔹 Constructor vacío requerido por Firebase
    public Movement() {
    }

    // 🔹 Constructor completo (opcional)
    public Movement(String id, String userId, String type, String category, double amount,
                    String date, String description, String linkedBudgetId, String linkedGoalId) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
        this.linkedBudgetId = linkedBudgetId;
        this.linkedGoalId = linkedGoalId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkedBudgetId() {
        return linkedBudgetId;
    }

    public void setLinkedBudgetId(String linkedBudgetId) {
        this.linkedBudgetId = linkedBudgetId;
    }

    public String getLinkedGoalId() {
        return linkedGoalId;
    }

    public void setLinkedGoalId(String linkedGoalId) {
        this.linkedGoalId = linkedGoalId;
    }
}
