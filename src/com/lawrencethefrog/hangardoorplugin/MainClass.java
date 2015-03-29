package com.lawrencethefrog.hangardoorplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin implements Listener {
	//TODO config option for ticksBetweenCycles
	//uses atomic integers so they can be modified in a method
	AtomicInteger maxFrameLength = new AtomicInteger(50);
	AtomicInteger maxWidth = new AtomicInteger(10);
	AtomicInteger ticksBetweenCycles = new AtomicInteger(20);
	

	@SuppressWarnings("serial")
	ArrayList<Material> allowedDoorMaterials = new ArrayList<Material>(){{
		add(Material.WOOD);
	}};
	@SuppressWarnings("serial")
	ArrayList<Material> allowedFrameMaterials = new ArrayList<Material>(){{
		add(Material.IRON_BLOCK);
	}};
	
	public AtomicInteger getMaxFrameLength() {
		return maxFrameLength;
	}

	public AtomicInteger getMaxWidth() {
		return maxWidth;
	}
	public AtomicInteger getTicksBetweenCycles() {
		return ticksBetweenCycles;
	}
	public ArrayList<Material> getAllowedDoorMaterials() {
		return allowedDoorMaterials;
	}
	
	public ArrayList<Material> getAllowedFrameMaterials() {
		return allowedFrameMaterials;
	}

		
		@Override
	public void onEnable(){		
		getServer().getPluginManager().registerEvents(this, this);	
		doConfigThings();
			
	}
	
	private void doConfigThings(){
		//if there is no config, saves the default one from the jar
		saveDefaultConfig();
		
		FileConfiguration config = getConfig();
		
		tryUpdateAtomicIntegerFromConfig(config, "MaxFrameLength", maxFrameLength);
		tryUpdateAtomicIntegerFromConfig(config, "MaxWidth", maxWidth);
		tryUpdateAtomicIntegerFromConfig(config, "TicksBetweenCycles", ticksBetweenCycles);
		tryUpdateMaterialArrayListFromConfig(config, "AllowedMaterials.Door", allowedDoorMaterials);
		tryUpdateMaterialArrayListFromConfig(config, "AllowedMaterials.Frame", allowedFrameMaterials);
		
	}
	//method that updates AtomicInteger from config
	private void tryUpdateAtomicIntegerFromConfig(FileConfiguration config, String configPath, AtomicInteger dest){
		Integer integer = config.getInt(configPath);
		if (integer != null){
			if(integer.equals(0)){
				exitWithConfigError("Integer value at " + configPath + " must not be null or equal to 0");
			} //else
			dest.set(integer.intValue());
		} else exitWithConfigError("No integer value found at path " + configPath);
	}
	//method that updates ArrayList of materials from config file
	private void tryUpdateMaterialArrayListFromConfig(FileConfiguration config, String configPath, ArrayList<Material> dest){
		List<String> stringList = config.getStringList(configPath);
		
		if (stringList != null){							//if the path is correct
			ArrayList<Material> materialsFromConfig = new ArrayList<>();
			for (String str : stringList){					//loops though material list from config
				Material parsedMaterial = Material.getMaterial(str);
				if (parsedMaterial != null){				//if the item on the list is a valid material
					materialsFromConfig.add(parsedMaterial);
				} else exitWithConfigError("Invalid material found in material list at path " + configPath + " index " + stringList.indexOf(str));
			}
			//if any correct items were found in the config, overwrites the ArrayList
			if(materialsFromConfig.size() != 0){
				dest.clear();
				dest.addAll(materialsFromConfig);
			} else exitWithConfigError("No materials were found in the list at path " + configPath);
		} else exitWithConfigError("Nothing found at path " + configPath);
	}
	
	private void exitWithConfigError(String details){
		getLogger().info("Something was wrong in the configuration file! The plugin will be disabled. Details of the problem are below");
		getLogger().info(details);
		setEnabled(false);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onRedstone(BlockRedstoneEvent event){
		new MoveTask(event, this);
	}

}
