package me.dreig_michihi.damagesplashespk.config;

import me.dreig_michihi.damagesplashespk.DamageSplash;
import me.dreig_michihi.damagesplashespk.DamageSplashesPK;
import me.dreig_michihi.damagesplashespk.config.comments.ConfigComments;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class SplashesConfig {

    private static File file;
    private static FileConfiguration customFile;

    //find or generate file
    public static void setup() {
        file = new File(Objects.requireNonNull(DamageSplashesPK.plugin).getDataFolder(), "config.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                DamageSplashesPK.plugin.getLogger().info("Couldn't create config file.");
            }
        }
        customFile = YamlConfiguration.loadConfiguration(file);
        reload();
    }

    public static FileConfiguration get() {
        return customFile;
    }

    public static File getFile() {
        return file;
    }

    public static void save() {
        try {
            SplashesConfig.get().options().copyDefaults(true);
            customFile.save(file);
        } catch (IOException e) {
            //oww
            DamageSplashesPK.plugin.getLogger().info("Couldn't save config file.");
        }
    }

    private static String permissionMessage;
    private static String reloadMessage;


    public static void loadLang(){
        SplashesConfig.get().addDefault("Language.PermissionMessage", "You have no permission to do it.");
        SplashesConfig.get().addDefault("Language.ReloadMessage", "DamageSplashesPK reloaded.");
        SplashesConfig.get().addDefault("Language.ToggleOnMessage", "Now you can see damage splashes.");
        SplashesConfig.get().addDefault("Language.ToggleOffMessage", "You can't see damage splashes now. Use \"/dspk toggle\" again to toggle it back.");
        permissionMessage = SplashesConfig.get().getString("Language.PermissionMessage", "You have no permission to do it.");
        reloadMessage = SplashesConfig.get().getString("Language.ReloadMessage", "DamageSplashesPK reloaded.");
    }

    public static void reload() {
        customFile = YamlConfiguration.loadConfiguration(file);
        SplashesConfig.loadLang();
        DamageSplash.load();
        ConfigComments.addDefaultComment("Language", """
                Translate plugin messages here.""", true);
        ConfigComments.addDefaultComment("Visuals", """
                
                # In the Color field use minecraft color code char. Color Codes: htmlcolorcodes.com/minecraft-color-codes
                # In the Symbol field use any UTF-8 symbol you like and minecraft supports.""", false);
        ConfigComments.addDefaultComment("Info.SplashDuration", """
                How long splash exists""", true);
        ConfigComments.addDefaultComment("Info.ShownNumberFactor", """
                When applying 20 pts. damage, splash will show the number 20*ShownNumberFactor.""", true);
        ConfigComments.addDefaultComment("Animations.Appearance.MinCloseness", """
                
                # This parameter determines how close damage splashes can approach you.
                # The higher the damage dealt, the closer the splash of damage gets to you.""", false);
        ConfigComments.addDefaultComment("Animations.Appearance.Scatter", """
                Adjust how far damage splashes can spread""", true);
        ConfigComments.addDefaultComment("Animations.CameraFollow", """
                
                # When you move your camera sideways, the damage spatter shifts slightly,
                # trying to stay in the frame longer""", false);
        ConfigComments.addDefaultComment("Animations.CameraFollow.MaxRange", """
                How far can the damage splash move while following the camera""", true);
        ConfigComments.addDefaultComment("Animations.Disappearance", """
                Damage splashes will visually decrease before disappearing""", true);
    }

    public static String getPermissionMessage() {
        return permissionMessage;
    }

    public static String getReloadMessage() {
        return reloadMessage;
    }
}
