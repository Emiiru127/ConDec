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

                //Second Batch Data
                defaultDao.insert(new DefaultBlockedUrl("pornhub.com"));
                defaultDao.insert(new DefaultBlockedUrl("xvideos.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx.com"));
                defaultDao.insert(new DefaultBlockedUrl("onlyfans.com"));
                defaultDao.insert(new DefaultBlockedUrl("patreon.com"));
                defaultDao.insert(new DefaultBlockedUrl("deviantart.com"));
                defaultDao.insert(new DefaultBlockedUrl("xhamsterlive.com"));
                defaultDao.insert(new DefaultBlockedUrl("redtube.com"));
                defaultDao.insert(new DefaultBlockedUrl("rutube.ru"));
                defaultDao.insert(new DefaultBlockedUrl("theporndude.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx.tv"));
                defaultDao.insert(new DefaultBlockedUrl("hqporner.com"));
                defaultDao.insert(new DefaultBlockedUrl("pornzog.com"));
                defaultDao.insert(new DefaultBlockedUrl("twidouga.net"));
                defaultDao.insert(new DefaultBlockedUrl("qorno.com"));
                defaultDao.insert(new DefaultBlockedUrl("cityheaven.net"));
                defaultDao.insert(new DefaultBlockedUrl("pornpics.com"));
                defaultDao.insert(new DefaultBlockedUrl("giphy.com"));
                defaultDao.insert(new DefaultBlockedUrl("iporntv.net"));
                defaultDao.insert(new DefaultBlockedUrl("xvideos.es"));
                defaultDao.insert(new DefaultBlockedUrl("literotica.com"));
                defaultDao.insert(new DefaultBlockedUrl("funnyjunk.com"));
                defaultDao.insert(new DefaultBlockedUrl("fuq.com"));
                defaultDao.insert(new DefaultBlockedUrl("motherless.com"));
                defaultDao.insert(new DefaultBlockedUrl("aznude.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx2.com"));
                defaultDao.insert(new DefaultBlockedUrl("xnxx3.com"));
                defaultDao.insert(new DefaultBlockedUrl("gelbooru.com"));
                defaultDao.insert(new DefaultBlockedUrl("mangapark.net"));
                defaultDao.insert(new DefaultBlockedUrl("hentaila.com"));
                defaultDao.insert(new DefaultBlockedUrl("disqus.com"));
                defaultDao.insert(new DefaultBlockedUrl("xhamster2.com"));
                defaultDao.insert(new DefaultBlockedUrl("himasoku.com"));
                defaultDao.insert(new DefaultBlockedUrl("xvideos.red"));
                defaultDao.insert(new DefaultBlockedUrl("f95zone.to"));
                defaultDao.insert(new DefaultBlockedUrl("1337x.to"));
                defaultDao.insert(new DefaultBlockedUrl("spankbang.party"));
                defaultDao.insert(new DefaultBlockedUrl("cmoa.jp"));
                defaultDao.insert(new DefaultBlockedUrl("jable.tv"));
                defaultDao.insert(new DefaultBlockedUrl("hentairead.com"));
                defaultDao.insert(new DefaultBlockedUrl("hot-sex-tube.com"));
                defaultDao.insert(new DefaultBlockedUrl("boyfriendtv.com"));
                defaultDao.insert(new DefaultBlockedUrl("allmylinks.com"));
                defaultDao.insert(new DefaultBlockedUrl("bakusai.com"));
                defaultDao.insert(new DefaultBlockedUrl("fetlife.com"));
                defaultDao.insert(new DefaultBlockedUrl("tubesafari.com"));
                defaultDao.insert(new DefaultBlockedUrl("allporncomic.com"));
                defaultDao.insert(new DefaultBlockedUrl("clip2vip.com"));
                defaultDao.insert(new DefaultBlockedUrl("hdporncomics.com"));
                defaultDao.insert(new DefaultBlockedUrl("livedoor.biz"));
                defaultDao.insert(new DefaultBlockedUrl("javmost.com"));
                defaultDao.insert(new DefaultBlockedUrl("doeda.com"));
                defaultDao.insert(new DefaultBlockedUrl("ukdevilz.com"));
                defaultDao.insert(new DefaultBlockedUrl("tenor.com"));
                defaultDao.insert(new DefaultBlockedUrl("donmai.us"));
                defaultDao.insert(new DefaultBlockedUrl("socialmediagirls.com"));
                defaultDao.insert(new DefaultBlockedUrl("pornhat.one"));
                defaultDao.insert(new DefaultBlockedUrl("tktube.com"));
                defaultDao.insert(new DefaultBlockedUrl("fanbox.cc"));
                defaultDao.insert(new DefaultBlockedUrl("hentaihaven.xxx"));
                defaultDao.insert(new DefaultBlockedUrl("yandex.com.tr"));
                defaultDao.insert(new DefaultBlockedUrl("sxyprn.net"));
                defaultDao.insert(new DefaultBlockedUrl("hdtube.porn"));
                defaultDao.insert(new DefaultBlockedUrl("vippers.jp"));
                defaultDao.insert(new DefaultBlockedUrl("56.47M	"));
                defaultDao.insert(new DefaultBlockedUrl("sex.com"));
                defaultDao.insert(new DefaultBlockedUrl("joemonster.org"));
                defaultDao.insert(new DefaultBlockedUrl("multporn.net"));
                defaultDao.insert(new DefaultBlockedUrl("bongacams.com"));
                defaultDao.insert(new DefaultBlockedUrl("gaymaletube.com"));
                defaultDao.insert(new DefaultBlockedUrl("fpo.xxx"));
                defaultDao.insert(new DefaultBlockedUrl("javhdporn.net"));
                defaultDao.insert(new DefaultBlockedUrl("porntrex.com"));
                defaultDao.insert(new DefaultBlockedUrl("thepiratebay.org"));
                defaultDao.insert(new DefaultBlockedUrl("movies7.to"));
                defaultDao.insert(new DefaultBlockedUrl("telegra.ph"));
                defaultDao.insert(new DefaultBlockedUrl("luxuretv.com"));
                defaultDao.insert(new DefaultBlockedUrl("hanime1.me"));
                defaultDao.insert(new DefaultBlockedUrl("worldstarhiphop.com"));
                defaultDao.insert(new DefaultBlockedUrl("furaffinity.net"));
                defaultDao.insert(new DefaultBlockedUrl("txxx.com"));
                defaultDao.insert(new DefaultBlockedUrl("fatalmodel.com"));
                defaultDao.insert(new DefaultBlockedUrl("skokka.com"));
                defaultDao.insert(new DefaultBlockedUrl("beeg.com"));
                defaultDao.insert(new DefaultBlockedUrl("nhentai.to"));
                defaultDao.insert(new DefaultBlockedUrl("hentai2read.com"));
                defaultDao.insert(new DefaultBlockedUrl("ouo.io"));
                defaultDao.insert(new DefaultBlockedUrl("xxxvideo.link"));
                defaultDao.insert(new DefaultBlockedUrl("chochox.com"));
                defaultDao.insert(new DefaultBlockedUrl("pornone.com"));
                defaultDao.insert(new DefaultBlockedUrl("ibb.co"));
                defaultDao.insert(new DefaultBlockedUrl("hdabla.net"));
                defaultDao.insert(new DefaultBlockedUrl("hentaiera.com"));
                defaultDao.insert(new DefaultBlockedUrl("brazzersnetwork.com"));
                defaultDao.insert(new DefaultBlockedUrl("hentaila.tv"));
                defaultDao.insert(new DefaultBlockedUrl("stake.com"));
                defaultDao.insert(new DefaultBlockedUrl("betano.com"));
                defaultDao.insert(new DefaultBlockedUrl("bet365.com"));
                defaultDao.insert(new DefaultBlockedUrl("web.de"));
                defaultDao.insert(new DefaultBlockedUrl("gmx.net"));
                defaultDao.insert(new DefaultBlockedUrl("sportybet.com"));
                defaultDao.insert(new DefaultBlockedUrl("bet9ja.com"));
                defaultDao.insert(new DefaultBlockedUrl("caliente.mx"));
                defaultDao.insert(new DefaultBlockedUrl("betway.co.za"));
                defaultDao.insert(new DefaultBlockedUrl("betika.com"));
                defaultDao.insert(new DefaultBlockedUrl("chumbacasino.com"));
                defaultDao.insert(new DefaultBlockedUrl("hollywoodbets.net"));
                defaultDao.insert(new DefaultBlockedUrl("mlive.com"));
                defaultDao.insert(new DefaultBlockedUrl("betfair.com"));
                defaultDao.insert(new DefaultBlockedUrl("bovada.lv"));
                defaultDao.insert(new DefaultBlockedUrl("forebet.com"));
                defaultDao.insert(new DefaultBlockedUrl("national-lottery.co.uk"));
                defaultDao.insert(new DefaultBlockedUrl("betplay.com.co"));
                defaultDao.insert(new DefaultBlockedUrl("gamdom.com"));
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
                defaultDao.insert(new DefaultBlockedUrl("omegle.com"));
                defaultDao.insert(new DefaultBlockedUrl("chatroulette.com"));
                defaultDao.insert(new DefaultBlockedUrl("chathub.com"));
                defaultDao.insert(new DefaultBlockedUrl("tinychat.com"));
                defaultDao.insert(new DefaultBlockedUrl("bazoocam.org"));
                defaultDao.insert(new DefaultBlockedUrl("shagle.com"));
                defaultDao.insert(new DefaultBlockedUrl("coomeet.com"));
                defaultDao.insert(new DefaultBlockedUrl("chatrandom.com"));
                defaultDao.insert(new DefaultBlockedUrl("fruzo.com"));
                defaultDao.insert(new DefaultBlockedUrl("chatterbate.com"));
                defaultDao.insert(new DefaultBlockedUrl("onlyfans.com"));
                defaultDao.insert(new DefaultBlockedUrl("faceflow.com"));
                defaultDao.insert(new DefaultBlockedUrl("chatspin.com"));
                defaultDao.insert(new DefaultBlockedUrl("camsurf.com"));
                defaultDao.insert(new DefaultBlockedUrl("ome.tv"));
                defaultDao.insert(new DefaultBlockedUrl("y99.in"));
                defaultDao.insert(new DefaultBlockedUrl("swebchat.com"));
                defaultDao.insert(new DefaultBlockedUrl("talkwithstranger.com"));
                defaultDao.insert(new DefaultBlockedUrl("randomchat.com"));
                defaultDao.insert(new DefaultBlockedUrl("chitchat.com"));
                defaultDao.insert(new DefaultBlockedUrl("coomeet.com"));

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
}
