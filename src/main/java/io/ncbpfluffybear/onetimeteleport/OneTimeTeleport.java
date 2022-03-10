package io.ncbpfluffybear.onetimeteleport;

import com.programmerdan.minecraft.simpleadminhacks.SimpleAdminHacks;
import com.programmerdan.minecraft.simpleadminhacks.configs.IntrobookConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("ConstantConditions, ResultOfMethodCallIgnored")
public class OneTimeTeleport extends JavaPlugin implements Listener {

    private static OneTimeTeleport instance;
    private final List<OfflinePlayer> teleportTracker = new ArrayList<>();
    private final List<OfflinePlayer> cheatTracker = new ArrayList<>();

    FileConfiguration teleportsConfig;
    File teleportsFile;

    Date date = new Date();

    @Override
    public void onEnable() {

        instance = this;

        getInstance().getDataFolder().mkdir();

        teleportsFile = new File(getInstance().getDataFolder(), "teleports.yml");
        if (!teleportsFile.exists()) {
            try {
                Files.copy(this.getClass().getResourceAsStream("/teleports.yml"), teleportsFile.toPath());
            } catch (IOException e) {
                getInstance().getLogger().log(Level.SEVERE, "Failed to copy teleports.yml, stopping plugin", e);
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }

        teleportsConfig = YamlConfiguration.loadConfiguration(teleportsFile);
        // Load teleport logs
        loadTeleports(teleportsConfig);

    }

    @Override
    public void onDisable() {
        List<String> uuidStrings = new ArrayList<>();

        for (OfflinePlayer p : teleportTracker) {
            uuidStrings.add(p.getUniqueId().toString());
        }

        teleportsConfig.set("players", uuidStrings);
        try {
            teleportsConfig.save(teleportsFile);
        } catch (IOException e) {
            getInstance().getLogger().log(Level.SEVERE, "There was an issue saving teleports! Please save these UUIDs to restore data!", e);

            for (String player : uuidStrings) {
                getInstance().getLogger().log(Level.WARNING, player);
            }
        }
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent joinEvent) {
        Player player = joinEvent.getPlayer();

        if (!player.hasPlayedBefore()) {
            send(player, "Welcome! You can use a one time teleport within the first 48 hours of joining the server. " +
                    "Do /ott [playername] to request to teleport. Warning! Your inventory will be cleared."
            );
        }
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command command, @Nonnull String label, String[] args) {

        if (sender instanceof Player) { // Player commands
            Player exec = (Player) sender;

            if (args.length < 1) {
                send(exec, "&c/ott <player|accept|deny|cancel>");
                return true;
            }

            switch (args[0].toUpperCase()) {
                case "ACCEPT":

                    // Check if exec is a target
                    if (TeleportRequest.getTargets().contains(exec)) {

                        Player requester = TeleportRequest.getExecutor(exec);
                        send(requester, "&a" + exec.getDisplayName() + " &ahas accepted your teleport request.");
                        send(requester, "&aTeleporting you to " + exec.getDisplayName() + "&a...");

                        send(exec, "&aTeleporting " + requester.getDisplayName() + " &ato you...");

                        requester.teleport(exec);
                        TeleportRequest.deleteRequest(exec);

                        requester.getInventory().clear();
                        requester.getInventory().addItem(new ItemStack(Material.COOKIE, 32));
                        requester.getInventory().addItem(((IntrobookConfig) SimpleAdminHacks.instance()
                                .getHackManager().getHack("Introbook").config()).getIntroBook(requester)
                        );
                        send(requester, "&aYou have been given an Introbook!");

                        teleportTracker.add(requester);

                        System.out.println(exec.getFirstPlayed());

                    } else {
                        send(exec, "&cYou have no teleport requests!");
                    }

                    return true;

                case "DENY":
                    // Check if exec is a target
                    if (TeleportRequest.getTargets().contains(exec)) {

                        Player requester = TeleportRequest.getExecutor(exec);
                        send(requester, "&c" + exec.getDisplayName() + " &chas denied your teleport request.");
                        send(exec, "&cYou have denied " + requester.getDisplayName() + "&c's teleport request.");
                        TeleportRequest.deleteRequest(exec);

                    } else {
                        send(exec, "&cYou have no teleport requests!");
                    }

                    return true;

                case "CANCEL":
                    if (TeleportRequest.getExecutors().contains(exec)) {
                        send(exec, "&cYou have cancelled your teleport request.");
                        TeleportRequest.cancelRequest(exec);
                    } else {
                        send(exec, "&cYou did not send a teleport request!");
                    }

                    return true;

                default: // No arg[0] to specify player, assume requesting to teleport
                    if (args.length < 1) {
                        send(exec, "&c/ott <player>");
                        return true;
                    }

                    if (!cheatTracker.contains(exec)) { // Cheater lol

                        if (date.getTime() >= exec.getFirstPlayed() + 172800000L) { // Add 48 Hrs to first played time
                            send(exec, "&cYou can only use this command within 48 hours of joining the server!");
                            return true;
                        }

                        if (teleportTracker.contains(exec)) {
                            send(exec, "&cYou have already used your teleport!");
                            return true;
                        }

                    }

                    if (cheatTracker.contains(exec)) {
                        send(exec, "&ePro Tip: &cYou are a cheater!!!");
                    }

                    Player target = Bukkit.getPlayer(args[0]);

                    if (target == null || target == exec) {
                        send(exec, "&cThat is not a valid player!");
                        return true;
                    }

                    if (TeleportRequest.getExecutors().contains(exec)) {
                        send(exec, "&cYou have already sent a request. Wait for it to expire or use /ott cancel.");
                        return true;
                    }

                    if (TeleportRequest.getTargets().contains(target)) {
                        send(exec, "&cThis player already has a request.");
                        return true;
                    }

                    send(exec, "&eYou have sent a teleport request to " + target.getDisplayName());
                    new TeleportRequest(exec, target);

                    return true;
            }


        } else { // Console commands
            if (args.length < 2) {
                send(sender, "&cUse /ott stats <player> to check their ott status");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);

            if (target == null) {
                send(sender, "&cThat is not a valid online player!");
                return true;
            }

            switch (args[0].toUpperCase()) {
                case "STATS":
                    send(sender, "&a" + target.getDisplayName() + "&e has used ott: " + teleportTracker.contains(target));
                    return true;
                case "CHEAT":
                    if (cheatTracker.contains(target)) {
                        cheatTracker.remove(target);
                        send(sender, "&a" + target.getDisplayName() + "&ehas uninstalled hacks");

                    } else {
                        cheatTracker.add(target);
                        send(sender, "&a" + target.getDisplayName() + "&ehas become a dirty cheater and it's your fault");
                    }
                    return true;

            }
        }

        return true;
    }

    private void loadTeleports(FileConfiguration teleportsConfig) {
        for (String uuidStr : teleportsConfig.getStringList("players")) {
            teleportTracker.add(Bukkit.getServer().getOfflinePlayer(UUID.fromString(uuidStr)));
        }
    }

    public static void send(CommandSender sender, String msg) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&6OneTimeTeleport&7]&f " + msg));
    }

    public static OneTimeTeleport getInstance() {
        return instance;
    }

}
