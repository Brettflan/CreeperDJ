package com.wimbli.creeperdj;

import java.util.List;
import java.util.Random;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;


public class CreeperDJ extends JavaPlugin
{
	private static final Random generator = new Random();
	private FileConfiguration cfg = null;
	private double recordDropRate;
	private boolean playerKillsOnly;

	@Override
	public void onEnable()
	{
		this.
		cfg = getConfig();
		cfg.options().copyDefaults(true);
		saveConfig();
		double perc = cfg.getDouble("record_drop_percentage", 3.0);
		recordDropRate = (perc == 0.0) ? 0.0 : (perc / 100);
		playerKillsOnly = cfg.getBoolean("player_kills_only", true);

		getServer().getPluginManager().registerEvents(new CreeperDJEntityListener(), this);

		PluginDescriptionFile desc = this.getDescription();
		System.out.println( "[" + desc.getName() + "] Drop Chance: " + (recordDropRate * 100) + "%. " + (playerKillsOnly ? "Player kills only." : "All deaths considered.") );
	}


	private class CreeperDJEntityListener implements Listener
	{
		@EventHandler(priority = EventPriority.NORMAL)
		public void onEntityDeath(EntityDeathEvent event)
		{
			Entity creeper = event.getEntity();
			if ( ! (creeper instanceof Creeper))
				return;

			List<ItemStack> drops = event.getDrops();
			for (ItemStack item : drops)
			{
				if (item.getType() == Material.GOLD_RECORD || item.getType() == Material.GREEN_RECORD)
				{	// already dropping a record, presumably from being killed by a skeleton; remove it
					drops.remove(item);
				}
			}

			// cause of death?
			EntityDamageEvent lastDamage = creeper.getLastDamageCause();
			Entity killer = null;
			if (lastDamage.getCause() == EntityDamageEvent.DamageCause.PROJECTILE || lastDamage.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
			{
				killer = ((EntityDamageByEntityEvent)lastDamage).getDamager();
				if (killer instanceof Projectile) 
					killer = ((Projectile)killer).getShooter();
			}

			// it was a skeleton, so give a random record
			if (killer != null && killer instanceof Skeleton)
			{
				drops.add(new ItemStack(randomRecord(), 1));
				return;
			}

			// no drops except from skeleton kills?
			if (recordDropRate <= 0.0)
				return;

			// only drop for player kills, and cause of death isn't player?
			if (playerKillsOnly && (killer == null || !(killer instanceof Player)))
				return;

			// OK then, there's a chance for a random record drop based on the drop rate
			if (generator.nextDouble() <= recordDropRate)
				drops.add(new ItemStack(randomRecord(), 1));
		}
	}

	private static int randomRecord()
	{	// GOLD_RECORD is material 2256, last record (RECORD_11) is 2266
		return 2256 + generator.nextInt(11);
	}
}
