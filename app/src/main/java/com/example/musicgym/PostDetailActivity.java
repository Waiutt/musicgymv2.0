package com.example.musicgym;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // 1. 扫描并绑定所有 UI 控件
        ImageView ivImage = findViewById(R.id.detail_iv_image);
        TextView tvTitle = findViewById(R.id.detail_tv_title);
        TextView tvAuthor = findViewById(R.id.detail_tv_author);
        TextView tvDate = findViewById(R.id.detail_tv_date);
        TextView tvContent = findViewById(R.id.detail_tv_content);
        ImageButton btnBack = findViewById(R.id.detail_btn_back);

        // 2. 接收从 ShareFragment 传过来的数据包裹
        String title = getIntent().getStringExtra("POST_TITLE");
        String author = getIntent().getStringExtra("POST_AUTHOR");
        String date = getIntent().getStringExtra("POST_DATE");
        String content = getIntent().getStringExtra("POST_CONTENT");

        // ⚡ 核心修改区：改为接收 String 类型的图片 URI ⚡
        String imageUri = getIntent().getStringExtra("POST_IMAGE_URI");

        // 3. 将数据装填到页面上
        tvTitle.setText(title != null ? title : "Unknown Title");
        tvAuthor.setText(author != null ? author : "Unknown Author");
        tvDate.setText(date != null ? date : "0000-00-00");
        tvContent.setText(content != null ? content : "No content available.");

        // ⚡ 使用 Glide 引擎加载 String 类型的真实路径图片
        if (imageUri != null && !imageUri.isEmpty()) {
            Glide.with(this).load(imageUri).into(ivImage);
        }

        // 4. 给左上角的返回按钮装上“撤退”逻辑
        btnBack.setOnClickListener(v -> finish());
    }
}