package com.lawrencethefrog.hangardoorplugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.material.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.material.Diode;
import org.bukkit.plugin.java.JavaPlugin;

public class MainClass extends JavaPlugin implements Listener {	
	
	private Material frameMaterial;
	private int maxFrameWidth;
	private long period;
	private HashMap<String, Material> doorTypes;
	private int maxLength;
	private boolean allowCrossings;
	
	@Override
	public void onEnable(){
		Server server = getServer();
		Logger logger = getLogger();
		
		doorTypes = new HashMap<>();
		
		saveDefaultConfig();
		FileConfiguration config = getConfig();
		
		String frameMaterialString = config.getString("FrameMaterial");
		if(frameMaterialString == null) {
			logger.info("Error reading plugin configuration, no frame material defined!");
			setEnabled(false);
			return;
		}
		Material configFrameMaterial = Material.valueOf(frameMaterialString);
		if(configFrameMaterial == null){
			logger.info("Error reading plugin configuration, invalid frame material!");
			setEnabled(false);
			return;
		}
		frameMaterial = configFrameMaterial;
		
		maxFrameWidth = config.getInt("MaxFrameWidth");
		period = config.getLong("MovePeriod");
		maxLength = config.getInt("MaxFrameLength");
		allowCrossings = config.getBoolean("AllowFrameCrossings");
				
		ConfigurationSection mapSec = config.getConfigurationSection("DoorTypes");
		if (mapSec == null) {
			logger.info("Error reading plugin configuration, no door types defined!");
			setEnabled(false);
			return;		
		}
		Map<String, Object> configFrameMaterials = mapSec.getValues(false);
		for (String key : configFrameMaterials.keySet()){
			Object object = configFrameMaterials.get(key);
			Material material = Material.valueOf(object.toString());
			if (material == null) {
				logger.info("Error reading plugin configuration, invalid door type definetion found!");
				setEnabled(false);
				return;
			}
			else doorTypes.put(key, material);
		}
		logger.info("Succesfully initialized plugin.");		
		server.getPluginManager().registerEvents(this, this);
	}
	
	@EventHandler
	public void onRestone(BlockRedstoneEvent event){
		Block poweredBlock = event.getBlock();
		if(poweredBlock.getType() != Material.DIODE_BLOCK_OFF) return;					//checks if repeater		
		@SuppressWarnings("deprecation")
		Diode repeater = new Diode(poweredBlock.getTypeId(), poweredBlock.getData());
		BlockFace repeaterDir = repeater.getFacing();
		Block signBlock = poweredBlock.getRelative(repeaterDir);		
		if(signBlock.getType() != Material.WALL_SIGN) return;							//checks if wall sign present
		@SuppressWarnings("deprecation")
		Sign materialSign = new Sign(Material.WALL_SIGN, signBlock.getData());
		BlockFace signDir = materialSign.getAttachedFace();
		Block blockBehindSign = signBlock.getRelative(signDir);
		if(blockBehindSign.getType() != frameMaterial) return;							//checks frame block behind sign
		
		int frameWidth = 0;
		Block farFrameBlock = null;
		for (int i = 2; i< maxFrameWidth; i++){											//searches for opposite frame block
			Block block = blockBehindSign.getRelative(signDir, i);
			if (block.getType() == frameMaterial){
				frameWidth = i+1;
				farFrameBlock = block;
				break;			
			}			
		}
		if (frameWidth == 0) return;														//checks if other frame block found
		
		org.bukkit.block.Sign blockSign = (org.bukkit.block.Sign)signBlock.getState();
		String signLine = blockSign.getLine(0).replace("񖶄", "");;
		Material doorMaterial = doorTypes.get(signLine);	//gets door material
		if(doorMaterial == null) return;
		
		new Door(this, frameWidth, signBlock, poweredBlock, blockBehindSign, farFrameBlock, signDir, doorMaterial, allowCrossings , maxLength);
	}

	long getPeriod() {
		return period;
	}
	
	Material getFrameMaterial(){
		return frameMaterial;
	}

}
