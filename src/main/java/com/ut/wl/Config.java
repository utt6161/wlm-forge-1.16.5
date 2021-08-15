package com.ut.wl;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import static com.ut.wl.WL.LOGGER;

import java.io.File;

@Mod.EventBusSubscriber()
public class Config {

    public static ForgeConfigSpec SERVER_CONFIG;

    public static String url;
    public static String user;
    public static String password;
    public static String driver;
    public static String kickphrase;
    public static int whitelistreloadtime;
    public static int monthlypayment;
    public static String da_token;
    public static int minutes;
    public static boolean toggle;
    private static String configDir;
    public static String status;

    static {
        setupDatabaseConfig();
    }

    public static void setupDatabaseConfig() {
//        SERVER_BUILDER.comment("FirstBlock settings").push(DB_CONNECT);
        configDir = FMLPaths.CONFIGDIR.get().resolve("wl-server.toml").toString();
        File file = new File(configDir); // load from folder
        FileConfig config;
        if(file.isFile() && file.exists()){ // if the file exists...
            config = FileConfig.of(file);
            config.load();
            WL.LOGGER.info("CONFIG FILES CONTENT: " + file.toString());
//            driver = config.get("driver");
        } else {
            config = FileConfig.of(file);
            config.set("url","jdbc:mariadb://localhost:3306/minedb");
            config.set("user", "root");
            config.set("password","1234");
            config.set("kickphrase","pay up, bud");
            config.set("whitelistreloadtime",1200); // 30 seconds
            config.set("da_token", "PASS YOUR TOKEN HERE");
            config.set("monthlypayment", 50);
            config.set("minutes", 1);
            config.set("toggle", true);
            config.save();
//            WL.LOGGER.info("Connection data: " + url + " " + user + " " + password);
//            driver = config
//                    .set("driver", "org.mariadb.jdbc.Driver");
        }

        url = config.get("url");
        user = config.get("user");
        password = config.get("password");
        kickphrase = config.get("kickphrase");
        whitelistreloadtime = config.getInt("whitelistreloadtime");
        da_token = config.get("da_token");
        monthlypayment = config.get("monthlypayment");
        minutes = config.get("minutes");
        toggle = config.get("toggle");
        if(toggle){
            status = "Timer for whitelist reload is ON";
        } else {
            status = "Timer for whitelist reload is OFF";
        }

        config.close();
        CommentedFileConfig cfc = CommentedFileConfig.of(file);
        SERVER_CONFIG = new ForgeConfigSpec.Builder().build();
        SERVER_CONFIG.setConfig(cfc);
//        SERVER_BUILDER.pop();
    }

    public static void toggleReload(){
        if (!toggle) {
            toggle = true ;
            status = "Timer for whitelist reload is ON";
            LOGGER.info(status);
        }
        else {
            toggle = false;
            status = "Timer for whitelist reload is OFF";
            LOGGER.info(status);
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("toggle", toggle);
        config.save();
        config.close();
    }

    public static void changeUrl(String url){
        boolean flag = false; // Assuming that the current status is on
        if(toggle){
            offStatus();
            flag = true;
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("url", url);
        config.save();
        config.close();
        if(flag){
            onStatus();
        }
        LOGGER.info("Url for database access has been changed, next time we'll use this one");
    }

    public static void changeUser(String user){
        boolean flag = false; // Assuming that the current status is on
        if(toggle){
            offStatus();
            flag = true;
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("user", user);
        config.save();
        config.close();
        if(flag){
            onStatus();
        }
        LOGGER.info("User for database access has been changed, next time we'll use this one");
    }

    public static void changePassword(String password){
        boolean flag = false; // Assuming that the current status is on
        if(toggle){
            offStatus();
            flag = true;
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("password", password);
        config.save();
        config.close();
        if(flag){
            onStatus();
        }
        LOGGER.info("Password for database access has been changed, next time we'll use this one");
    }

    public static void changeUserAndPassword(String user, String password){
        boolean flag = false; // Assuming that the current status is on
        if(toggle){
            offStatus();
            flag = true;
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("user", user);
        config.set("password", password);
        config.save();
        config.close();
        if(flag){
            onStatus();
        }
        LOGGER.info("Both user and password for database access has been changed, next time we'll use them instead");
    }


    public static void changeDbCred(String url, String user, String password){
        boolean flag = false; // Assuming that the current status is on
        if(toggle){
            offStatus();
            flag = true;
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("url", url);
        config.set("user", user);
        config.set("password", password);
        config.save();
        config.close();
        if(flag){
            onStatus();
        }
        LOGGER.info("All of the database credentials has been changed, it'll be applied immediately");
    }

    public static void changeReloadTime(String whitelistreloadtime){
        boolean flag = false; // Assuming that the current status is on
        if(toggle){
            offStatus();
            flag = true;
        }
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("whitelistreloadtime", Integer.valueOf(whitelistreloadtime));
        config.save();
        config.close();
        if(flag){
            onStatus();
        }
        LOGGER.info("Whitelist reload time has been changed, if the timer is on, then we'll start counting from zero");
    }

    public static void changeMonthlyPayment(String monthlypayment){
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("monthlypayment", Integer.valueOf(monthlypayment));
        config.save();
        config.close();
        LOGGER.info("Monthly payment has been changed, new donations will have to abide by new sum :^)");
    }

    public static void changeDAToken(String da_token){
        FileConfig config = FileConfig.of(new File(configDir));
        config.load();
        config.set("da_token", da_token);
        config.save();
        config.close();
        LOGGER.info("Token has been changed, use '/wl restart' to apply changes");
    }

    public static void offStatus(){  // USE ONLY IN PAIR WITH ON() !!!
        if(toggle){
            toggle = false;
        }
    }

    public static void onStatus(){  // USE ONLY IN PAIR WITH OFF() !!!
        if(!toggle){
            toggle = true;
        }
    }

    @SubscribeEvent
    public static void onLoad(final ModConfig.Loading configEvent) {

    }

    @SubscribeEvent
    public static void onReload(final ModConfig.Reloading configEvent) {
    }

}
