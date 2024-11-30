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

@Database(entities = {DefaultBlockedUrl.class, UserBlockedUrl.class}, version = 2)
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

                //Initial data
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
                defaultDao.insert(new DefaultBlockedUrl("toppornsites.co.uk"));
                defaultDao.insert(new DefaultBlockedUrl("Kink.com"));
                defaultDao.insert(new DefaultBlockedUrl("Brazzers.com"));
                defaultDao.insert(new DefaultBlockedUrl("YouPorn.com"));
                defaultDao.insert(new DefaultBlockedUrl("8Tube.xxx"));
                defaultDao.insert(new DefaultBlockedUrl("Omegle.com"));
                defaultDao.insert(new DefaultBlockedUrl("PalTalk.com"));
                defaultDao.insert(new DefaultBlockedUrl("TalkWithStranger.com"));
                defaultDao.insert(new DefaultBlockedUrl("ChatRoulette.com"));
                defaultDao.insert(new DefaultBlockedUrl("Chat-Avenue.com"));
                defaultDao.insert(new DefaultBlockedUrl("Chatango.com"));
                defaultDao.insert(new DefaultBlockedUrl("Teenchat.com"));
                defaultDao.insert(new DefaultBlockedUrl("Wireclub.com"));
                defaultDao.insert(new DefaultBlockedUrl("ChatHour.com"));
                defaultDao.insert(new DefaultBlockedUrl("Chatzy.com"));
                defaultDao.insert(new DefaultBlockedUrl("Chatib.us"));
                defaultDao.insert(new DefaultBlockedUrl("E-Chat.co"));
                defaultDao.insert(new DefaultBlockedUrl("Tinder.com"));
                defaultDao.insert(new DefaultBlockedUrl("Match.com"));
                defaultDao.insert(new DefaultBlockedUrl("Bumble.com"));
                defaultDao.insert(new DefaultBlockedUrl("MeetMe.com"));
                defaultDao.insert(new DefaultBlockedUrl("OKCupid.com"));
                defaultDao.insert(new DefaultBlockedUrl("POF.com"));
                defaultDao.insert(new DefaultBlockedUrl("eHarmony.com"));
                defaultDao.insert(new DefaultBlockedUrl("Zoosk.com"));
                defaultDao.insert(new DefaultBlockedUrl("Hinge.co"));
                defaultDao.insert(new DefaultBlockedUrl("Grindr.com"));
                defaultDao.insert(new DefaultBlockedUrl("AshleyMadison.com"));
                defaultDao.insert(new DefaultBlockedUrl("AdultFriendFinder.com"));
                defaultDao.insert(new DefaultBlockedUrl("BetOnline.ag"));
                defaultDao.insert(new DefaultBlockedUrl("FreeSpin.com"));
                defaultDao.insert(new DefaultBlockedUrl("Bovada.lv"));
                defaultDao.insert(new DefaultBlockedUrl("SlotoCash.im"));
                defaultDao.insert(new DefaultBlockedUrl("RoyalAceCasino.com"));
                defaultDao.insert(new DefaultBlockedUrl("PokerStars.com"));
                defaultDao.insert(new DefaultBlockedUrl("888casino.com"));
                defaultDao.insert(new DefaultBlockedUrl("SportsBetting.ag"));
                defaultDao.insert(new DefaultBlockedUrl("Betway.com"));
                defaultDao.insert(new DefaultBlockedUrl("Slots.com"));
                defaultDao.insert(new DefaultBlockedUrl("LiveLeak.com"));
                defaultDao.insert(new DefaultBlockedUrl("BestGore.com"));
                defaultDao.insert(new DefaultBlockedUrl("TheYNC.com"));
                defaultDao.insert(new DefaultBlockedUrl("DocumentingReality.com"));
                defaultDao.insert(new DefaultBlockedUrl("Ogrish.tv"));
                defaultDao.insert(new DefaultBlockedUrl("Stormfront.org"));
                defaultDao.insert(new DefaultBlockedUrl("4chan.org"));
                defaultDao.insert(new DefaultBlockedUrl("Gab.com"));
                defaultDao.insert(new DefaultBlockedUrl("NationalVanguard.org"));
                defaultDao.insert(new DefaultBlockedUrl("DailyStormer.su"));

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
}
