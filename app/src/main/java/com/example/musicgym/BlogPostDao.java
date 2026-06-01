package com.example.musicgym;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

// ⚡ @Dao 告诉系统：这就是我们的数据库翻译官 (Data Access Object)
@Dao
public interface BlogPostDao {

    // ⚡ 插入一条新动态到数据库 (系统会自动把它存入硬盘)
    @Insert
    void insertPost(BlogPost post);

    // ⚡ 读取所有动态
    // (ORDER BY id DESC 表示按身份证号倒序排列，也就是最新发出的帖子在最上面！)
    @Query("SELECT * FROM blog_posts ORDER BY id DESC")
    List<BlogPost> getAllPosts();

}