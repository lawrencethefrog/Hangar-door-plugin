package com.lawrencethefrog.hangardoorplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.material.Diode;

public class MainClass extends JavaPlugin implements Listener{
	
	//uses atomic integers so they can be modified in a method
	static AtomicInteger maxFrameLength = new AtomicInteger(50);
	static AtomicInteger maxWidth = new AtomicInteger(10);
	@SuppressWarnings("serial")
	ArrayList<Material> allowedDoorMaterials = new ArrayList<Material>(){{
		add(Material.WOOD);
	}};
	@SuppressWarnings("serial")
	ArrayList<Material> allowedFrameMaterials = new ArrayList<Material>(){{
		add(Material.IRON_BLOCK);
	}};
	

	
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
		tryUpdateMaterialArrayListFromConfig(config, "AllowedMaterials.Door", allowedDoorMaterials);
		tryUpdateMaterialArrayListFromConfig(config, "AllowedMaterials.Frame", allowedFrameMaterials);
		
	}
	
	private void tryUpdateAtomicIntegerFromConfig(FileConfiguration config, String configPath, AtomicInteger dest){
		Integer integer = config.getInt(configPath);
		if (integer != null){
			dest.set(integer.intValue());
		} else exitWithConfigError("No integer value found at path " + configPath);
	}
	//method that updates arraylist of materials from config file
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
		Block poweredBlock = event.getBlock();
		if (poweredBlock.getType() == Material.DIODE_BLOCK_OFF){
			
			@SuppressWarnings("deprecation")
			Diode repeater = new Diode(poweredBlock.getTypeId(), poweredBlock.getData());
			
			BlockFace direction = repeater.getFacing();
			Block firstIronBlock = poweredBlock.getRelative(direction);
			
			if(firstIronBlock.getType() == Material.IRON_BLOCK){
				
				BlockFace adjacentRightFace = getAdjacentRightFace(direction);
				int doorWidth = 0;
				//gets width of door
				for (int i = 1; i <= maxWidth.intValue() + 1; i++){
					Block testingBlock = firstIronBlock.getRelative(adjacentRightFace, i);
					if (testingBlock.getType() == Material.IRON_BLOCK) {
						doorWidth = i -1;		//saves door width (minus removes width of end rail)
						break;
					}
				}
				//if no door width found, finishes
				if (doorWidth == 0) return;
				
				int frameLength = 0;
				//finds frame length and checks validity
				for (int row = 1; row <= maxFrameLength.intValue(); row++){
					Block leftRailBlock = poweredBlock.getRelative(direction, row);
					Block rightRailBlock = leftRailBlock.getRelative(adjacentRightFace, doorWidth+1);
					//if the rail blocks on the sides are present, set frame length to current row
					if(allowedFrameMaterials.contains(leftRailBlock.getType()) && allowedFrameMaterials.contains(rightRailBlock.getType())){
						frameLength=row;
					} else {
						frameLength=row-1;
						break;
					}
				}
				//returns if the frame has length 0 (does not exist) or is length 1 (useless)
				if (frameLength == 0 || frameLength == 1) return;
				
				
				Block firstDoorCorner = null;
				Block lastDoorCorner = null;
				
				int firstCornerRow = 0;
				int firstCornerCol = 0;
				int doorLength = 0;
				
				ArrayList<Block> doorBlocks = new ArrayList<>();
				//finds door corners
				rowLoop: for(int row = 1; row <= frameLength; row++){
					for(int c = 1; c <= doorWidth; c++){
						Block testingBlock = poweredBlock.getRelative(direction, row).getRelative(adjacentRightFace, c);
						if (allowedDoorMaterials.contains(testingBlock.getType())){					//if door block found
							if (firstDoorCorner == null){								//if first allowed block found
								firstDoorCorner = testingBlock;	 						//registers as first corner
								firstCornerRow = row;
								firstCornerCol = c;
							}
							doorBlocks.add(testingBlock);
							if (c == doorWidth){
								lastDoorCorner = testingBlock;				//if on end of row registers as last
							}
							doorLength = row;
						} else {										//if not allowed block
							
							if (firstDoorCorner != null)  {				//if first door block has been found		
								doorLength = row-firstCornerRow;
								break rowLoop;							//finishes detection
							}
						}
					}
				}
				
				//if no full row found, finish
				if ((firstDoorCorner == null) || (lastDoorCorner == null) || (doorLength == 0) || (firstCornerCol != 1)){
					return;
				}
				
				
				BlockFace adjacentLeftFace = adjacentRightFace.getOppositeFace();
				//check blocks in front of lastDoorCorner's row are air				
				for (int c = 0; c < doorWidth; c++){
					Block testingBlock = lastDoorCorner.getRelative(direction).getRelative(adjacentLeftFace, c);
					if (testingBlock.getType() != Material.AIR){	//if air is not found in front of door
						return;										//finishes
					}
				}
									
				nudgeBlocks(direction, firstDoorCorner, lastDoorCorner, doorBlocks, doorWidth, doorLength);
				
			}		
		}
	}
	
	private BlockFace getAdjacentRightFace(BlockFace startingFace){
		if (startingFace == BlockFace.NORTH) return BlockFace.EAST;
		else if (startingFace == BlockFace.EAST) return BlockFace.SOUTH;
		else if (startingFace == BlockFace.SOUTH) return BlockFace.WEST;
		else return BlockFace.NORTH;
	}
	
	//nudges blocks by copying the material and data of each block over to the new block, and replacing the last row with air
	@SuppressWarnings("deprecation")
	private void nudgeBlocks(BlockFace directionTowards, Block firstCorner, Block lastCorner, ArrayList<Block> allBlocks, int doorWidth, int doorLength){
		BlockFace directionFrom = directionTowards.getOppositeFace();
		BlockFace directionFromRight = getAdjacentRightFace(directionFrom);
		Block firstDestinationBlock = lastCorner.getRelative(directionTowards);
		
		for (int destRow = 0; destRow < doorLength; destRow++){
			for(int destCol = 0; destCol < doorWidth; destCol++){
				Block destinationBlock = firstDestinationBlock.getRelative(directionFrom, destRow).getRelative(directionFromRight, destCol);
				Block sourceBlock = destinationBlock.getRelative(directionFrom);
				destinationBlock.setType(sourceBlock.getType());
				destinationBlock.setData(sourceBlock.getData());
				
				if(destRow == doorLength-1){
					sourceBlock.setType(Material.AIR);
				}
			}
		}
	}
	
	
	
}
