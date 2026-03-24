package me.dreig_michihi.damagesplashespk;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.projectkorra.projectkorra.Element;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DamageSplash {
    private static HashMap<Player, Map<LivingEntity, Map<String, DamageSplash>>> Player_LentElementSplash = new HashMap<>();

    private static double damageFactor;
    private static double minCloseness;
    private static double scatter;
    private static boolean followCamera;
    private static double cameraFollowMaxRange;
    private static boolean disappearAnimation;
    private static boolean closerCombatCloserSplashes;
    private static long splashDuration;
    private static long comboAddsDuration;
    private static long maxDuration;
    private static String comboPrefix = "x";
    public static double minDamageDelta;
    private static final HashMap<String, net.kyori.adventure.text.format.TextColor> elementColors = new HashMap<>();
    private static final HashMap<String, String> elementSymbols = new HashMap<>();

    private int entityID;
    private UUID entityUUID;
    private int combo = 1;
    private double damage;
    private Player player;
    private User user;
    private LivingEntity target;
    private @Nullable String element;
    private Location origin;
    private Vector vector;
    private BukkitRunnable disappear;
    private BukkitRunnable follow;
    private BukkitRunnable removing;
    private double angle;
    private double offset;

    private Location location;

    public DamageSplash(Player player, double damage, LivingEntity target, @Nullable String element) {
        this.player = player;
        this.user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        for (Player spectator : player.getServer().getOnlinePlayers()) {
            if (spectator.getGameMode() != GameMode.SPECTATOR) continue;
            if (!player.equals(spectator.getSpectatorTarget())) continue;
            new DamageSplash(spectator, damage, target, element);
        }
        this.target = target;
        this.element = element;
        this.damage = damage;
        DamageSplash splash = player.hasPermission("dspk.display.sum") ? addSplash(this) : this;
        if (this.equals(splash)) {
            this.entityID = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
            this.entityUUID = UUID.randomUUID();
            this.angle = ThreadLocalRandom.current().nextDouble(/*2 * */Math.PI);
            this.offset = ThreadLocalRandom.current().nextDouble(scatter / 2, scatter + 0.1);
        }
        splash.location = target.getEyeLocation();
        splash.origin = splash.location;
        Vector splashDirection = getDirection(player.getEyeLocation(), target.getEyeLocation());
        Vector x = new Vector(splashDirection.getZ(), 0, -splashDirection.getX()).normalize();
        Vector y = splashDirection.clone().crossProduct(x).normalize();
        Location side = target.getEyeLocation()
                .add(x.clone().multiply(Math.cos(splash.angle)).multiply(splash.offset))
                .add(y.clone().multiply(Math.sin(splash.angle)).multiply(splash.offset));
        splash.vector = (side.toVector().subtract(target.getEyeLocation().toVector())).normalize();
        splash.summon();
    }

    private static DamageSplash addSplash(DamageSplash splash) {
        //String element = splash.element;
        String element = splash.player.hasPermission("dspk.display.joint") ? null : splash.element;
        Map<LivingEntity, Map<String, DamageSplash>> lentSplashes = Player_LentElementSplash.computeIfAbsent(splash.player, k -> new HashMap<>());
        Map<String, DamageSplash> elementSplashes = lentSplashes.computeIfAbsent(splash.target, k -> new HashMap<>());
        DamageSplash damageSplash = elementSplashes.get(element);
        if (damageSplash == null) {
            elementSplashes.put(element, splash);
            return splash;
        } else {
            damageSplash.cancelTasks();
            damageSplash.damage += splash.damage;
            damageSplash.combo++;
            damageSplash.element = splash.element;
            elementSplashes.put(element, damageSplash);
            return damageSplash;
        }
    }

    private void cancelTasks() {
        if (follow != null && !follow.isCancelled()) {
            follow.cancel();
        }
        if (disappear != null && !disappear.isCancelled()) {
            disappear.cancel();
        }
        if (removing != null && !removing.isCancelled()) {
            removing.cancel();
        }
    }

    private static void removeSplash(DamageSplash splash) {
        Map<LivingEntity, Map<String, DamageSplash>> lentSplashes = Player_LentElementSplash.get(splash.player);
        if (lentSplashes == null || lentSplashes.isEmpty()) return;
        Map<String, DamageSplash> elementSplashes = lentSplashes.get(splash.target);
        if (elementSplashes == null || elementSplashes.isEmpty()) return;
        //String element = splash.element;
        String element = splash.player.hasPermission("dspk.display.joint") ? null : splash.element;
        elementSplashes.remove(element, splash);
        splash.cancelTasks();
        if (elementSplashes.isEmpty()) {
            lentSplashes.remove(splash.target, elementSplashes);
            if (lentSplashes.isEmpty()) {
                Player_LentElementSplash.remove(splash.player, lentSplashes);
            }
        }
    }

    private static void startTasks(DamageSplash splash) {
        splash.cancelTasks();
        splash.follow = splash.getBukkitRunnable(splash.player, splash.vector);
        splash.follow.runTaskTimer(DamageSplashesPK.plugin, 0L, 0L);
        if (disappearAnimation) {
            splash.disappear = new BukkitRunnable() {

                private void disappear() {
                    if (!splash.follow.isCancelled()) {
                        splash.follow.cancel();
                    }
                    if (splash.player.getLocation().getWorld().equals(splash.location.getWorld())) {
                        splash.teleport(splash.player.getEyeLocation().add(splash.getDirection(splash.player.getEyeLocation(), splash.location)
                                .normalize().multiply(10)));
                    }
                }

                @Override
                public synchronized void cancel() throws IllegalStateException {
                    super.cancel();
                    disappear();
                }

                @Override
                public void run() {
                    disappear();
                }
            };
            splash.disappear.runTaskLater(DamageSplashesPK.plugin, (long) ((Math.min(maxDuration, splashDuration + ((splash.combo - 1) * comboAddsDuration)) - 250) * 0.02));
        }
        splash.removing = new BukkitRunnable() {

            private void removing() {
                if (!splash.follow.isCancelled()) {
                    splash.follow.cancel();
                }
                splash.remove();
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
            }

            @Override
            public void run() {
                removing();
                removeSplash(splash);
            }
        };
        splash.removing.runTaskLater(DamageSplashesPK.plugin, (long) (Math.min(maxDuration, splashDuration + ((splash.combo - 1) * comboAddsDuration)) * 0.02));
    }

    @NotNull
    private BukkitRunnable getBukkitRunnable(Player player, Vector vector) {
        double closeness = (minCloseness + 15 / (1.1 * (damage/combo) + 2));
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                //double distance = player.getEyeLocation().distance(location);
/*                Location destination = player.getEyeLocation()
                        .add((*//*source instanceof Player ? *//*player.getLocation().getDirection()
                 *//*: GeneralMethods.getDirection(player.getEyeLocation(), source.getEyeLocation()).normalize()*//*)
                                .multiply(closeness)) //9*u=4.5
                        .add(vector.clone()*//*.multiply(scatter)*//*);*/
                if (!player.getLocation().getWorld().equals(origin.getWorld())){
                    this.cancel();
                    return;
                }
                Location destination = player.getEyeLocation()
                        .add((getDirection(player.getEyeLocation(), origin).normalize())
                                .multiply(closeness)
                                .multiply(Math.min(1, closerCombatCloserSplashes ?
                                        Math.max(minCloseness / (minCloseness + 7.5),
                                                player.getEyeLocation().distance(origin) / (minCloseness + 7.5)) : 1))) //9*u=4.5
                        .add(vector);
                if (followCamera) {
                    Vector splashDirection = getDirection(player.getEyeLocation(), origin).normalize().multiply(cameraFollowMaxRange);
                    Vector cameraFollow = player.getLocation().getDirection().multiply(cameraFollowMaxRange).subtract(splashDirection);
                    cameraFollow.add(player.getLocation().getDirection().multiply(cameraFollow.length()));
                    if (cameraFollow.length() > cameraFollowMaxRange)
                        cameraFollow = cameraFollow.normalize().multiply(cameraFollowMaxRange);
                    destination.add(cameraFollow);
                }
                teleport(destination);
            }
        };
        return task;
    }

    @SuppressWarnings("deprecation")
    private static TextColor chatColorToTextColor(ChatColor chatColor) {
        return chatColor == null ? NamedTextColor.WHITE :
                TextColor.color(chatColor.getColor().getRGB());
    }

    public static void load() {
        SplashesConfig.get().addDefault("Info.ShownNumberFactor", 0.5);
        SplashesConfig.get().addDefault("Info.SplashDuration", 1500L);
        SplashesConfig.get().addDefault("Info.ComboAddsDuration", 250L);
        SplashesConfig.get().addDefault("Info.MaxDuration", 3000L);
        SplashesConfig.get().addDefault("Info.MinDamageDelta", 0.000001);
        SplashesConfig.get().addDefault("Animations.Appearance.MinCloseness", 1.5);
        SplashesConfig.get().addDefault("Animations.Appearance.CloserCombatCloserSplashes", true);
        SplashesConfig.get().addDefault("Animations.Appearance.Scatter", 1);
        SplashesConfig.get().addDefault("Animations.CameraFollow.Enabled", true);
        SplashesConfig.get().addDefault("Animations.CameraFollow.MaxRange", 1.5);
        SplashesConfig.get().addDefault("Animations.Disappearance.Enabled", true);
        SplashesConfig.get().addDefault("Visuals.Default.Color",
                "#" + String.format("%06x", 0xFFFFFF & Color.WHITE.getRGB()));
        SplashesConfig.get().addDefault("Visuals.Default.ComboPrefix", "x");
        SplashesConfig.get().addDefault("Visuals.Default.Symbol", "♥");
        for (Element element : Element.getAllElements()) {
            try {
                SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + element.getName() + ".Color",
                        "#" + String.format("%06x", 0xFFFFFF & getElementColorText(element).value()));
            } catch (Exception e) {
                DamageSplashesPK.plugin.getLogger().info(ChatColor.RED + "" + ChatColor.BOLD + "Something got wrong while loading element \"" + element.getName() +
                        "\" from plugin \"" + element.getPlugin() + "\", so WHITE color will be used for this element.");
                SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + element.getName() + ".Color",
                        "#" + String.format("%06x", 0xFFFFFF & Color.WHITE.getRGB()));
            }
            SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + element.getName() + ".Symbol", "♥");
            for (Element.SubElement subElement : Element.getSubElements(element)) {
                try {
                    SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + subElement.getName() + ".Color",
                            "#" + String.format("%06x", 0xFFFFFF & (subElement.getPlugin() == null ? NamedTextColor.WHITE : getElementColorText(subElement)).value()));
                } catch (Exception e) {
                    DamageSplashesPK.plugin.getLogger().info(ChatColor.RED + "" + ChatColor.BOLD + "Something got wrong while loading element \"" + subElement.getName() +
                            "\" from plugin \"" + subElement.getPlugin() + "\", so WHITE color will be used for this element.");
                    SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + subElement.getName() + ".Color",
                            "#" + String.format("%06x", 0xFFFFFF & Color.WHITE.getRGB()));
                }
                SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + subElement.getName() + ".Symbol", "♥");
            }
        }
        SplashesConfig.save();
        elementColors.put(null, TextColor.fromHexString((SplashesConfig.get().getString("Visuals.Default.Color", "#" + String.format("%06x", 0xFFFFFF & Color.WHITE.getRGB())))));
        elementSymbols.put(null, SplashesConfig.get().getString("Visuals.Default.Symbols", "♥"));
        for (Element element : Element.getAllElements()) {
            /*try {

            } catch (Exception e) {
                DamageSplashesPK.plugin.getLogger().info(ChatColor.RED + "" + ChatColor.BOLD + "Something got wrong while loading element \"" + element.getName() +
                        "\" from plugin \"" + element.getPlugin() + "\", so WHITE color will be used for this element.");
            }*/
            elementColors.put(element.getName(), TextColor.fromHexString(
                    SplashesConfig.get().getString("Visuals." + element.getName() + "." + element.getName() + ".Color",
                            "#" + String.format("%06x", 0xFFFFFF & getElementColorText(element).value()))));
            elementSymbols.put(element.getName(),
                    SplashesConfig.get().getString("Visuals." + element.getName() + "." + element.getName() + ".Symbol", "♥"));
            for (Element.SubElement subElement : Element.getSubElements(element)) {
                elementColors.put(subElement.getName(), TextColor.fromHexString(
                        SplashesConfig.get().getString("Visuals." + element.getName() + "." + subElement.getName() + ".Color",
                                "#" + String.format("%06x", 0xFFFFFF & (subElement.getPlugin() == null ? NamedTextColor.WHITE : getElementColorText(subElement)).value()))));
                elementSymbols.put(subElement.getName(),
                        SplashesConfig.get().getString("Visuals." + element.getName() + "." + subElement.getName() + ".Symbol", "♥"));
            }
        }
        damageFactor = SplashesConfig.get().getDouble("Info.ShownNumberFactor", 0.5);
        splashDuration = SplashesConfig.get().getLong("Info.SplashDuration", 1250);
        comboAddsDuration = SplashesConfig.get().getLong("Info.ComboAddsDuration",  250);
        maxDuration = SplashesConfig.get().getLong("Info.MaxDuration", 3000);
        minDamageDelta = SplashesConfig.get().getDouble("Info.MinDamageDelta", 0.000001);
        comboPrefix = SplashesConfig.get().getString("Visuals.Default.ComboPrefix", "x");
        minCloseness = SplashesConfig.get().getDouble("Animations.Appearance.MinCloseness", 1.5);
        closerCombatCloserSplashes = SplashesConfig.get().getBoolean("Animations.Appearance.CloserCombatCloserSplashes", true);
        scatter = SplashesConfig.get().getDouble("Animations.Appearance.Scatter", 1);
        followCamera = SplashesConfig.get().getBoolean("Animations.CameraFollow.Enabled", true);
        cameraFollowMaxRange = SplashesConfig.get().getDouble("Animations.CameraFollow.MaxRange", 1.5);
        disappearAnimation = SplashesConfig.get().getBoolean("Animations.CameraFollow.Enabled", true);
    }

    @SuppressWarnings("deprecation")
    private static TextColor getElementColorText(Element element) {
        /*if (element.getType() == Element.ElementType.NO_SUFFIX) {
            return NamedTextColor.WHITE;
        }*/

        try {
            ChatColor elementColor = element.getColor();
            return TextColor.color(elementColor.getColor().getRGB());  // 🎯 Единственная строка!
        } catch (Exception e) {
            DamageSplashesPK.plugin.getLogger().warning("Failed to get color for " + element.getName());
            return NamedTextColor.WHITE;
        }
    }

    private Component getDamageComponent() {
        TextColor color = elementColors.getOrDefault(element, NamedTextColor.WHITE);

        String dmg = String.format("%.2f", -damage * damageFactor).replace(',', '.');

        int zeroIndex = dmg.indexOf('0', dmg.indexOf('.'));
        if (zeroIndex > 0) {
            dmg = dmg.substring(0, zeroIndex);
        }
        if (dmg.endsWith(".")) {
            dmg = dmg.substring(0, dmg.length() - 1);
        }
        dmg += " " + elementSymbols.get(element);
        if (combo > 1) {
            dmg += " " + comboPrefix + combo;
        }
        TextComponent result = Component.text(dmg).color(color);
        if (damage >= 20 && damage < 26) {
            result = result.decorate(TextDecoration.OBFUSCATED);
        } else if (damage >= 18) {
            result = result.decorate(TextDecoration.BOLD).decorate(TextDecoration.ITALIC);
        } else if (damage >= 12) {
            result = result.decorate(TextDecoration.BOLD);
        } else if (damage >= 6) {
            result = result.decorate(TextDecoration.ITALIC);
        }
        return result;
    }

    /*private String getDamageString() {
        ChatColor color = elementColors.get(element);
        //player.sendMessage("damage: " + damage);
        //player.sendMessage("-damage/2: " + -damage/2);
        //player.sendMessage("%.2f: " + String.format("%.2f", -damage / 2));
        String dmg = (String.format("%.2f", -damage * damageFactor)).replace(',', '.');
        //player.sendMessage("dmg: " + dmg);
        int zeroIndex = dmg.indexOf('0', dmg.indexOf('.'));
        //player.sendMessage("zeroIndex: " + zeroIndex);
        if (zeroIndex > 0) {
            dmg = dmg.substring(0, zeroIndex);
            //player.sendMessage("dmg.substring(0, zeroIndex): " + dmg.substring(0, zeroIndex));
        }
        if (dmg.endsWith(".")) {
            dmg = dmg.substring(0, dmg.length() - 1);
            //player.sendMessage("dmg.substring(0, dmg.length() - 1): " + dmg.substring(0, dmg.length() - 1));
        }
        dmg += " " + elementSymbols.get(element);
        if (combo > 1) {
            dmg += " " + comboPrefix + combo;
        }
        //player.sendMessage("dmg: " + dmg);
        return color + *//*(element == null ? "" : ("" + ChatColor.BOLD)) +*//* dmg;
    }*/

    private void remove() {
        cancelTasks();
        WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityID);
        user.sendPacket(destroyPacket);
    }

    private static com.github.retrooper.packetevents.protocol.world.Location convert(Location location) {
        return SpigotConversionUtil.fromBukkitLocation(location);
    }

    private void teleport(Location destination) {
        WrapperPlayServerEntityTeleport teleportPacket = new WrapperPlayServerEntityTeleport(
                entityID,
                convert(destination),
                false);
        user.sendPacket(teleportPacket);
    }

    private static com.github.retrooper.packetevents.protocol.entity.type.EntityType convert(EntityType type) {
        return SpigotConversionUtil.fromBukkitEntityType(type);
    }

    private void summon() {
        this.location = this.target.getEyeLocation();

        // 1️⃣ SPAWN_ENTITY (ArmorStand)
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityID,                               // int entityID
                entityUUID,                             // @Nullable UUID uuid
                convert(EntityType.ARMOR_STAND),        // EntityType entityType
                convert(location),                      // Location location
                0.0f,                                   // float headYaw
                0,                                      // int data
                null                                    // @Nullable Vector3d velocity
        );

        List<EntityData<?>> metadata = new ArrayList<>();

        // Индекс 0: Невидимый (byte flags | 0x20)
        metadata.add(new EntityData<>(0, EntityDataTypes.BYTE, (byte) 0x20));

        // Индекс 2: CustomName (Component)
        //String plainText = TextComponent.stripColor(getDamageString());
        Component damageText = getDamageComponent();
        metadata.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, Optional.of(damageText)));

        // Индекс 3: CustomNameVisible (BOOLEAN)
        metadata.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, true));

        // Индекс 15: ArmorStand flags (byte)
        metadata.add(new EntityData<>(15, EntityDataTypes.BYTE, (byte) (0x01 | 0x08 | 0x10)));

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
                entityID, metadata);
        user.sendPacket(spawnPacket);
        user.sendPacket(metadataPacket);

        startTasks(this);
    }

    private Vector getDirection(Location origin, Location destination){
        double x0 = origin.getX();
        double y0 = origin.getY();
        double z0 = origin.getZ();
        double x1 = destination.getX();
        double y1 = destination.getY();
        double z1 = destination.getZ();
        return new Vector(x1 - x0, y1 - y0, z1 - z0);
    }
}
