package com.example.musicgym;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** Firebase 用户管理 — 匿名登录 + 昵称/头像 */
public class UserManager {

    private static UserManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final SharedPreferences prefs;

    private String userId, nickname;
    private OnUserReadyListener listener;

    public interface OnUserReadyListener {
        void onReady(String userId, String nickname);
    }

    private UserManager(Context ctx) {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = ctx.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        nickname = prefs.getString("nickname", "健身达人");
    }

    public static UserManager get(Context ctx) {
        if (instance == null) instance = new UserManager(ctx.getApplicationContext());
        return instance;
    }

    /** 自动匿名登录 */
    public void signIn(OnUserReadyListener listener) {
        this.listener = listener;
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            onSignedIn(user);
        } else {
            auth.signInAnonymously().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    onSignedIn(task.getResult().getUser());
                }
            });
        }
    }

    private void onSignedIn(FirebaseUser user) {
        userId = user.getUid();
        // 首次登录初始化 Firestore 用户文档
        db.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || !task.getResult().exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("nickname", nickname);
                        data.put("createdAt", System.currentTimeMillis());
                        db.collection("users").document(userId).set(data);
                    }
                    if (listener != null) listener.onReady(userId, nickname);
                });
    }

    public void setNickname(String name) {
        this.nickname = name;
        prefs.edit().putString("nickname", name).apply();
        if (userId != null) {
            db.collection("users").document(userId).update("nickname", name);
        }
    }

    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
}
