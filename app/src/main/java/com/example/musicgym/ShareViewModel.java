package com.example.musicgym;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

/** 社区页 ViewModel — 仅管理帖子列表加载状态 */
public class ShareViewModel extends AndroidViewModel {

    private final CommunityRepository repo = new CommunityRepository();
    private final MutableLiveData<List<CommunityRepository.CommunityPost>> posts
            = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("加载中...");

    public ShareViewModel(@NonNull Application app) { super(app); }

    public LiveData<List<CommunityRepository.CommunityPost>> getPosts() { return posts; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getStatusMessage() { return statusMessage; }

    public void loadPosts() {
        isLoading.postValue(true);
        repo.loadPosts(result -> {
            isLoading.postValue(false);
            if (result == null) {
                statusMessage.postValue("社区不可用，请检查网络连接");
                posts.postValue(new ArrayList<>());
            } else {
                posts.postValue(result);
                statusMessage.postValue(result.isEmpty() ? "暂无帖子，点击 + 发布第一条" : "");
            }
        });
    }
}
