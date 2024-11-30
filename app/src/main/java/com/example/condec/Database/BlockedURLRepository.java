package com.example.condec.Database;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Transformations;

import com.example.condec.CondecDatabase;
import com.example.condec.CondecVPNService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockedURLRepository {
    private final DefaultBlockedUrlDao defaultBlockedUrlDao;
    public final UserBlockedUrlDao userBlockedUrlDao;
    public final ExecutorService executorService;

    public BlockedURLRepository(Application application) {
        CondecDatabase db = CondecDatabase.getDatabase(application);
        defaultBlockedUrlDao = db.defaultBlockedUrlDao();
        userBlockedUrlDao = db.userBlockedUrlDao();
        executorService = Executors.newSingleThreadExecutor();
    }
    public LiveData<List<UserBlockedUrl>> getUserBlockedUrls() {
        return userBlockedUrlDao.getAllUrls();
    }
    public void removeUserBlockedUrl(UserBlockedUrl userBlockedUrl) {

        userBlockedUrlDao.delete(userBlockedUrl);

    }

    public void insertDefaultBlockedUrl(final DefaultBlockedUrl url) {
        executorService.execute(() -> defaultBlockedUrlDao.insert(url));
    }

    public void insertDefaultBlockedUrls(final DefaultBlockedUrl... urls) {
        executorService.execute(() -> defaultBlockedUrlDao.insertAll(urls));
    }

    public void insertUserBlockedUrl(final UserBlockedUrl url) {
        executorService.execute(() -> userBlockedUrlDao.insert(url));
    }

    public void insertUserBlockedUrls(final UserBlockedUrl... urls) {
        executorService.execute(() -> userBlockedUrlDao.insertAll(urls));
    }

    public List<String> getAllBlockedUrlsSync() {
        List<String> allUrls = new ArrayList<>();
        allUrls.addAll(defaultBlockedUrlDao.getAllUrlsSync());
        allUrls.addAll(userBlockedUrlDao.getAllUrlsSync());
        return allUrls;
    }

    public LiveData<List<String>> getAllBlockedUrls() {

        LiveData<List<DefaultBlockedUrl>> defaultUrls = defaultBlockedUrlDao.getAllUrls();
        LiveData<List<UserBlockedUrl>> userUrls = userBlockedUrlDao.getAllUrls();


        MediatorLiveData<List<String>> result = new MediatorLiveData<>();

        result.addSource(defaultUrls, urls -> {
            List<String> allUrls = new ArrayList<>();
            for (DefaultBlockedUrl url : urls) {
                allUrls.add(url.getUrl());
            }
            result.setValue(allUrls);
        });

        result.addSource(userUrls, urls -> {
            List<String> allUrls = result.getValue() != null ? new ArrayList<>(result.getValue()) : new ArrayList<>();
            for (UserBlockedUrl url : urls) {
                allUrls.add(url.getUrl());
            }
            result.setValue(allUrls);
        });

        return result;
    }

    private List<String> combineLists(List<DefaultBlockedUrl> defaultUrls, List<UserBlockedUrl> userUrls) {
        List<String> combinedUrls = new ArrayList<>();
        if (defaultUrls != null) {
            for (DefaultBlockedUrl urlEntity : defaultUrls) {
                combinedUrls.add(urlEntity.getUrl());
            }
        }
        if (userUrls != null) {
            for (UserBlockedUrl urlEntity : userUrls) {
                combinedUrls.add(urlEntity.getUrl());
            }
        }
        return combinedUrls;
    }
}
