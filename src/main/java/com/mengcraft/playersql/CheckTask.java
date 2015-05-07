package com.mengcraft.playersql;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public class CheckTask implements Runnable {

    private static final String MESSAGE_KICK;

    static {
        MESSAGE_KICK = "Your data is locked, login later.";
    }

    private final DataCompond compond = DataCompond.DEFAULT;
    private final SyncManager manager = SyncManager.DEFAULT;
    private final Main main;
    private final Server server;
    private final List<UUID> kick;
    private final Map<UUID, String> map;

    public CheckTask(Main main) {
        this.main = main;
        this.server = main.getServer();
        this.kick = compond.kick();
        this.map = compond.map();
    }

    @Override
    public void run() {
        List<UUID> list = compond.entry();
        for (UUID uuid : list) {
            String data = map.get(uuid);
            if (data != null) {
                Player p = server.getPlayer(uuid);
                manager.load(p, data);
            }
            scheduleTask(uuid);
            map.remove(uuid);
            compond.unlock(uuid);
        }
        synchronized (kick) {
            checkKick();
        }
    }

    private void checkKick() {
        for (UUID uuid : kick) {
            server.getPlayer(uuid).kickPlayer(MESSAGE_KICK);
        }
        kick.clear();
    }

    private void scheduleTask(UUID uuid) {
        Map<UUID, Integer> task = compond.task();
        if (task.get(uuid) != null) {
            server.getScheduler().cancelTask(task.remove(uuid));
        }
        Runnable runnable = new TimerSaveTask(server, uuid);
        server.getScheduler().runTaskTimer(main, runnable, 3600, 3600);
    }

}

class TimerSaveTask implements Runnable {

    private final Server server;
    private final UUID uuid;

    public TimerSaveTask(Server server, UUID uuid) {
        this.server = server;
        this.uuid = uuid;
    }

    @Override
    public void run() {
        Player p = server.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            SyncManager.DEFAULT.save(p, 1);
        } else {
            int id = DataCompond.DEFAULT.task().remove(uuid);
            server.getScheduler().cancelTask(id);
        }
    }

}