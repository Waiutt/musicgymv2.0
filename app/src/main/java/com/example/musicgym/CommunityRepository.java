package com.example.musicgym;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Firestore 社区数据层。Firebase 不可用时所有操作静默降级 */
public class CommunityRepository {

    private FirebaseFirestore db;
    private boolean available;

    public CommunityRepository() {
        try {
            db = FirebaseFirestore.getInstance();
            available = true;
        } catch (Exception e) {
            available = false;
        }
    }

    public boolean isAvailable() { return available; }

    // ═══════════ 帖子 ═══════════

    public void publishPost(String userId, String nickname, String title,
                             String content, String imageUri,
                             OnCompleteListener<String> listener) {
        if (!available) { if (listener != null) listener.onResult("local_offline"); return; }
        try {
            Map<String, Object> post = new HashMap<>();
            post.put("userId", userId); post.put("nickname", nickname);
            post.put("title", title); post.put("content", content);
            post.put("imageUri", imageUri != null ? imageUri : "");
            post.put("timestamp", System.currentTimeMillis());
            post.put("likes", new ArrayList<String>());
            post.put("comments", new ArrayList<Map<String, Object>>());
            db.collection("posts").add(post)
                    .addOnSuccessListener(doc -> { if (listener != null) listener.onResult(doc.getId()); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onResult(null); });
        } catch (Exception e) { if (listener != null) listener.onResult(null); }
    }

    public void loadPosts(OnPostsLoadedListener listener) {
        if (!available) { if (listener != null) listener.onLoaded(null); return; }
        try {
            db.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING).limit(50).get()
                    .addOnCompleteListener(task -> {
                        List<CommunityPost> result = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot doc : task.getResult()) result.add(docToPost(doc));
                        }
                        if (listener != null) {
                            if (!task.isSuccessful()) listener.onLoaded(null); // network error
                            else listener.onLoaded(result);
                        }
                    });
        } catch (Exception e) { if (listener != null) listener.onLoaded(null); }
    }

    public void loadPost(String postId, OnPostLoadedListener listener) {
        if (!available) { if (listener != null) listener.onLoaded(null); return; }
        try {
            db.collection("posts").document(postId).get()
                    .addOnCompleteListener(task -> {
                        CommunityPost post = null;
                        if (task.isSuccessful() && task.getResult() != null) post = docToPost(task.getResult());
                        if (listener != null) listener.onLoaded(post);
                    });
        } catch (Exception e) { if (listener != null) listener.onLoaded(null); }
    }

    // ═══════════ 点赞 ═══════════

    public void toggleLike(String postId, String userId, OnCompleteListener<Boolean> listener) {
        if (!available) return;
        try {
            db.collection("posts").document(postId).get().addOnSuccessListener(doc -> {
                List<String> likes = (List<String>) doc.get("likes");
                if (likes == null) likes = new ArrayList<>();
                if (likes.contains(userId))
                    db.collection("posts").document(postId).update("likes", FieldValue.arrayRemove(userId));
                else
                    db.collection("posts").document(postId).update("likes", FieldValue.arrayUnion(userId));
                if (listener != null) listener.onResult(!likes.contains(userId));
            });
        } catch (Exception ignored) {}
    }

    // ═══════════ 评论 ═══════════

    public void addComment(String postId, String userId, String nickname, String text,
                            OnCompleteListener<Boolean> listener) {
        if (!available) return;
        try {
            Map<String, Object> comment = new HashMap<>();
            comment.put("userId", userId); comment.put("nickname", nickname);
            comment.put("text", text); comment.put("timestamp", System.currentTimeMillis());
            db.collection("posts").document(postId).update("comments", FieldValue.arrayUnion(comment))
                    .addOnSuccessListener(v -> { if (listener != null) listener.onResult(true); })
                    .addOnFailureListener(e -> { if (listener != null) listener.onResult(false); });
        } catch (Exception ignored) {}
    }

    // ═══════════ 工具 ═══════════

    private CommunityPost docToPost(DocumentSnapshot doc) {
        CommunityPost p = new CommunityPost();
        p.id = doc.getId();
        p.userId = doc.getString("userId"); p.nickname = doc.getString("nickname");
        p.title = doc.getString("title"); p.content = doc.getString("content");
        p.imageUri = doc.getString("imageUri");
        Long ts = doc.getLong("timestamp"); p.timestamp = ts != null ? ts : 0;
        List<String> likes = (List<String>) doc.get("likes");
        p.likeCount = likes != null ? likes.size() : 0;
        List<Map<String, Object>> comments = (List<Map<String, Object>>) doc.get("comments");
        p.commentCount = comments != null ? comments.size() : 0;
        p.comments = comments != null ? comments : new ArrayList<>();
        return p;
    }

    public static class CommunityPost {
        public String id, userId, nickname, title, content, imageUri;
        public long timestamp;
        public int likeCount, commentCount;
        public List<Map<String, Object>> comments;
    }

    public interface OnCompleteListener<T> { void onResult(T result); }
    public interface OnPostsLoadedListener { void onLoaded(List<CommunityPost> posts); }
    public interface OnPostLoadedListener { void onLoaded(CommunityPost post); }
}
