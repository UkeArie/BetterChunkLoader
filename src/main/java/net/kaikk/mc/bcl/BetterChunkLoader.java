package net.kaikk.mc.bcl;

import java.io.File;
import java.util.UUID;

import net.kaikk.mc.bcl.datastore.DataStoreManager;
import net.kaikk.mc.bcl.datastore.MySqlDataStore;
import net.kaikk.mc.bcl.datastore.XmlDataStore;
import net.kaikk.mc.bcl.forgelib.BCLForgeLib;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterChunkLoader extends JavaPlugin {
	private static BetterChunkLoader instance;
	private Config config;
	
	public void onLoad() {
		// Register XML DataStore
		DataStoreManager.registerDataStore("XML", XmlDataStore.class);
		
		// Register MySQL DataStore
		DataStoreManager.registerDataStore("MySQL", MySqlDataStore.class);
	}
	
	public void onEnable() {
		// check if forge is running
		try {
			Class.forName("net.minecraftforge.common.ForgeVersion");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Cauldron/KCauldron and BCLForgeLib are needed to run this plugin!");
		}
		
		// check if BCLForgeLib is present
		try {
			Class.forName("net.kaikk.mc.bcl.forgelib.BCLForgeLib");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("BCLForgeLib is needed to run this plugin!");
		}
		
		instance=this;
		
		try {
			// load config
			this.getLogger().info("Loading config...");
			this.config = new Config(this);
			
			// instantiate data store, if needed
			if (DataStoreManager.getDataStore()==null) {
				DataStoreManager.setDataStoreInstance(config.dataStore);
			}
			
			// load datastore
			this.getLogger().info("Loading "+DataStoreManager.getDataStore().getName()+" Data Store...");
			DataStoreManager.getDataStore().load();
			
			this.getLogger().info("Loaded "+DataStoreManager.getDataStore().getChunkLoaders().size()+" chunk loaders data.");
			this.getLogger().info("Loaded "+DataStoreManager.getDataStore().getPlayersData().size()+" players data.");
			
			// load always on chunk loaders
			int count=0;
			for (CChunkLoader cl : DataStoreManager.getDataStore().getChunkLoaders()) {
				if (cl.isLoadable()) {
					BCLForgeLib.instance().addChunkLoader(cl);
					count++;
				}
			}
			
			this.getLogger().info("Loaded "+count+" always-on chunk loaders.");
			
			this.getLogger().info("Loading Listeners...");
			this.getServer().getPluginManager().registerEvents(new EventListener(), this);
			this.getCommand("betterchunkloader").setExecutor(new CommandExec(this));
			
			this.getLogger().info("Load complete.");
		} catch (Exception e) {
			e.printStackTrace();
			this.getLogger().warning("Load failed!");
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}
	
	public void onDisable() {
		for (CChunkLoader cl : DataStoreManager.getDataStore().getChunkLoaders()) {
			BCLForgeLib.instance().removeChunkLoader(cl);
		}
		instance=null;
	}

	public static BetterChunkLoader instance() {
		return instance;
	}
	
	public static long getPlayerLastPlayed(UUID playerId) {
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
		if (player.getLastPlayed()!=0) {
			return player.getLastPlayed();
		} else if (player.getName()!=null && !player.getName().isEmpty()) {
			return getPlayerDataLastModified(playerId);
		}
		
		return 0;
	}
	
	public static long getPlayerDataLastModified(UUID playerId) {
		File playerData =new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata"+File.separator+playerId.toString()+".dat");
		if (playerData.exists()) {
			return playerData.lastModified();
		}
		return 0;
	}
	
	public Config config() {
		return this.config;
	}
}
