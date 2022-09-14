package me.dreig_michihi.damagesplashespk;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.GeneralMethods;
import me.dreig_michihi.damagesplashespk.config.SplashesConfig;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class DamageSplash {
    private final int entityID;
    private final double damage;
    private final Player player;
    private final LivingEntity source;
    private final Element element;
    private final ProtocolManager manager;
    private final Location origin;

    private Location location;

    public DamageSplash(Player player, double damage, LivingEntity source, Element element) {
        this.player = player;
        this.manager = ProtocolLibrary.getProtocolManager();
        this.entityID = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        this.damage = damage;
        this.source = source;
        this.element = element instanceof Element.SubElement sub ? sub : element;
        this.location = source.getEyeLocation();
        this.origin = location;
        summon();
        Vector splashDirection = GeneralMethods.getDirection(player.getEyeLocation(), origin);
        Vector x = new Vector(splashDirection.getZ(), 0, -splashDirection.getX()).normalize();
        Vector y = splashDirection.clone().crossProduct(x).normalize();
        double angle = ThreadLocalRandom.current().nextDouble(/*2 * */Math.PI);
        double offset = ThreadLocalRandom.current().nextDouble(scatter / 2, scatter + 0.1);
        Location side = location.clone()
                .add(x.clone().multiply(Math.cos(angle)).multiply(offset))
                .add(y.clone().multiply(Math.sin(angle)).multiply(offset));
        Vector random = (side.toVector().subtract(location.toVector())).normalize()/*.add(direction.clone().multiply(3)).normalize()*/;
        double closeness = (minCloseness + 15 / (1.1 * damage + 2));
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                //double distance = player.getEyeLocation().distance(location);
/*                Location destination = player.getEyeLocation()
                        .add((*//*source instanceof Player ? *//*player.getLocation().getDirection()
                 *//*: GeneralMethods.getDirection(player.getEyeLocation(), source.getEyeLocation()).normalize()*//*)
                                .multiply(closeness)) //9*u=4.5
                        .add(random.clone()*//*.multiply(scatter)*//*);*/
                Location destination = player.getEyeLocation()
                        .add((GeneralMethods.getDirection(player.getEyeLocation(), origin).normalize())
                                .multiply(closeness)) //9*u=4.5
                        .add(random);
                if (followCamera) {
                    Vector splashDirection = GeneralMethods.getDirection(player.getEyeLocation(), origin).normalize().multiply(cameraFollowMaxRange);
                    Vector cameraFollow = player.getLocation().getDirection().multiply(cameraFollowMaxRange).subtract(splashDirection);
                    cameraFollow.add(player.getLocation().getDirection().multiply(cameraFollow.length()));
                    if (cameraFollow.length() > cameraFollowMaxRange)
                        cameraFollow = cameraFollow.normalize().multiply(cameraFollowMaxRange);
                    destination.add(cameraFollow);
                }
                teleport(destination);
            }
        };
        task.runTaskTimer(DamageSplashesPK.plugin, 0L, 0L);
        if (disappearAnimation)
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.cancel();
                    teleport(player.getEyeLocation().add(GeneralMethods.getDirection(player.getEyeLocation(), location)
                            .normalize().multiply(10)));
                }
            }.runTaskLater(DamageSplashesPK.plugin, (long) ((splashDuration - 250) * 0.02));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!task.isCancelled())
                    task.cancel();
                remove();
            }
        }.runTaskLater(DamageSplashesPK.plugin, (long) (splashDuration * 0.02));
    }

    private static double damageFactor;
    private static double minCloseness;
    private static double scatter;
    private static boolean followCamera;
    private static double cameraFollowMaxRange;
    private static boolean disappearAnimation;
    private static long splashDuration;
    private static final HashMap<Element, ChatColor> elementColors = new HashMap<>();
    private static final HashMap<Element, String> elementSymbols = new HashMap<>();

    public static void load() {
        SplashesConfig.get().addDefault("Info.ShownNumberFactor", 0.5);
        SplashesConfig.get().addDefault("Info.SplashDuration", 1250L);
        SplashesConfig.get().addDefault("Animations.Appearance.MinCloseness", 1.5);
        SplashesConfig.get().addDefault("Animations.Appearance.Scatter", 1);
        SplashesConfig.get().addDefault("Animations.CameraFollow.Enabled", true);
        SplashesConfig.get().addDefault("Animations.CameraFollow.MaxRange", 1.5);
        SplashesConfig.get().addDefault("Animations.Disappearance.Enabled", true);
        SplashesConfig.get().addDefault("Visuals.Default.Color", ChatColor.WHITE.getChar());
        SplashesConfig.get().addDefault("Visuals.Default.Symbol", "♥");
        for (Element element : Element.getAllElements()) {
            SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + element.getName() + ".Color", element.getColor().getChar());
            SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + element.getName() + ".Symbol", "♥");
            for (Element.SubElement subElement : Element.getSubElements(element)) {
                SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + subElement.getName() + ".Color", subElement.getColor().getChar());
                SplashesConfig.get().addDefault("Visuals." + element.getName() + "." + subElement.getName() + ".Symbol", "♥");
            }
        }
        SplashesConfig.save();
        elementColors.put(null, ChatColor.getByChar(SplashesConfig.get().getString("Visuals.Default.Color", String.valueOf(ChatColor.WHITE.getChar()))));
        elementSymbols.put(null, SplashesConfig.get().getString("Visuals.Default.Symbols", "♥"));
        for (Element element : Element.getAllElements()) {
            elementColors.put(element, ChatColor.getByChar(
                    SplashesConfig.get().getString("Visuals." + element.getName() + "." + element.getName() + ".Color", String.valueOf(element.getColor().getChar()))));
            elementSymbols.put(element,
                    SplashesConfig.get().getString("Visuals." + element.getName() + "." + element.getName() + ".Symbol", "♥"));
            for (Element.SubElement subElement : Element.getSubElements(element)) {
                elementColors.put(subElement, ChatColor.getByChar(
                        SplashesConfig.get().getString("Visuals." + element.getName() + "." + subElement.getName() + ".Color", String.valueOf(subElement.getColor().getChar()))));
                elementSymbols.put(subElement,
                        SplashesConfig.get().getString("Visuals." + element.getName() + "." + subElement.getName() + ".Symbol", "♥"));
            }
        }
        damageFactor = SplashesConfig.get().getDouble("Info.ShownNumberFactor", 0.5);
        splashDuration = SplashesConfig.get().getLong("Info.SplashDuration", 1250);
        minCloseness = SplashesConfig.get().getDouble("Animations.Appearance.MinCloseness", 1.5);
        scatter = SplashesConfig.get().getDouble("Animations.Appearance.Scatter", 1);
        followCamera = SplashesConfig.get().getBoolean("Animations.CameraFollow.Enabled", true);
        cameraFollowMaxRange = SplashesConfig.get().getDouble("Animations.CameraFollow.MaxRange", 1.5);
        disappearAnimation = SplashesConfig.get().getBoolean("Animations.CameraFollow.Enabled", true);
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
        dmg += " ";
        dmg += elementSymbols.get(element);
        //player.sendMessage("dmg: " + dmg);
        return color + /*(element == null ? "" : ("" + ChatColor.BOLD)) +*/ dmg;
    }

    private void remove() {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists().write(0, List.of(entityID));
        try {
            manager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void teleport(Location destination) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, entityID);
        packet.getDoubles()
                .write(0, destination.getX())
                .write(1, destination.getY())
                .write(2, destination.getZ());
        location = destination;
        try {
            manager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void summon() {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        // Entity ID
        packet.getIntegers().write(0, entityID);
        // Entity Type
        packet.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);//or EntityType.AREA_EFFECT_CLOUD!!!
        // Set optional velocity (/8000)
        packet.getIntegers().write(1, 0);
        packet.getIntegers().write(2, 0);
        packet.getIntegers().write(3, 0);
        // Set yaw pitch
        packet.getIntegers().write(4, 0);
        packet.getIntegers().write(5, 0);
        // Set object data
        packet.getIntegers().write(6, 0);
        // Set location
        location = source.getEyeLocation();
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        // Set UUID
        packet.getUUIDs().write(0, UUID.randomUUID());
        WrappedDataWatcher metadata = new WrappedDataWatcher();
        Optional<?> opt = Optional
                .of(WrappedChatComponent
                        .fromChatMessage(getDamageString())[0].getHandle());
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0,
                WrappedDataWatcher.Registry.get(Byte.class)), (byte) 0x20); //invis
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2,
                WrappedDataWatcher.Registry.getChatComponentSerializer(true)), opt);
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3,
                WrappedDataWatcher.Registry.get(Boolean.class)), true); //custom name visible
        metadata.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(15,
                WrappedDataWatcher.Registry.get(Byte.class)), (byte) (0x01 | 0x08 | 0x10)); //isSmall, noBasePlate, set Marker
        PacketContainer dataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        dataPacket.getModifier().writeDefaults();
        dataPacket.getIntegers().write(0, entityID);
        dataPacket.getWatchableCollectionModifier().write(0, metadata.getWatchableObjects());
        try {
            manager.sendServerPacket(player, packet);
            manager.sendServerPacket(player, dataPacket);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        //return packet;
    }
}
