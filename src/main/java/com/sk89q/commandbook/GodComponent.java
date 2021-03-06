package com.sk89q.commandbook;

import com.sk89q.commandbook.components.AbstractComponent;
import com.sk89q.commandbook.components.ComponentInformation;
import com.sk89q.commandbook.config.ConfigurationBase;
import com.sk89q.commandbook.config.Setting;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashSet;
import java.util.Set;

@ComponentInformation(friendlyName = "God", desc = "God mode support")
public class GodComponent extends AbstractComponent implements Listener {
    /**
     * List of people with god mode.
     */
    private final Set<String> hasGodMode = new HashSet<String>();
    
    private LocalConfiguration config;
    
    @Override
    public void initialize() {
        config = configure(new LocalConfiguration());
        registerCommands(Commands.class);
        // Check god mode for existing players, if any
        for (Player player : CommandBook.server().getOnlinePlayers()) {
            if (config.autoEnable && (CommandBook.inst().getPermissionsResolver()
                    .inGroup(player, "cb-invincible")
                    || CommandBook.inst().hasPermission(player, "commandbook.god.auto-invincible"))) {
                enableGodMode(player);
            }
        }
        CommandBook.registerEvents(this);
    }
    
    @Override
    public void reload() {
        super.reload();
        config = configure(config);
        // Check god mode for existing players, if any
        for (Player player : CommandBook.server().getOnlinePlayers()) {
            if (config.autoEnable && (CommandBook.inst().getPermissionsResolver()
                    .inGroup(player, "cb-invincible")
                    || CommandBook.inst().hasPermission(player, "commandbook.god.auto-invincible"))) {
                enableGodMode(player);
            }
        }
    }
    
    private static class LocalConfiguration extends ConfigurationBase {
        @Setting("auto-enable") public boolean autoEnable = true;
    }

    /**
     * Enable god mode for a player.
     *
     * @param player
     */
    public void enableGodMode(Player player) {
        hasGodMode.add(player.getName());
    }

    /**
     * Disable god mode for a player.
     *
     * @param player
     */
    public void disableGodMode(Player player) {
        hasGodMode.remove(player.getName());
    }

    /**
     * Check to see if god mode is enabled for a player.
     *
     * @param player
     * @return
     */
    public boolean hasGodMode(Player player) {
        return hasGodMode.contains(player.getName());
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (config.autoEnable && (CommandBook.inst().getPermissionsResolver()
                .inGroup(player, "cb-invincible")
                || CommandBook.inst().hasPermission(player, "commandbook.god.auto-invincible"))) {
            enableGodMode(player);
        }

    }

    /**
     * Called on entity combust.
     */
    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (hasGodMode(player)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
        }
    }
    
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            if (hasGodMode(player)) {
                event.setCancelled(true);
                player.setFireTicks(0);
                return;
            }
        }
    }
    
    public class Commands {
        @Command(aliases = {"god"}, usage = "[player]",
                desc = "Enable godmode on a player", flags = "s", max = 1)
        public void god(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets = null;
            boolean included = false;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            }

            // Check permissions!
            for (Player player : targets) {
                if (player == sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.god");
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.god.other");
                    break;
                }
            }

            for (Player player : targets) {
                enableGodMode(player);
                player.setFireTicks(0);

                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "God mode enabled! Use /ungod to disable.");

                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "God enabled by "
                            + PlayerUtil.toName(sender) + ".");

                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players now have god mode.");
            }
        }

        @Command(aliases = {"ungod"}, usage = "[player]",
                desc = "Disable godmode on a player", flags = "s", max = 1)
        public void ungod(CommandContext args, CommandSender sender) throws CommandException {

            Iterable<Player> targets = null;
            boolean included = false;

            // Detect arguments based on the number of arguments provided
            if (args.argsLength() == 0) {
                targets = PlayerUtil.matchPlayers(PlayerUtil.checkPlayer(sender));
            } else if (args.argsLength() == 1) {
                targets = PlayerUtil.matchPlayers(sender, args.getString(0));
            }

            // Check permissions!
            for (Player player : targets) {
                if (player == sender) {
                    CommandBook.inst().checkPermission(sender, "commandbook.god");
                } else {
                    CommandBook.inst().checkPermission(sender, "commandbook.god.other");
                    break;
                }
            }

            for (Player player : targets) {
                disableGodMode(player);

                // Tell the user
                if (player.equals(sender)) {
                    player.sendMessage(ChatColor.YELLOW + "God mode disabled!");

                    // Keep track of this
                    included = true;
                } else {
                    player.sendMessage(ChatColor.YELLOW + "God disabled by "
                            + PlayerUtil.toName(sender) + ".");

                }
            }

            // The player didn't receive any items, then we need to send the
            // user a message so s/he know that something is indeed working
            if (!included && args.hasFlag('s')) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "Players no longer have god mode.");
            }
        }
    }
}
