//package com.ut.wl;
//
//import com.google.gson.Gson;
//import com.mojang.authlib.GameProfile;
//import com.mojang.brigadier.exceptions.CommandSyntaxException;
//import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
//import net.minecraft.server.MinecraftServer;
//import net.minecraft.server.management.WhiteList;
//import net.minecraft.server.management.WhitelistEntry;
//import net.minecraft.util.text.TranslationTextComponent;
//import net.minecraftforge.fml.loading.FMLPaths;
//import net.minecraftforge.fml.server.ServerLifecycleHooks;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.Statement;
//import java.util.*;
//
//import static com.ut.wl.WL.LOGGER;
//
//public class WhiteListRunnable implements Runnable{
//    private static final SimpleCommandExceptionType PLAYER_ALREADY_WHITELISTED = new SimpleCommandExceptionType(new TranslationTextComponent("commands.whitelist.add.failed"));
//    private static final SimpleCommandExceptionType PLAYER_NOT_WHITELISTED = new SimpleCommandExceptionType(new TranslationTextComponent("commands.whitelist.remove.failed"));
//    private final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//    @Override
//    public void run() {
//        try
//        {
//            ResultSet rslt = null;
//            try(Connection conn = DriverManager.getConnection(Config.url, Config.user, Config.password)){
//                Statement stmt = conn.createStatement();
//                rslt = stmt.executeQuery("SELECT username, uuid FROM users");
//            } catch (Exception e){
//                LOGGER.info(e.getMessage());
//            }
//            HashMap<String, String> DbPlayers = new HashMap<>();
//            while(rslt.next()){
//                DbPlayers.put(rslt.getString(2),rslt.getString(1)); // 2 - uuid 1 - name (username)
//            }
//            //assemble databases players gameprofiles
//            ArrayList<GameProfile> databaseProfiles = new ArrayList<>();
//            for(Map.Entry<String, String> entry : DbPlayers.entrySet()){
//                databaseProfiles.add(new GameProfile(UUID.fromString(entry.getKey()),entry.getValue()));
//            }
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
//            removePlayers(server, whitelistProfiles);
//            addPlayers(server, databaseProfiles);
//            reload(server);
//        }
//        catch (Exception e)
//        {
//            LOGGER.info("Something went wrong while.. doing database things! \n" + e.getMessage() + "\n" + e.getLocalizedMessage());
//        }
//    }
//    private static int reload(MinecraftServer server) {
//        server.getCommandSource();
//        server.getPlayerList().reloadWhitelist();
//        LOGGER.info("!!!! SERVERS WHITELIST GOT RELOADED !!!!");
//        server.kickPlayersNotWhitelisted(server.getCommandSource());
//        return 1;
//    }
//    private static int addPlayers(MinecraftServer server, Collection<GameProfile> players) throws CommandSyntaxException {
//        WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
//        int i = 0;
//
//        for(GameProfile gameprofile : players) {
//            if (!whitelist.isWhitelisted(gameprofile)) {
//                WhitelistEntry whitelistentry = new WhitelistEntry(gameprofile);
//                whitelist.addEntry(whitelistentry);
//                ++i;
//            }
//        }
//
//        if (i == 0) {
//            throw PLAYER_ALREADY_WHITELISTED.create();
//        } else {
//            return i;
//        }
//    }
//
//    private static int removePlayers(MinecraftServer server, Collection<GameProfile> players) throws CommandSyntaxException {
//        WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
//        int i = 0;
//
//        for(GameProfile gameprofile : players) {
//            if (whitelist.isWhitelisted(gameprofile)) {
//                WhitelistEntry whitelistentry = new WhitelistEntry(gameprofile);
//                whitelist.removeEntry(whitelistentry);
//                ++i;
//            }
//        }
//
//        if (i == 0) {
//            throw PLAYER_NOT_WHITELISTED.create();
//        } else {
////            server.kickPlayersNotWhitelisted(server.getCommandSource());
//            return i;
//        }
//    }
//}
