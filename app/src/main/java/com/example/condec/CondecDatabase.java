package com.example.condec;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

import com.example.condec.Database.*;

@Database(entities = {DefaultBlockedUrl.class, UserBlockedUrl.class}, version = 1)
public abstract class CondecDatabase extends RoomDatabase {
    public abstract DefaultBlockedUrlDao defaultBlockedUrlDao();
    public abstract UserBlockedUrlDao userBlockedUrlDao();

    private static volatile CondecDatabase INSTANCE;

    public static CondecDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (CondecDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    CondecDatabase.class, "CondecDatabase")
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            Executors.newSingleThreadExecutor().execute(() -> {
                DefaultBlockedUrlDao dao = INSTANCE.defaultBlockedUrlDao();
                dao.insertAll(
                        new DefaultBlockedUrl("www.pornhub.com")
                );
            });
        }
    };
}
