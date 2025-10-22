package com.example.cookmate.models;

public class FavoriteItem {
    private String id;
    private String image;
    private String title;
    private String creator; // mô tả/điểm nhận diện người tạo công thức

    public FavoriteItem() {}

    public FavoriteItem(String id, String image, String title, String creator) {
        this.id = id;
        this.image = image;
        this.title = title;
        this.creator = creator;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
}
