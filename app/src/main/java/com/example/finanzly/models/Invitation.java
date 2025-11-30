package com.example.finanzly.models;

public class Invitation {

    private String id;                // ID autogenerado por Firebase
    private String fromUserId;        // UID del usuario que envía la invitación
    private String toUserId;          // UID del usuario invitado
    private String resourceType;      // "budget" | "goal"
    private String resourceIdBudget;  // ID del budget (si aplica)
    private String resourceIdGoal;    // ID del goal (si aplica)
    private String status;            // "pending" | "accepted" | "declined"
    private String createdAt;         // Fecha ISO
    private String respondedAt;       // Fecha ISO (si respondió)
    private String message;           // Mensaje opcional

    // 🔹 Constructor vacío obligatorio para Firebase
    public Invitation() {}

    // 🔹 Constructor completo opcional
    public Invitation(String id, String fromUserId, String toUserId,
                      String resourceType, String resourceIdBudget, String resourceIdGoal,
                      String status, String createdAt, String respondedAt, String message) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.resourceType = resourceType;
        this.resourceIdBudget = resourceIdBudget;
        this.resourceIdGoal = resourceIdGoal;
        this.status = status;
        this.createdAt = createdAt;
        this.respondedAt = respondedAt;
        this.message = message;
    }

    // 🔹 Getters y Setters (Firebase los necesita)
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceIdBudget() { return resourceIdBudget; }
    public void setResourceIdBudget(String resourceIdBudget) { this.resourceIdBudget = resourceIdBudget; }

    public String getResourceIdGoal() { return resourceIdGoal; }
    public void setResourceIdGoal(String resourceIdGoal) { this.resourceIdGoal = resourceIdGoal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getRespondedAt() { return respondedAt; }
    public void setRespondedAt(String respondedAt) { this.respondedAt = respondedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
