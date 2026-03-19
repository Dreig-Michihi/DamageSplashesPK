package me.dreig_michihi.damagesplashespk;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.projectkorra.projectkorra.Element;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
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
    private static final ProtocolManager manager = ProtocolLibrary.getProtocolManager();

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
    protected static double minDamageDelta;
    private static final HashMap<String, ChatColor> elementColors = new HashMap<>();
    private static final HashMap<String, String> elementSymbols = new HashMap<>();

    private int entityID;
    private int combo = 1;
    private double damage;
    private Player player;
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
                        "#" + String.format("%06x", 0xFFFFFF & getElementColor(element).getColor().getRGB()));
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
                            "#" + String.format("%06x", 0xFFFFFF & (subElement.getPlugin() == null ? Color.WHITE : getElementColor(subElement).getColor()).getRGB()));
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
        elementColors.put(null, ChatColor.of((SplashesConfig.get().getString("Visuals.Default.Color", "#" + String.format("%06x", 0xFFFFFF & Color.WHITE.getRGB())))));
        elementSymbols.put(null, SplashesConfig.get().getString("Visuals.Default.Symbols", "♥"));
        for (Element element : Element.getAllElements()) {
            /*try {

            } catch (Exception e) {
                DamageSplashesPK.plugin.getLogger().info(ChatColor.RED + "" + ChatColor.BOLD + "Something got wrong while loading element \"" + element.getName() +
                        "\" from plugin \"" + element.getPlugin() + "\", so WHITE color will be used for this element.");
            }*/
            elementColors.put(element.getName(), ChatColor.of(
                    SplashesConfig.get().getString("Visuals." + element.getName() + "." + element.getName() + ".Color",
                            "#" + String.format("%06x", 0xFFFFFF & getElementColor(element).getColor().getRGB()))));
            elementSymbols.put(element.getName(),
                    SplashesConfig.get().getString("Visuals." + element.getName() + "." + element.getName() + ".Symbol", "♥"));
            for (Element.SubElement subElement : Element.getSubElements(element)) {
                elementColors.put(subElement.getName(), ChatColor.of(
                        SplashesConfig.get().getString("Visuals." + element.getName() + "." + subElement.getName() + ".Color",
                                "#" + String.format("%06x", 0xFFFFFF & (subElement.getPlugin() == null ? Color.WHITE : getElementColor(subElement).getColor()).getRGB()))));
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

    private static ChatColor getElementColor(Element element) {
        if (element.getType()== Element.ElementType.NO_SUFFIX)
            return ChatColor.of(Color.WHITE);
        try{
            Method getColor = Element.class.getDeclaredMethod("getColor");
            ChatColor color;
            if (getColor.getReturnType().isAssignableFrom(org.bukkit.ChatColor.class))
                color = ((org.bukkit.ChatColor) getColor.invoke(element)).asBungee();
            else
                color = element.getColor();
            return color;
        } catch (NoSuchMethodException | InvocationTargetException | SecurityException | IllegalAccessException |
                 IllegalArgumentException e) {
            return ChatColor.of(Color.WHITE);
        }
    }

    private String getDamageString() {
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
        return color + /*(element == null ? "" : ("" + ChatColor.BOLD)) +*/ dmg;
    }

    private void remove() {
        cancelTasks();
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        List<Integer> intList = new ArrayList<>();
        intList.add(entityID);
        try {
            packet.getIntLists().write(0, intList);
            manager.sendServerPacket(player, packet);
        } catch (FieldAccessException e1) {
            try {
                packet.getIntegerArrays().write(0, new int[]{entityID});
                manager.sendServerPacket(player, packet);
            } catch (FieldAccessException e2) {
                teleport(location.add(0, -1000, 0));
                new BukkitRunnable() {
                    int times = 5;
                    @Override
                    public void run() {
                        teleport(location.add(0, -1000, 0));
                        times--;
                        if (times < 0) {
                            cancel();
                        }
                    }
                }.runTaskTimer(DamageSplashesPK.plugin, 0, 0);
            }
        }
    }

    private void teleport(Location destination) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        try {
            packet.getIntegers().write(0, entityID);
            packet.getUUIDs().write(0, player.getUniqueId());
            StructureModifier<Double> doubles = packet.getDoubles();
            doubles.write(0, destination.getX());
            doubles.write(1, destination.getY());
            doubles.write(2, destination.getZ());
            packet.getFloat().write(0, (float) destination.getYaw());
            packet.getFloat().write(1, (float) destination.getPitch());
            location = destination;
            manager.sendServerPacket(player, packet);
        } catch (Exception e) {
            //silent
        }
    }

    private void summon() {
        PacketContainer spawnPacket = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        // Entity ID
        spawnPacket.getIntegers().write(0, this.entityID);
        // Entity Type
        spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);//or EntityType.AREA_EFFECT_CLOUD!!!
        this.location = this.target.getEyeLocation();
        spawnPacket.getDoubles().write(0, this.location.getX());
        spawnPacket.getDoubles().write(1, this.location.getY());
        spawnPacket.getDoubles().write(2, this.location.getZ());
        // Set UUID
        UUID uuid = UUID.randomUUID();
        spawnPacket.getUUIDs().write(0, uuid);

        PacketContainer dataPacket = manager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        dataPacket.getIntegers().write(0, entityID);
        List<WrappedDataValue> dataValues = new ArrayList<>();
        // Сериализатор для типа Byte
        WrappedDataWatcher.Serializer byteSerializer = WrappedDataWatcher.Registry.get(Byte.class);
        WrappedDataWatcher.Serializer boolSerializer = WrappedDataWatcher.Registry.get(Boolean.class);
        WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);

        Optional<?> opt = Optional
                .of(WrappedChatComponent
                        .fromChatMessage(this.getDamageString())[0].getHandle());

        dataValues.add(new WrappedDataValue(0, byteSerializer, (byte) 0x20)); // invisible
        dataValues.add(new WrappedDataValue(2, chatSerializer, opt)); // name
        dataValues.add(new WrappedDataValue(3, boolSerializer, true)); // Custom Name visible
        dataValues.add(new WrappedDataValue(15, byteSerializer, (byte) (0x01 | 0x08 | 0x10))); // isSmall, noBasePlate, set Marker

        dataPacket.getDataValueCollectionModifier().write(0, dataValues);

        /*WrappedDataWatcher metadata = WrappedDataWatcher.getEntityWatcher(null);
        Optional<?> opt = Optional
                .of(WrappedChatComponent
                        .fromChatMessage(this.getDamageString())[0].getHandle());
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(
        0, WrappedDataWatcher.Registry.get(Byte.class)), (byte) 0x20); //invis
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(
        2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), opt);
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(
        3, WrappedDataWatcher.Registry.get(Boolean.class)), true); //custom name visible
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(
        15, WrappedDataWatcher.Registry.get(Byte.class)), (byte) (0x01 | 0x08 | 0x10)); //isSmall, noBasePlate, set Marker
        PacketContainer dataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        dataPacket.getModifier().writeDefaults();
        dataPacket.getIntegers().write(0, this.entityID);
        if (MinecraftVersion.getCurrentVersion().isAtLeast(new MinecraftVersion("1.19.3"))) {
            final List<WrappedDataValue> wrappedDataValueList = new ArrayList<>();
            metadata.getWatchableObjects().stream().filter(Objects::nonNull).forEach(entry -> {
                final WrappedDataWatcher.WrappedDataWatcherObject dataWatcherObject = entry.getWatcherObject();
                wrappedDataValueList.add(new WrappedDataValue(dataWatcherObject.getIndex(), dataWatcherObject.getSerializer(), entry.getRawValue()));
            });
            dataPacket.getDataValueCollectionModifier().write(0, wrappedDataValueList);
        } else {
            dataPacket.getWatchableCollectionModifier().write(0, metadata.getWatchableObjects());
        }*/
        manager.sendServerPacket(this.player, spawnPacket);
        manager.sendServerPacket(this.player, dataPacket);
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
