package com.example.cookmate.models;

public class ShoppingItem {
    private String _id;     // note: API uses _id
    private String userId;
    private String name;
    private String status;  // e.g., "draft"
    private String notes;
    private String createdAt;
    private String updatedAt;
    private int __v;

    // Getters / setters
    public String getId() { return _id; }
    public void setId(String id) { this._id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public int get__v() { return __v; }
    public void set__v(int __v) { this.__v = __v; }
}
