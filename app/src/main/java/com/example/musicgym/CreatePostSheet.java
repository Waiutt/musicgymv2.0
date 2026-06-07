package com.example.musicgym;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.FileOutputStream;

/** 发帖浮窗 — 替代 CreatePostActivity */
public class CreatePostSheet extends BottomSheetDialogFragment {

    private ImageView ivPreview;
    private String currentImageUri = "";
    private CommunityRepository repo;
    private UserManager userManager;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Bitmap bmp = (Bitmap) result.getData().getExtras().get("data");
                    if (bmp != null) {
                        currentImageUri = saveToCache(bmp);
                        Glide.with(this).load(currentImageUri).into(ivPreview);
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    currentImageUri = uri.toString();
                    Glide.with(this).load(uri).into(ivPreview);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_MusicGym);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sheet_create_post, container, false);
        ivPreview = v.findViewById(R.id.create_iv_preview);
        EditText etTitle = v.findViewById(R.id.create_et_title);
        EditText etContent = v.findViewById(R.id.create_et_content);
        Button btnCamera = v.findViewById(R.id.create_btn_camera);
        Button btnGallery = v.findViewById(R.id.create_btn_gallery);
        Button btnPublish = v.findViewById(R.id.create_btn_publish);

        repo = new CommunityRepository();
        userManager = UserManager.get(requireContext());
        userManager.signIn((uid, nick) -> {});

        btnCamera.setOnClickListener(v2 -> cameraLauncher.launch(
                new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)));
        btnGallery.setOnClickListener(v2 -> galleryLauncher.launch("image/*"));

        btnPublish.setOnClickListener(v2 -> {
            String title = etTitle.getText().toString().trim();
            String content = etContent.getText().toString().trim();
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(getContext(), "请填写标题和内容", Toast.LENGTH_SHORT).show();
                return;
            }
            repo.publishPost(userManager.getUserId(), userManager.getNickname(),
                    title, content, currentImageUri, postId -> {
                        if (postId != null) {
                            Toast.makeText(getContext(), "发布成功！", Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    });
        });

        return v;
    }

    private String saveToCache(Bitmap bmp) {
        try {
            File dir = new File(requireContext().getCacheDir(), "images");
            dir.mkdirs();
            File f = new File(dir, "snap_" + System.currentTimeMillis() + ".png");
            FileOutputStream os = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            os.close();
            return Uri.fromFile(f).toString();
        } catch (Exception e) { return ""; }
    }
}
