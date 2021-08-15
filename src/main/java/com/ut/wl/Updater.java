package com.ut.wl;

public final class Updater {
    public static WhiteListUpdater updater;
    public synchronized static WhiteListUpdater getInstance(){
        if(updater == null) {
            updater = new WhiteListUpdater();
        }
        return updater;
    }
}
