package com.example.musicgym;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreatePostActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private String currentImageUri = "";
    private CommunityRepository repo;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        ivPreview = findViewById(R.id.create_iv_preview);
        EditText etTitle = findViewById(R.id.create_et_title);
        EditText etContent = findViewById(R.id.create_et_content);
        Button btnCamera = findViewById(R.id.create_btn_camera);
        Button btnGallery = findViewById(R.id.create_btn_gallery);
        Button btnPublish = findViewById(R.id.create_btn_publish);

        repo = new CommunityRepository();
        userManager = UserManager.get(this);
        userManager.signIn((uid, nick) -> {});

        // ⚡ 触发相机：获取拍下的照片
        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        // ⚡ 触发相册：安全打开系统文件选择器 (免读写权限)
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        // ⚡ 发布按钮：将数据写入硬盘并飞回主页
        btnPublish.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Log corrupted: Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userManager.getUserId() == null) {
                Toast.makeText(this, "正在连接社区...", Toast.LENGTH_SHORT).show();
                return;
            }

            // 发布到 Firestore 社区
            repo.publishPost(userManager.getUserId(), userManager.getNickname(),
                    title, content, currentImageUri, postId -> {
                        if (postId != null) {
                            Toast.makeText(this, "发布成功！", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "发布失败，请重试", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    // ================== 硬件回调处理区 ==================

    // 相册选择器的回调
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        // 锁定这张图片的读取权限，防止重启后变黑
                        getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        currentImageUri = selectedImage.toString();
                        Glide.with(this).load(currentImageUri).into(ivPreview);
                    }
                }
            }
    );

    // 相机的回调 (主脑特殊黑科技：将缓存图片直接转为 URI，免去繁琐的 FileProvider 配置)
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    currentImageUri = saveBitmapToCache(imageBitmap); // 转换为安全路径
                    Glide.with(this).load(currentImageUri).into(ivPreview);
                }
            }
    );

    // 将相机的画面存入 App 私密缓存区
    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "cyber_snap_" + System.currentTimeMillis() + ".png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            return Uri.fromFile(file).toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}