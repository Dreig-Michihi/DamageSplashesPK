package me.dreig_michihi.damagesplashespk;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import com.projectkorra.projectkorra.util.MovementHandler;
import org.bukkit.GameMode;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class DamageListener implements Listener {
    private static final Set<Player> abilityDamagePlayersList = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void DamageEvent(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)
                || target.isInvulnerable()
                || event.getFinalDamage() <= 0
                || (target instanceof Player player
                && !(player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)))
            return;
        Player player;
        if (event.getDamager() instanceof Player damager)
            player = damager;
        else if (event.getDamager() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player damager)
            player = damager;
        else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player damager)
            player = damager;
        else
            return;
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null || bPlayer.isChiBlocked() || bPlayer.isBloodbent() || MovementHandler.isStopped(player) || !player.hasPermission("dspk.cansee"))
            return;
        if (!abilityDamagePlayersList.contains(player)) {
            final double health = target.getHealth();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!event.isCancelled() && health > target.getHealth()) {
                        new DamageSplash(player, event.getDamage(), target, null);
                    }
                }
            }.runTaskLater(DamageSplashesPK.plugin, 1);
        }
        if (!abilityDamagePlayersList.isEmpty())
            abilityDamagePlayersList.remove(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void AbilityDamageEvent(final AbilityDamageEntityEvent event) {
        Player player;
        Ability ability = event.getAbility();
        if (event.getSource() != null) {
            player = event.getSource();
        } else if (ability != null && ability.getPlayer() != null) {
            player = ability.getPlayer();
        } else {
            return;
        }
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if ( bPlayer==null
                ||!player.hasPermission("dspk.cansee")
                || !(event.getEntity() instanceof LivingEntity target && target != player)
                || target.isInvulnerable()
                || bPlayer.isChiBlocked() || bPlayer.isBloodbent()
                || event.getDamage() <= 0
                || checkTicks(target, event.getDamage())
                || MovementHandler.isStopped(player)
                || (target instanceof Player p && !(p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE))) {
            return;
        }
        final double health = target.getHealth();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!event.isCancelled() && health > target.getHealth()) {
                    new DamageSplash(player, event.getDamage(), target, ability.getElement().getName());
                }
            }
        }.runTaskLater(DamageSplashesPK.plugin, 1);
        abilityDamagePlayersList.add(player);
    }

    private static boolean checkTicks(LivingEntity entity, double damage) {
        return (float)entity.getNoDamageTicks() > (float)entity.getMaximumNoDamageTicks() / 2.0F
                && (damage <= entity.getLastDamage() || Math.abs(damage - entity.getLastDamage()) <= DamageSplash.minDamageDelta);
    }
}
