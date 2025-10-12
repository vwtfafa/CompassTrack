package org.vwtfafa.compassTrack;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class CompassTrack extends JavaPlugin {
    // Cooldown in Sekunden
    private static final int COOLDOWN_SECONDS = 30;
    // Map: Spielername -> letzter Tracking-Zeitpunkt (Millis)
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    // Map: Tracker -> Zielspieler
    private final Map<UUID, UUID> tracking = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("CompassTrack aktiviert!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        tracking.clear();
        cooldowns.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("compassTrack.use")) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl!");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage("§eVerwendung: /track <Spieler|off>");
            return true;
        }
        if (args[0].equalsIgnoreCase("off")) {
            if (tracking.remove(player.getUniqueId()) != null) {
                player.sendMessage("§aKompass-Tracking deaktiviert.");
            } else {
                player.sendMessage("§eDu verfolgst aktuell niemanden.");
            }
            return true;
        }
        // Cooldown prüfen
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_SECONDS * 1000) {
            long wait = (COOLDOWN_SECONDS * 1000 - (now - last)) / 1000;
            player.sendMessage("§cBitte warte noch " + wait + " Sekunden, bevor du erneut trackst.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline() || target == player) {
            player.sendMessage("§cSpieler nicht gefunden oder ungültig.");
            return true;
        }
        // Kompass im Inventar?
        if (!hasCompass(player)) {
            player.sendMessage("§cDu benötigst einen Kompass im Inventar!");
            return true;
        }
        // Tracking setzen
        tracking.put(player.getUniqueId(), target.getUniqueId());
        cooldowns.put(player.getUniqueId(), now);
        player.sendMessage("§aDu verfolgst jetzt: §e" + target.getName());
        updateCompass(player, target.getLocation());
        return true;
    }

    // Hilfsmethode: Hat Spieler einen Kompass?
    private boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS) {
                return true;
            }
        }
        return false;
    }

    // Kompassziel setzen
    private void updateCompass(Player player, Location loc) {
        player.setCompassTarget(loc);
    }

    // Event-Listener für Bewegung, damit Kompass aktuell bleibt
    @Override
    public void onLoad() {
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
                Player tracker = event.getPlayer();
                UUID targetId = tracking.get(tracker.getUniqueId());
                if (targetId != null) {
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null && target.isOnline()) {
                        updateCompass(tracker, target.getLocation());
                    }
                }
            }
        }, this);
    }
}
