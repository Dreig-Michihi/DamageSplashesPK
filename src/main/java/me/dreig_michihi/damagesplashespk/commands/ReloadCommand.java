package me.dreig_michihi.damagesplashespk.commands;

import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player && !player.hasPermission("dspk.command.reload")) {
            player.sendMessage(ChatColor.RED + SplashesConfig.getPermissionMessage());
            return true;
        }
        SplashesConfig.reload();
        sender.sendMessage(ChatColor.AQUA + SplashesConfig.getReloadMessage());
        return true;
    }

}
