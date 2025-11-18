package com.example.finanzly.models;


public class Movement {

    private String id;                 // ID autogenerado por Firebase
    private String userId;             // Usuario dueño del movimiento
    private String type;               // "income" o "expense"
    private String category;           // Categoría
    private double amount;             // Cantidad
    private String date;               // Fecha YYYY-MM-DD (o DD-MM-YYYY si quieres)
    private String description;        // Descripción opcional
    private String linkedBudgetId;     // Puede ser null
    private String linkedGoalId;       // Puede ser null

    // 🔥 Firebase NECESITA SI O SI un constructor vacío
    public Movement() {}

    // 👉 Getters y setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLinkedBudgetId() { return linkedBudgetId; }
    public void setLinkedBudgetId(String linkedBudgetId) { this.linkedBudgetId = linkedBudgetId; }

    public String getLinkedGoalId() { return linkedGoalId; }
    public void setLinkedGoalId(String linkedGoalId) { this.linkedGoalId = linkedGoalId; }
}

