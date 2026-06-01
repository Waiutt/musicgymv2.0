package com.example.musicgym;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blog_posts")
public class BlogPost {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String date;
    private String snippet;
    private String author;
    private String fullContent;

    // ⚡ 核心改动：把以前的 int imageResId 换成了 String imageUri ⚡
    private String imageUri;

    public BlogPost(String title, String date, String snippet, String author, String fullContent, String imageUri) {
        this.title = title;
        this.date = date;
        this.snippet = snippet;
        this.author = author;
        this.fullContent = fullContent;
        this.imageUri = imageUri;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getSnippet() { return snippet; }
    public String getAuthor() { return author; }
    public String getFullContent() { return fullContent; }

    // ⚡ 对应的 Getter 也要改
    public String getImageUri() { return imageUri; }
}