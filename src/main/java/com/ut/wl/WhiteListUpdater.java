package com.ut.wl;


import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.EngineIOException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.WhiteList;
import net.minecraft.server.management.WhitelistEntry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Pattern;

import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import io.socket.emitter.Emitter.Listener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import static com.ut.wl.WL.LOGGER;

@Mod.EventBusSubscriber
public class WhiteListUpdater {

    private final SimpleCommandExceptionType PLAYER_ALREADY_WHITELISTED = new SimpleCommandExceptionType(new TranslationTextComponent("commands.whitelist.add.failed"));
    private final SimpleCommandExceptionType PLAYER_NOT_WHITELISTED = new SimpleCommandExceptionType(new TranslationTextComponent("commands.whitelist.remove.failed"));
    private final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    private final String da_url = "https://socket.donationalerts.ru:443";
    private Listener donationListener;
    private Listener errorListener;
    private Listener connectListener;
    private Socket sock;
    private JSONObject json;
//    private final IO.Options options = IO.Options.builder()
//            .setReconnection(true)
//            .setReconnectionAttempts(Integer.MAX_VALUE)
//            .setReconnectionDelay(1_000)
//            .setReconnectionDelayMax(5_000)
//            .setRandomizationFactor(0.5)
//            .setTimeout(20_000)
//            .build();

    public void StartSyncing() {
        LOGGER.info("Well, lets try to make DonationAlerts thingie work");
        try {
//            IO.Options opt = new IO.Options();
//            opt.forceNew = true;
//            opt.path = "/wss";
            sock = IO.socket(da_url);
        } catch(URISyntaxException e) {
            LOGGER.info("Ну пиздец, урл умер");
        }
        donationListener = new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                LOGGER.info(new JSONObject((String)arg0[0]).getString("username"));
                LOGGER.info("amount_main: " + new JSONObject((String)arg0[0]).getInt("amount_main"));
                LOGGER.info(new JSONObject((String)arg0[0]).toString());
                processDonate((String)arg0[0]);

            }
        };
        errorListener = new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                LOGGER.info("THERE IS SOME PROBLEM!!");
                EngineIOException obj = (EngineIOException)arg0[0];
                LOGGER.info(obj.getMessage());
                LOGGER.info(ExceptionUtils.getStackTrace(obj));

//                JSONObject error = (JSONObject) args[0];
//                String message = error.getString("message");
//                System.out.println(error); // not authorized
//                JSONObject data = error.getJSONObject("data"); // additional details (optional)
                LOGGER.info(obj.code.toString());
            }
        };
        connectListener = new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                LOGGER.info("Connection with donationalerts server has been established!");
            }
        };
        try {
            connectAndSetListeners();
        } catch(JSONException e){
            LOGGER.info(e.getMessage());
        }
//        run();
    }




    private void processDonate(String data){
        json = new JSONObject(data);
        ResultSet rslt = null;
        Statement stmt = null;
        String query = null;
        String username = json.getString("username");
        String regexp = "[a-zA-Z0-9_]{3,16}";
        String regexp2 = "[a-zA-Z0-9]*_?[a-zA-Z0-9]*";
        if(!(Pattern.matches(regexp, username) && Pattern.matches(regexp2, username))){
            LOGGER.info("Some dumbass forgot how to read or has severe brain damage so he cant understand any of those fucking rules that we have");
            LOGGER.info("Thats his 'name': " + username);
            LOGGER.info("Expect him to reach out soon");
        } else {
            double amount_in_rubbles = json.getDouble("amount_main");
            int months_to_add = (int) amount_in_rubbles / 50;
            WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
            UUID offlineUUID = PlayerEntity.getOfflineUUID(username);
            GameProfile donaterProfile = new GameProfile(offlineUUID, username);
            if (months_to_add > 0) {
                try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                    stmt = conn.createStatement();
                    DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");
                    if (whitelist.isWhitelisted(donaterProfile)) {
                        ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
                        DateTime end_datetime_from_db = null;
                        DateTime new_datetime = null;
                        while (results.next()) {
                            end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
                        }
                        new_datetime = end_datetime_from_db.plusMonths(months_to_add);
                        LOGGER.info("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit");
                        query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
                        LOGGER.info("End DateTime from database: " + end_datetime_from_db.toString(format));
                        LOGGER.info("New DateTime" + new_datetime.toString(format));
                    } else {
                        DateTime current_time = new DateTime();
                        DateTime current_time_plus = current_time.plusMonths(months_to_add);
                        LOGGER.info("New player: " + username + " got added for " + months_to_add + " month(s)");
                        LOGGER.info(current_time.toString(format));
                        LOGGER.info(current_time_plus.toString(format));
                        query = "insert into users (`username`,`start_datetime`,`end_datetime`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "')";
                    }
                    rslt = stmt.executeQuery(query);
                    run(); // sync whitelist with database
                } catch (Exception e) {
                    LOGGER.info(e.getMessage());
                }
            } else {
                LOGGER.info("Whitelist entry with the name: " + username + " got denied, sum is less than 50 rubles");
                LOGGER.info("Money donated: " + amount_in_rubbles);
                LOGGER.info("You better expect someone to reach you out soon, admin");
            }
        }
    }


    public void connectAndSetListeners() throws JSONException {
        String _token = Config.da_token;
        LOGGER.info(_token);
        sock.connect();
        LOGGER.info("Sending token to DA server");
        sock.emit("add-user", new JSONObject()
                .put("token", _token)
                .put("type", "minor"));

        sock.on(Socket.EVENT_CONNECT, connectListener)
                .on(Socket.EVENT_CONNECT_ERROR, errorListener)
                .on("donation", donationListener);
        LOGGER.info("Listeners for events has been set");
    }

    public void run() {
        try
        {
//            Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password);
//            Statement stmt = conn.createStatement();
//            ResultSet rslt = stmt.executeQuery("SELECT username, uuid FROM users");
//            conn.close();
            ResultSet rslt = null;
            Statement stmt = null;
            ResultSet deleteRslt = null;
            ArrayList<String> DbPlayers = new ArrayList<>();
            try(Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)){
                stmt = conn.createStatement();
                deleteRslt = stmt.executeQuery("DELETE FROM users WHERE users.end_datetime <= (NOW() - INTERVAL 12 HOUR)");
                rslt = stmt.executeQuery("SELECT username FROM users");
                while(rslt.next()){
                    DbPlayers.add(rslt.getString(1));
                }
                try { conn.close(); } catch (Exception e) { /* ignored */ }
            } catch (Exception e){
                LOGGER.info(e.getMessage());
            } finally {
            try { rslt.close(); } catch (Exception e) { /* ignored */ }
            try { stmt.close(); } catch (Exception e) { /* ignored */ }
        }

            ArrayList<GameProfile> databaseProfiles = new ArrayList<>();
            for(String u : DbPlayers){
                UUID offlineUUID = PlayerEntity.getOfflineUUID(u);
                databaseProfiles.add(new GameProfile(offlineUUID, u));
            }

            ArrayList<GameProfile> whitelistProfiles = new ArrayList<>();
            try(BufferedReader bufferedReader = new BufferedReader(new FileReader(FMLPaths.GAMEDIR.get().resolve("whitelist.json").toString()))) {
                ArrayList<UserMapping> whitelistPlayersList = new ArrayList<>(Arrays.asList(new Gson().fromJson(bufferedReader, UserMapping[].class)));
                for(UserMapping user : whitelistPlayersList){
                    whitelistProfiles.add(new GameProfile(UUID.fromString(user.uuid), user.name));
                }
            } catch (Exception e){
                LOGGER.info(e.getMessage());
                LOGGER.info("Something got fucked up during attempt to create a list of players from whitelist");
            }
            removePlayers(server, whitelistProfiles);
            addPlayers(server, databaseProfiles);
            reload(server, "Server's whitelist got synced with database");
        }
        catch (Exception e)
        {
            LOGGER.info(e.getMessage());
            LOGGER.info("Something went wrong while.. doing database things! \n");
        }
    }
    private int reload(MinecraftServer server, String message) {
        server.getCommandSource();
        server.getPlayerList().reloadWhitelist();
        LOGGER.info(message);
        server.kickPlayersNotWhitelisted(server.getCommandSource());
        return 1;
    }
    private int addPlayers(MinecraftServer server, Collection<GameProfile> players) throws CommandSyntaxException {
        WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
        int i = 0;

        for(GameProfile gameprofile : players) {
            if (!whitelist.isWhitelisted(gameprofile)) {
                WhitelistEntry whitelistentry = new WhitelistEntry(gameprofile);
                whitelist.addEntry(whitelistentry);
                ++i;
            }
        }
        return i;
//        if (i == 0) {
//            throw PLAYER_ALREADY_WHITELISTED.create();
//        } else {
//            return i;
//        }
    }

    private int removePlayers(MinecraftServer server, Collection<GameProfile> players) throws CommandSyntaxException {
        WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
        int i = 0;

        for(GameProfile gameprofile : players) {
            if (whitelist.isWhitelisted(gameprofile)) {
                WhitelistEntry whitelistentry = new WhitelistEntry(gameprofile);
                whitelist.removeEntry(whitelistentry);
                ++i;
            }
        }
        return i;
//        if (i == 0) {
//            throw PLAYER_NOT_WHITELISTED.create();
//        } else {
////            server.kickPlayersNotWhitelisted(server.getCommandSource());
//            return i;
//        }
    }

    public void addPlayerFromCommand(String username, String months){
        ResultSet rslt = null;
        Statement stmt = null;
        String query = null;
        String regexp = "[a-zA-Z0-9_]{3,16}";
        String regexp2 = "[a-zA-Z0-9]*_?[a-zA-Z0-9]*";
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");

        Config.offStatus();

        if(!(Pattern.matches(regexp, username) && Pattern.matches(regexp2, username))){
            LOGGER.info("I'll remind you of our username policy, 3-16 chars latin + nums and 1 _ (underscore)");
            LOGGER.info("Now try this command once again");
        } else {
            try {
                int months_to_add = Integer.parseInt(months);
                LOGGER.info("Months to add/subtract: " + months_to_add);
                WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
                UUID offlineUUID = PlayerEntity.getOfflineUUID(username);
                GameProfile playerProfile = new GameProfile(offlineUUID, username);
                if (months_to_add > 0) {
                    try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                        stmt = conn.createStatement();
                        if (whitelist.isWhitelisted(playerProfile)) {
                            ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
                            DateTime end_datetime_from_db = null;
                            DateTime new_datetime = null;
                            while (results.next()) {
                                end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
                            }
                            new_datetime = end_datetime_from_db.plusMonths(months_to_add);
                            LOGGER.info("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit");
                            query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
                            LOGGER.info("End DateTime from database: " + end_datetime_from_db.toString(format));
                            LOGGER.info("New DateTime: " + new_datetime.toString(format));
                        } else {
                            DateTime current_time = new DateTime();
                            DateTime current_time_plus = current_time.plusMonths(months_to_add);
                            LOGGER.info("New player: " + username + " got added for " + months_to_add + " month(s)");
                            LOGGER.info(current_time.toString(format));
                            LOGGER.info(current_time_plus.toString(format));
                            query = "insert into users (`username`,`start_datetime`,`end_datetime`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "')";
                        }
                        rslt = stmt.executeQuery(query);
                        LOGGER.info("So called 'po blatu', huh :^)");
                        run(); // sync whitelist with database
                    } catch (Exception e) {
                        LOGGER.info(e.getMessage());
                    }
                } else {
                    if(months_to_add == 0){
                        LOGGER.info("I mean.. how can i add or remove something with the value of ZERO MONTHS??? FOR FUCKS SAKE MAN");
                    } else {
                        LOGGER.info("Well, lets subtract some months from somebody");
                        try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                            stmt = conn.createStatement();
                            if (whitelist.isWhitelisted(playerProfile)) {
                                ResultSet results = stmt.executeQuery("select end_datetime from users where username = '" + username + "'");
                                DateTime end_datetime_from_db = null;
                                DateTime current_datetime = new DateTime();
                                DateTime new_datetime = null;
                                months_to_add = months_to_add * -1;
                                while (results.next()) {
                                    end_datetime_from_db = new DateTime(results.getTimestamp("end_datetime"));
                                }
                                if(end_datetime_from_db.getMonthOfYear() - current_datetime.getMonthOfYear() <= 0){
                                    LOGGER.info("This player has less than 1 month of playtime left, if you want to, just delete him, you know the command");
                                } else {
                                    new_datetime = end_datetime_from_db.minusMonths(months_to_add);
                                    LOGGER.info("Player with the name: " + username + ". Subtracting " + months_to_add + " month(s) from his limit");
                                    query = "update users set end_datetime = '" + new_datetime.toString(format) + "' where username = '" + username + "'";
                                    LOGGER.info("End DateTime from database: " + end_datetime_from_db.toString(format));
                                    LOGGER.info("New DateTime: " + new_datetime.toString(format));
                                    rslt = stmt.executeQuery(query);
                                    run(); // sync whitelist with database
                                }
                            } else {
                                LOGGER.info("This player is not whitelisted, i cant subtract months from nothing, you know");
                            }
                        } catch (Exception e) {
                            LOGGER.info(e.getMessage());
                        }

                    }
                }
            } catch (NumberFormatException e){
                LOGGER.info("Your 'months' doesnt seem like a number to me, try again");
            }
        }
        Config.onStatus();
    }

    public void deletePlayer(String username){
        WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
        UUID offlineUUID = PlayerEntity.getOfflineUUID(username);
        GameProfile playerProfile = new GameProfile(offlineUUID, username);
        ResultSet deleteRslt = null;
        Statement stmt = null;

        Config.offStatus();

        if(whitelist.isWhitelisted(playerProfile)){
            LOGGER.info("Attempting to remove the player");
            try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                stmt = conn.createStatement();
                deleteRslt = stmt.executeQuery("DELETE FROM users where username = '" + username + "'");
                run();
            } catch (Exception e) {
                LOGGER.info(e.getMessage());
            }
        } else {
            LOGGER.info("Player not whitelisted");
        }

        Config.onStatus();
    }


    public void full_restart(){
        Config.onStatus();

        LOGGER.info("Commencing full restart");
        Config.setupDatabaseConfig();
        sock.close();
        StartSyncing();

        Config.offStatus();
    }

    public void reload_from_db(){
        reload(server, "Server's whitelist got synced with database per Admin request");
    }

}
