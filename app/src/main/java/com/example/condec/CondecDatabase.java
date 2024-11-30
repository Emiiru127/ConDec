package com.example.condec;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
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
                            .addMigrations(MIGRATION_1_2) // Add migration here
                            .addMigrations(MIGRATION_1_3) // Add migration here
                            .addMigrations(MIGRATION_1_4) // Add migration here
                            .addMigrations(MIGRATION_1_5) // Add migration here
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
                DefaultBlockedUrlDao defaultDao = INSTANCE.defaultBlockedUrlDao();
                UserBlockedUrlDao userDao = INSTANCE.userBlockedUrlDao();
                Log.d("Condec Database", "Database created, inserting initial data.");

                //First Batch Data
                defaultDao.insert(new DefaultBlockedUrl("pornhub.com"));
                defaultDao.insert(new DefaultBlockedUrl("theporndude.com"));
                defaultDao.insert(new DefaultBlockedUrl("xvideos.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx.com"));
                defaultDao.insert(new DefaultBlockedUrl("toppornsites.com"));
                defaultDao.insert(new DefaultBlockedUrl("eporner.com"));
                defaultDao.insert(new DefaultBlockedUrl("freesafeporn.com"));
                defaultDao.insert(new DefaultBlockedUrl("pornmate.com"));
                defaultDao.insert(new DefaultBlockedUrl("youjizz.com"));
                defaultDao.insert(new DefaultBlockedUrl("spankbang.com"));
                defaultDao.insert(new DefaultBlockedUrl("thebestfetishsites.com"));
                defaultDao.insert(new DefaultBlockedUrl("rapbeh.net"));
                defaultDao.insert(new DefaultBlockedUrl("redtube.com"));
                defaultDao.insert(new DefaultBlockedUrl("porn.com"));
                defaultDao.insert(new DefaultBlockedUrl("bellesa.co"));
                defaultDao.insert(new DefaultBlockedUrl("xhamster.com"));
                defaultDao.insert(new DefaultBlockedUrl("mypornbible.com"));
                defaultDao.insert(new DefaultBlockedUrl("urporn.com"));
                defaultDao.insert(new DefaultBlockedUrl("porngeek.com"));
                defaultDao.insert(new DefaultBlockedUrl("similar.porn"));
                defaultDao.insert(new DefaultBlockedUrl("Kink.com"));
                defaultDao.insert(new DefaultBlockedUrl("Brazzers.com"));
                defaultDao.insert(new DefaultBlockedUrl("YouPorn.com"));
                defaultDao.insert(new DefaultBlockedUrl("Omegle.com"));
                defaultDao.insert(new DefaultBlockedUrl("Teenchat.com"));
                defaultDao.insert(new DefaultBlockedUrl("Wireclub.com"));
                defaultDao.insert(new DefaultBlockedUrl("Tinder.com"));
                defaultDao.insert(new DefaultBlockedUrl("Match.com"));
                defaultDao.insert(new DefaultBlockedUrl("Bumble.com"));
                defaultDao.insert(new DefaultBlockedUrl("Grindr.com"));
                defaultDao.insert(new DefaultBlockedUrl("AshleyMadison.com"));
                defaultDao.insert(new DefaultBlockedUrl("AdultFriendFinder.com"));
                defaultDao.insert(new DefaultBlockedUrl("RoyalAceCasino.com"));
                defaultDao.insert(new DefaultBlockedUrl("PokerStars.com"));
                defaultDao.insert(new DefaultBlockedUrl("888casino.com"));
                defaultDao.insert(new DefaultBlockedUrl("SportsBetting.ag"));
                defaultDao.insert(new DefaultBlockedUrl("DailyStormer.su"));

                //Second Batch Data
                defaultDao.insert(new DefaultBlockedUrl("onlyfans.com"));
                defaultDao.insert(new DefaultBlockedUrl("patreon.com"));
                defaultDao.insert(new DefaultBlockedUrl("deviantart.com"));
                defaultDao.insert(new DefaultBlockedUrl("fuq.com"));
                defaultDao.insert(new DefaultBlockedUrl("motherless.com"));
                defaultDao.insert(new DefaultBlockedUrl("aznude.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx2.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx3.com"));
                defaultDao.insert(new DefaultBlockedUrl("gelbooru.com"));
                defaultDao.insert(new DefaultBlockedUrl("mangapark.net"));
                defaultDao.insert(new DefaultBlockedUrl("hentaila.com"));
                defaultDao.insert(new DefaultBlockedUrl("pornhat.one"));
                defaultDao.insert(new DefaultBlockedUrl("tktube.com"));
                defaultDao.insert(new DefaultBlockedUrl("fanbox.cc"));
                defaultDao.insert(new DefaultBlockedUrl("hentaihaven.xxx"));
                defaultDao.insert(new DefaultBlockedUrl("yandex.com.tr"));
                defaultDao.insert(new DefaultBlockedUrl("sxyprn.net"));
                defaultDao.insert(new DefaultBlockedUrl("hdtube.porn"));
                defaultDao.insert(new DefaultBlockedUrl("fpo.xxx"));
                defaultDao.insert(new DefaultBlockedUrl("beeg.com"));
                defaultDao.insert(new DefaultBlockedUrl("nhentai.to"));
                defaultDao.insert(new DefaultBlockedUrl("hentai2read.com"));
                defaultDao.insert(new DefaultBlockedUrl("ouo.io"));
                defaultDao.insert(new DefaultBlockedUrl("sportingbet.com"));

                //Third Batch Data
                defaultDao.insert(new DefaultBlockedUrl("bk8link1.com"));
                defaultDao.insert(new DefaultBlockedUrl("25.aw8pro1.asia"));
                defaultDao.insert(new DefaultBlockedUrl("bk8links8.com"));
                defaultDao.insert(new DefaultBlockedUrl("casinoplus.com.ph"));
                defaultDao.insert(new DefaultBlockedUrl("onlinecasinoph.net"));
                defaultDao.insert(new DefaultBlockedUrl("bing.com"));
                defaultDao.insert(new DefaultBlockedUrl("yahoo.com"));
                defaultDao.insert(new DefaultBlockedUrl("duckduckgo.com"));
                defaultDao.insert(new DefaultBlockedUrl("ecosia.org"));
                defaultDao.insert(new DefaultBlockedUrl("baidu.com"));
                defaultDao.insert(new DefaultBlockedUrl("yandex.com"));
                defaultDao.insert(new DefaultBlockedUrl("ask.com"));
                defaultDao.insert(new DefaultBlockedUrl("search.aol.com"));
                defaultDao.insert(new DefaultBlockedUrl("startpage.com"));
                defaultDao.insert(new DefaultBlockedUrl("qwant.com"));
                defaultDao.insert(new DefaultBlockedUrl("gigablast.com"));
                defaultDao.insert(new DefaultBlockedUrl("searchencrypt.com"));
                defaultDao.insert(new DefaultBlockedUrl("mojeek.com"));
                defaultDao.insert(new DefaultBlockedUrl("searx.me"));
                defaultDao.insert(new DefaultBlockedUrl("wolframalpha.com"));
                defaultDao.insert(new DefaultBlockedUrl("boardreader.com"));
                defaultDao.insert(new DefaultBlockedUrl("lite.duckduckgo.com"));
                defaultDao.insert(new DefaultBlockedUrl("infotiger.com"));
                defaultDao.insert(new DefaultBlockedUrl("metager.org"));
                defaultDao.insert(new DefaultBlockedUrl("fruzo.com"));

            });
        }
    };

    // Define migration for version 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add unique index to default_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_default_blocked_urls_url ON default_blocked_urls(url)");

            // Add unique index to user_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_user_blocked_urls_url ON user_blocked_urls(url)");
        }
    };
    static final Migration MIGRATION_1_3 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add unique index to default_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_default_blocked_urls_url ON default_blocked_urls(url)");

            // Add unique index to user_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_user_blocked_urls_url ON user_blocked_urls(url)");
        }
    };

    static final Migration MIGRATION_1_4 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add unique index to default_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_default_blocked_urls_url ON default_blocked_urls(url)");

            // Add unique index to user_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_user_blocked_urls_url ON user_blocked_urls(url)");
        }
    };

    static final Migration MIGRATION_1_5 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add unique index to default_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_default_blocked_urls_url ON default_blocked_urls(url)");

            // Add unique index to user_blocked_urls table
            database.execSQL("CREATE UNIQUE INDEX index_user_blocked_urls_url ON user_blocked_urls(url)");
        }
    };
}
