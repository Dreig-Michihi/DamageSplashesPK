package me.dreig_michihi.damagesplashespk;

import com.projectkorra.projectkorra.event.AbilityDamageEntityEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.Set;

public class DamageListener implements Listener {
    private static final Set<Player> abilityDamagePlayersList = new HashSet<>();

    @EventHandler(ignoreCancelled = true)
    public void DamageEvent(final EntityDamageByEntityEvent event) {
        if (event.getEntity().isInvulnerable() || event.getEntity() instanceof Player player &&
                (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE))
            return;
        if (event.getEntity() instanceof LivingEntity living) {
            Player player;
            if (event.getDamager() instanceof Player damager)
                player = damager;
            else if (event.getDamager() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player damager)
                player = damager;
            else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player damager)
                player = damager;
            else
                return;
            if (!player.hasPermission("dspk.cansee"))
                return;
            if (!abilityDamagePlayersList.contains(player))
                new DamageSplash(player, event.getDamage(), living, null);
            if (!abilityDamagePlayersList.isEmpty() && event.getDamager() instanceof Player damager)
                abilityDamagePlayersList.remove(damager);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void AbilityDamageEvent(final AbilityDamageEntityEvent event) {
        if (event.getEntity().isInvulnerable() || event.getEntity() instanceof Player player &&
                (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE))
            return;
        Player player;
        if (event.getSource() != null)
            player = event.getSource();
        else if (event.getAbility() != null && event.getAbility().getPlayer() != null) {
            player = event.getAbility().getPlayer();
        } else {
            return;
        }
        if (!player.hasPermission("dspk.cansee"))
            return;
        if (event.getEntity() instanceof LivingEntity living && living != player) {
            new DamageSplash(player, event.getDamage(), living, event.getAbility().getElement());
            abilityDamagePlayersList.add(player);
        }
    }
}
