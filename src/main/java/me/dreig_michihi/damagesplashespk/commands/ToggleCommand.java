package me.dreig_michihi.damagesplashespk.commands;

import me.dreig_michihi.damagesplashespk.DamageSplashesPK;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

public class ToggleCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("dspk.command.toggle")) {
                player.sendMessage(ChatColor.RED + SplashesConfig.getPermissionMessage());
                return true;
            }
            HashMap<UUID, PermissionAttachment> perms = new HashMap<>();
            PermissionAttachment attachment = player.addAttachment(DamageSplashesPK.plugin);
            perms.put(player.getUniqueId(), attachment);
            PermissionAttachment pperms = perms.get(player.getUniqueId());
            if (!player.hasPermission("dspk.cansee")) {
                pperms.setPermission("dspk.cansee", true);
                player.sendMessage(ChatColor.YELLOW + SplashesConfig.getCanseeMessage());
            } else {
                perms.get(player.getUniqueId()).unsetPermission("dspk.cansee");
                player.sendMessage(ChatColor.YELLOW + SplashesConfig.getCantseeMessage());
            }
            return true;
        }
        return false;
    }
}
