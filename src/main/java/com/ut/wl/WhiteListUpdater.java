package com.ut.wl;


import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import com.ut.wl.UuidUserResponse;

import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import io.socket.emitter.Emitter.Listener;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Months;
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

    public class DbPlayerProfile{
        public String username;
        public String UUID;

        public DbPlayerProfile(String username, String UUID) {
            this.username = username;
            this.UUID = UUID;
        }
    }

    private static void writeLogToFile(String message) throws IOException {
//        FileHandler handler = new FileHandler("WLM.log", false);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("WLM.log", true)))
        {
            String newline = System.getProperty("line.separator");
            String formatedMessage = message.replaceAll("(\n|\\n|\\\\n)", newline);
            bw.newLine();
            bw.write("[WLM log entry]");
            bw.newLine();
            DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");
            bw.write("Date and time: " + new DateTime().toString(format));
            bw.newLine();
            bw.write(formatedMessage);
            bw.newLine();
        }
        catch (IOException e)
        {
            LOGGER.info(e.getMessage());
        }
//        Logger logger = Logger.getLogger("com.ut.wl");
//        logger.addHandler(handler);
//        logger.info(message);
//        logger.removeHandler(handler);
    }

    public static UUID getUUIDfromUsername(String username) {
        UUID uuid = PlayerEntity.getOfflineUUID(username); // in case we fucked up something, we still have a uuid;

        try {
            LOGGER.info("Attempting to retrieve the UUID from the ely.by server");
            URL url = new URL("https://authserver.ely.by/api/users/profiles/minecraft/" + username);
            UuidUserResponse mapped;
            try {
                // Open a connection(?) on the URL(??) and cast the response(???)
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Now it's "open", we can set the request method, headers etc.
                connection.setRequestProperty("accept", "application/json");
                // This line makes the request
                InputStream responseStream = connection.getInputStream();

                // Manually converting the response body InputStream to APOD using Jackson
                ObjectMapper mapper = new ObjectMapper();
                mapped = mapper.readValue(responseStream, UuidUserResponse.class);
                byte[] data;
                try {
                    uuid = UUID.fromString(mapped.getUuid().replaceFirst( "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5" ));
                    LOGGER.info("Retrieved uuid: " + uuid);
                } catch (IllegalArgumentException e) {
                    LOGGER.info(e.getMessage());
                    LOGGER.info("Something is not right with UUID, failed to extract UUID object");
                    LOGGER.info("Whitelist will be populated with default \"offline player\" UUID");
                    try{
                        writeLogToFile(e.getMessage() + "\n" +
                                        "Something is not right with UUID, failed to extract UUID object\n" +
                                        "Whitelist will be populated with \"offline player\"  UUID\n" +
                                        "Generated UUID: " + uuid + "\n" +
                                        "Username: " + mapped.name + "\n" + "Returned UUID: " + mapped.getUuid());
                        LOGGER.info("Have written entry to the log.");
                    } catch (IOException b){
                        LOGGER.info(b.getMessage());
                        LOGGER.info("Couldnt write down the log entry");
                    }
                }
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
                LOGGER.info("Either Username was not found, or something else happened");
                try{
                    writeLogToFile(e.getMessage() + "\n" +
                            "Either Username was not found, or something else happened\n" +
                            "Whitelist will be populated with \"offline player\"  UUID\n" +
                            "Generated UUID: " + uuid + "\n" +
                            "Username: " + username);
                    LOGGER.info("Have written entry to the log.");
                } catch (IOException b){
                    LOGGER.info(b.getMessage());
                    LOGGER.info("Couldnt write down the log entry");
                }
            }
            // Finally we have the response
        } catch (MalformedURLException e) {
            LOGGER.info(e.getMessage());
            LOGGER.info("No fucking clue what happened, seems like internal URL got broken");
            LOGGER.info("better restart the server and try to contact \"you know who\"");
            try{
                writeLogToFile(e.getMessage() + "\n" +
                        "No fucking clue what happened, seems like internal URL got broken\n" +
                                "better restart the server and try to contact \"you know who\"");
            } catch (IOException b){
                LOGGER.info(b.getMessage());
                LOGGER.info("Couldnt write down the log entry");
            }
        }
        return uuid;
    }

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
        double amount_in_rubbles = json.getDouble("amount_main");
        int months_to_add = (int) amount_in_rubbles / 50;
        if (months_to_add > 0) {
            addPlayerFromCommandOrDonation(username, String.valueOf(months_to_add), true);
        } else {
            LOGGER.info("Whitelist entry with the name: " + username + " got denied, sum is less than 50 rubles");
            LOGGER.info("Money donated: " + amount_in_rubbles);
            LOGGER.info("You better expect someone to reach you out soon, admin");
            try{
                writeLogToFile("Whitelist entry with the name: " + username + " got denied, sum is less than 50 rubles\n" +
                                "Money donated: " + amount_in_rubbles + "\n" +
                                "Expect him to reach out soon");
                LOGGER.info("Have written entry to the log.");
            } catch (IOException e) {
                LOGGER.info(e.getMessage());
                LOGGER.info("Have failed to write entry to the log.");
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
            ArrayList<DbPlayerProfile> DbPlayers = new ArrayList<>();
            try(Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)){
                stmt = conn.createStatement();
                deleteRslt = stmt.executeQuery("DELETE FROM users WHERE users.end_datetime <= (NOW() - INTERVAL 12 HOUR)");
                rslt = stmt.executeQuery("SELECT username, uuid FROM users");
                while(rslt.next()){
                    DbPlayers.add(new DbPlayerProfile(rslt.getString(1), rslt.getString(2)));
                }
                try { conn.close(); } catch (Exception e) { LOGGER.info(e.getMessage()); }
            } catch (Exception e){
                LOGGER.info(e.getMessage());
            } finally {
            try { rslt.close(); } catch (Exception e) { LOGGER.info(e.getMessage()); }
            try { stmt.close(); } catch (Exception e) { LOGGER.info(e.getMessage()); }
        }

            ArrayList<GameProfile> databaseProfiles = new ArrayList<>();
            for(DbPlayerProfile u : DbPlayers){
//                UUID offlineUUID = PlayerEntity.getOfflineUUID(u);
                UUID uuid = UUID.fromString(u.UUID);
                databaseProfiles.add(new GameProfile(uuid, u.username));
            }

//            ArrayList<GameProfile> whitelistProfiles = new ArrayList<>();
//            try(BufferedReader bufferedReader = new BufferedReader(new FileReader(FMLPaths.GAMEDIR.get().resolve("whitelist.json").toString()))) {
//                ArrayList<UserMapping> whitelistPlayersList = new ArrayList<>(Arrays.asList(new Gson().fromJson(bufferedReader, UserMapping[].class)));
//                for(UserMapping user : whitelistPlayersList){
//                    whitelistProfiles.add(new GameProfile(UUID.fromString(user.uuid), user.name));
//                }
//            } catch (Exception e){
//                LOGGER.info(e.getMessage());
//                LOGGER.info("Something got fucked up during attempt to create a list of players from whitelist");
//            }

            addPlayers(server, databaseProfiles);
            reload(server, "Server's whitelist got reloaded");
        }
        catch (Exception e)
        {
            LOGGER.info(e.getMessage());
            LOGGER.info("Something went wrong while.. doing database things! \n");
        }
    }
    private int reload(MinecraftServer server, String message) {
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

    // isInternal:
    // false - came from the command line, true - came from whatever donation service
    // we are using
    public void addPlayerFromCommandOrDonation(String username, String months, boolean isInternal){
        ResultSet rslt = null;
        Statement stmt = null;
        String query = null;
        String regexp = "[a-zA-Z0-9_]{3,16}";
        String regexp2 = "[a-zA-Z0-9]*_?[a-zA-Z0-9]*";
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss");

        Config.offStatus();
        if(!(Pattern.matches(regexp, username) && Pattern.matches(regexp2, username))){

            if(isInternal){
                LOGGER.info("Someone has no clue about our nickname policy");
                LOGGER.info("Thats his 'name': " + username);
                LOGGER.info("Expect him to reach out soon");
                try{
                    writeLogToFile("Someone has no clue about our nickname policy\n" +
                            "Thats his 'name': " + username + "\n" +
                            "Expect him to reach out soon");
                    LOGGER.info("Have written entry to the log.");
                } catch (IOException e) {
                    LOGGER.info(e.getMessage());
                    LOGGER.info("Have failed to write entry to the log.");
                }
            } else {
                LOGGER.info("I'll remind you of our username policy, 3-16 chars latin + nums and 1 _ (underscore)");
                LOGGER.info("Now try this command once again");
            }
        } else {
            ResultSet userRslt = null;
            PreparedStatement userStmt = null;
            ArrayList<DbPlayerProfile> DbPlayers = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                userStmt = conn.prepareStatement("SELECT username, uuid FROM users WHERE username = (?)");
                userStmt.setString(1, username);
                userRslt = userStmt.executeQuery();
                // it should virtually impossible to get any duplicates of the same user
                // but who knows...?
                while (userRslt.next()) {
                    DbPlayers.add(new DbPlayerProfile(userRslt.getString(1), userRslt.getString(2)));
                }

                if (DbPlayers.size() > 1)
                    try {
                        conn.close();
                    } catch (Exception e) { LOGGER.info(e.getMessage()); }

                WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
                UUID uuid = PlayerEntity.getOfflineUUID("something_went_horribly_wrong");
                GameProfile playerProfile = new GameProfile(uuid, "something_went_horribly_wrong");
                int months_to_add = Integer.parseInt(months);
                // in case we didnt find such user in our database
                if(DbPlayers.size() == 0) {
                    uuid = getUUIDfromUsername(username);
                    playerProfile = new GameProfile(uuid, username);
                } else if(DbPlayers.size() == 1) {
                    uuid = UUID.fromString(DbPlayers.get(0).UUID);
                    playerProfile = new GameProfile(uuid, username);
                }
                if(DbPlayers.size()>1) {

                    if(isInternal){
                        try{
                            LOGGER.info("Found multiple database entries of such username during donation processing");
                            writeLogToFile("Found multiple database entries of such username during donation processing\n" +
                                    "Thats his 'name': " + username + "\n" +
                                    "failed to process the donation");
                            LOGGER.info("Have written entry to the log.");
                        } catch (IOException e) {
                            LOGGER.info(e.getMessage());
                            LOGGER.info("Have failed to write entry to the log.");
                        }
                    } else {
                        LOGGER.info("Now this is getting funny, there is multiple of users with such name");
                        LOGGER.info("Fix your database and get back");
                    }
                } else {
                    if (months_to_add > 0) {
                        try (Connection conn1 = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
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
                                try{
                                    writeLogToFile("Player with the name: " + username + " is already whitelisted. Adding " + months_to_add + " more month(s) to his limit\n" +
                                           "End DateTime from database: " + end_datetime_from_db.toString(format) + "\n" +
                                            "New DateTime: " + new_datetime.toString(format));
                                    LOGGER.info("Have written entry to the log.");
                                } catch (IOException e) {
                                    LOGGER.info(e.getMessage());
                                    LOGGER.info("Have failed to write entry to the log.");
                                }
                            } else {
                                DateTime current_time = new DateTime();
                                DateTime current_time_plus = current_time.plusMonths(months_to_add);
                                LOGGER.info("New player: " + username + " got added for " + months_to_add + " month(s)");
                                LOGGER.info(current_time.toString(format));
                                LOGGER.info(current_time_plus.toString(format));
                                try{
                                    writeLogToFile("New player: " + username + " got added for " + months_to_add + " month(s)\n" +
                                            current_time.toString(format) + "\n" +
                                            current_time_plus.toString(format));
                                    LOGGER.info("Have written entry to the log.");
                                } catch (IOException e) {
                                    LOGGER.info(e.getMessage());
                                    LOGGER.info("Have failed to write entry to the log.");
                                }
                                query = "insert into users (`username`,`start_datetime`,`end_datetime`,`uuid`) values ('" + username + "','" + current_time.toString(format) + "','" + current_time_plus.toString(format) + "','" + uuid + "')";
                            }
                            rslt = stmt.executeQuery(query);
                            if(!isInternal){
                                LOGGER.info("So called 'po blatu', huh :^)");
                            }
                            run(); // sync whitelist with database
                        } catch (Exception e) {
                            LOGGER.info(e.getMessage());
                        }
                    } else {
                        if(months_to_add == 0){
                            LOGGER.info("I mean.. how can i add or remove something with the value of ZERO MONTHS??? FOR FUCKS SAKE MAN");
                        } else {
                            LOGGER.info("Well, lets subtract some months from somebody");
                            try (Connection conn2 = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
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
                                    if(Months.monthsBetween(current_datetime, end_datetime_from_db).getMonths() <= 0){
                                        LOGGER.info("This player has around 1 month of playtime left, if you want to, just delete him, you know the command");
                                        LOGGER.info("Playtime ends at: " + end_datetime_from_db.toString());
                                        LOGGER.info("Time now: " + current_datetime.toString());
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
                }


            } catch (SQLException e){
                LOGGER.info(e.getMessage());
                LOGGER.info("Failed during player addition process");
            }


        }
        Config.onStatus();
    }

    public void deletePlayer(String username){
        WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
        try {
            ResultSet rslt = null;
            PreparedStatement stmt = null;
            ArrayList<DbPlayerProfile> DbPlayers = new ArrayList<>();
            try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                stmt = conn.prepareStatement("SELECT username, uuid FROM users WHERE username = (?)");
                stmt.setString(1, username);
                rslt = stmt.executeQuery();
                // it should virtually impossible to get any duplicates of the same user
                // but who knows...?
                while (rslt.next()) {
                    DbPlayers.add(new DbPlayerProfile(rslt.getString(1), rslt.getString(2)));
                }
                try {
                    conn.close();
                } catch (Exception e) { LOGGER.info(e.getMessage()); }
            } catch (Exception e) {
                LOGGER.info(e.getMessage());
            } finally {
                try {
                    rslt.close();
                } catch (Exception e) { LOGGER.info(e.getMessage()); }
                try {
                    stmt.close();
                } catch (Exception e) { LOGGER.info(e.getMessage()); }
            }
            if(DbPlayers.size()==0){
                LOGGER.info("Players not whitelisted");
            } else if(DbPlayers.size()>1){
                LOGGER.info("Now this is getting funny, there is multiple of users with such name");
                LOGGER.info("Fix your database and get back");
            } else {
                UUID uuid = UUID.fromString(DbPlayers.get(0).UUID);
                GameProfile playerProfile = new GameProfile(uuid, username);
                ResultSet deleteRslt = null;
                Statement deleteStmt = null;

                Config.offStatus();

                if(whitelist.isWhitelisted(playerProfile)){
                    LOGGER.info("Attempting to remove the player");
                    try (Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)) {
                        deleteStmt = conn.createStatement();
                        deleteRslt = stmt.executeQuery("DELETE FROM users where username = '" + username + "'");
                        run();
                    } catch (Exception e) {
                        LOGGER.info(e.getMessage());
                    }
                } else {
                    LOGGER.info("Players not whitelisted");
                }

                Config.onStatus();
            }
        } catch (Exception e)
        {
            LOGGER.info(e.getMessage());
            LOGGER.info("Couldnt connect to the database during player removal per console command\n");
        }
    }


    public void full_restart(){
        Config.onStatus();

        LOGGER.info("Commencing full restart");
        Config.setupDatabaseConfig();
        sock.close();
        StartSyncing();

    }

    public void reload_from_db(){
        run();
        LOGGER.info("(per admins request)");
    }

}
