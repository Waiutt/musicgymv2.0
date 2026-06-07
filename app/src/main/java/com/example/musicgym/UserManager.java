package com.example.musicgym;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/** Firebase 用户管理 — 匿名登录 + 昵称/头像。Firebase 不可用时降级为离线模式 */
public class UserManager {

    private static volatile UserManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean firebaseAvailable;
    private final SharedPreferences prefs;

    private String userId, nickname;
    private OnUserReadyListener listener;

    public interface OnUserReadyListener {
        void onReady(String userId, String nickname);
    }

    private UserManager(Context ctx) {
        prefs = ctx.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        nickname = prefs.getString("nickname", "健身达人");
        try {
            FirebaseApp.initializeApp(ctx.getApplicationContext());
            auth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            firebaseAvailable = true;
        } catch (Exception e) {
            firebaseAvailable = false;
            userId = "offline_user";
        }
    }

    public static UserManager get(Context ctx) {
        if (instance == null) {
            synchronized (UserManager.class) {
                if (instance == null) {
                    instance = new UserManager(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public boolean isFirebaseAvailable() { return firebaseAvailable; }

    /** 自动匿名登录。不可用时直接回退离线模式 */
    public void signIn(OnUserReadyListener listener) {
        this.listener = listener;
        if (!firebaseAvailable) {
            listener.onReady(userId, nickname);
            return;
        }
        try {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                onSignedIn(user);
            } else {
                auth.signInAnonymously().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        onSignedIn(task.getResult().getUser());
                    } else {
                        userId = "offline_user";
                        listener.onReady(userId, nickname);
                    }
                });
            }
        } catch (Exception e) {
            userId = "offline_user";
            listener.onReady(userId, nickname);
        }
    }

    private void onSignedIn(FirebaseUser user) {
        userId = user.getUid();
        try {
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
        } catch (Exception e) {
            if (listener != null) listener.onReady(userId, nickname);
        }
    }

    public void setNickname(String name) {
        this.nickname = name;
        prefs.edit().putString("nickname", name).apply();
    }

    public String getUserId() { return userId; }
    public String getNickname() { return nickname; }
}
