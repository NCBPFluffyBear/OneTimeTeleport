package io.ncbpfluffybear.onetimeteleport;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Creates a runnable with the executor and target
 */
public class TeleportRequest {

    // Target : Executor
    private static final Map<Player, Player> requests = new HashMap<>();

    /**
     * Creates a teleportation request
     * @param executor Player who sent the teleport request
     * @param target Player who received the teleport request
     * @apiNote Assumes that the executor and target do not already have requests sent/received
     */
    public TeleportRequest(Player executor, Player target) {
        requests.put(target, executor);
        OneTimeTeleport.send(target, executor.getDisplayName() +
                " &ehas sent you a teleport request, use /ott accept or /ott deny."
        );

        this.startCountdown(executor, target);
    }

    private void startCountdown(Player executor, Player target) {
        Bukkit.getScheduler().runTaskLater(OneTimeTeleport.getInstance(), () -> {
            if (requests.getOrDefault(target, null) == executor) {
                OneTimeTeleport.send(executor, "&cYour teleport request to " + target.getDisplayName() + " &ehas expired.");
            }
            requests.remove(target);
        }, 20L * 30); // 20 t/S * S
    }

    /**
     * Gets the executor matching the target
     * @param target The target of the teleport request
     */
    public static Player getExecutor(Player target) {
        return requests.get(target);
    }

    public static void cancelRequest(Player executor) {
        for (Player target : requests.keySet()) {
            if (requests.get(target) == executor) {
                requests.remove(target);
                return;
            }
        }
    }

    public static void deleteRequest(Player target) {
        requests.remove(target);
    }

    public static Collection<Player> getExecutors() {
        return requests.values();
    }

    public static Set<Player> getTargets() {
        return requests.keySet();
    }
}
