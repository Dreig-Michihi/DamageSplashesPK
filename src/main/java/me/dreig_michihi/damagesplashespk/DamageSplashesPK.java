package me.dreig_michihi.damagesplashespk;

import me.dreig_michihi.damagesplashespk.commands.ReloadCommand;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DamageSplashesPK extends JavaPlugin {
    public static DamageSplashesPK plugin;
    @Override
    public void onEnable() {
        plugin = this;

        /*getConfig().options().copyDefaults();
        saveDefaultConfig();*/

        SplashesConfig.setup();
        SplashesConfig.reload();
        SplashesConfig.get().options().copyDefaults(true);
        SplashesConfig.save();
        Objects.requireNonNull(getCommand("dspkreload")).setExecutor(new ReloadCommand());
        Listener damageListener = new DamageListener();
        getServer().getPluginManager().registerEvents(damageListener, this);
        this.getLogger().info("DAMAGE SPLASHES ENABLED!!!");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        this.getLogger().info("DAMAGE SPLASHES DISABLED!!!");
    }


}
