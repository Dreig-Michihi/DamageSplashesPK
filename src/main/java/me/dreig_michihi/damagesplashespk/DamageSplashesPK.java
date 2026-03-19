package me.dreig_michihi.damagesplashespk;

import com.github.retrooper.packetevents.PacketEvents;
import me.dreig_michihi.damagesplashespk.commands.ReloadCommand;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
import me.dreig_michihi.damagesplashespk.listeners.DamageListener;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class DamageSplashesPK extends JavaPlugin {
    public static DamageSplashesPK plugin;

    @Override
    public void onEnable() {
        plugin = this;

        // Инициализация PacketEvents - обязательно первый шаг
        // PacketEvents.getAPI().load() - загружает нативные библиотеки и инициализирует менеджеры
        // PacketEvents.getAPI().init() - подключает PacketEvents к серверу и начинает перехватывать пакеты
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().init();

        SplashesConfig.setup();
        SplashesConfig.get().options().copyDefaults(true);
        SplashesConfig.save();

        Objects.requireNonNull(getCommand("dspkreload")).setExecutor(new ReloadCommand());

        // Регистрируем слушатель событий Bukkit (остается без изменений)
        // DamageListener теперь должен использовать PacketEvents вместо ProtocolLib
        Listener damageListener = new DamageListener();
        getServer().getPluginManager().registerEvents(damageListener, this);

        this.getLogger().info("DAMAGE SPLASHES ENABLED with PacketEvents 2.11.2!!!");
    }

    @Override
    public void onDisable() {
        // Корректное выключение PacketEvents - освобождает ресурсы и останавливает перехват пакетов
        PacketEvents.getAPI().terminate();

        // Отписываемся от всех Bukkit событий этого плагина
        HandlerList.unregisterAll(this);

        this.getLogger().info("DAMAGE SPLASHES DISABLED!!!");
    }
}
