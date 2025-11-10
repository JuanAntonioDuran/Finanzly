package com.example.finanzly.models;

public class User {

    private String uid;         // ID único del usuario
    private String name;        // Nombre del usuario
    private String email;       // Correo electrónico
    private String createdAt;   // Fecha de creación (opcional)

    // Constructor vacío requerido por Firebase
    public User() {}

    // Constructor completo
    public User(String uid, String name, String email, String createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
