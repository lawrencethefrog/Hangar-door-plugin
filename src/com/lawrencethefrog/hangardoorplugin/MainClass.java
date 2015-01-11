package com.lawrencethefrog.hangardoorplugin;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.material.Diode;

public class MainClass extends JavaPlugin implements Listener{
	
	static final int maxFrameLength = 50;
	static final int maxLength = 10;
	static final int maxWidth = 10;
	static final Material allowedMaterial = Material.WOOD;
	

	
	@Override
	public void onEnable(){
		getServer().getLogger().info("Successfully started lawrencethefrog's hangar door plugin");
		getServer().getPluginManager().registerEvents(this, this);
		
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onRedstone(BlockRedstoneEvent event){
		final Block poweredBlock = event.getBlock();
		if (poweredBlock.getType() == Material.DIODE_BLOCK_OFF){
			
			@SuppressWarnings("deprecation")
			Diode repeater = new Diode(poweredBlock.getTypeId(), poweredBlock.getData());
			
			BlockFace direction = repeater.getFacing();
			Block firstIronBlock = poweredBlock.getRelative(direction);
			
			if(firstIronBlock.getType() == Material.IRON_BLOCK){
				
				BlockFace adjacentLeftFace = getAdjacentLeftFace(direction);
				BlockFace adjacentRightFace = getAdjacentLeftFace(direction.getOppositeFace());
				
				int doorWidth = 0;
				//get width
				for (int i = 1; i <= maxWidth + 1; i++){
					Block testingBlock = firstIronBlock.getRelative(adjacentLeftFace, i);
					if (testingBlock.getType() == Material.IRON_BLOCK) {
						doorWidth = i -1;		//save door width (minus removes width of end rail)
						//getServer().getLogger().info("Door width is " + doorWidth);
						break;
					}
				}
				//if no door width found, finish
				if (doorWidth == 0) return;
				
				int frameLength = 0;
				//find frame length and check validity
				RowLoop: for (int r = 0; r <= maxFrameLength; r++){
					for (int c = 0; c <= doorWidth +1; c+= (doorWidth+1)){
						Block testingBlock = firstIronBlock.getRelative(direction, r).getRelative(adjacentLeftFace, c);
						if (testingBlock.getType() != Material.IRON_BLOCK){
							frameLength = r-1;
							break RowLoop;
						}
					}					
				}
				if (frameLength == 0) {
					return;
				}
				
				
				Block firstDoorCorner = null;
				Block lastDoorCorner = null;
				
				//find door corners
				r: for(int r = 1; r <= frameLength; r++){
					for(int c = 1; c <= doorWidth; c++){
						Block testingBlock = poweredBlock.getRelative(direction, r).getRelative(adjacentLeftFace, c);
						if (testingBlock.getType() == allowedMaterial){						//if door block found
							if (firstDoorCorner == null){									//if first allowed block found
								firstDoorCorner = testingBlock;	 							//register as first corner
							}
							if (c == doorWidth) lastDoorCorner = testingBlock;				//if on end of row register as last
						} else if (firstDoorCorner != null)  {								//if not allowed block and  finish detection
							//getServer().getLogger().info("Error at X:" + testingBlock.getX() + ", Z:" + testingBlock.getZ());
							break r;													//finish detection
						}
					}
				}
				//if no full row found, finish
				if ((firstDoorCorner == null) || (lastDoorCorner == null)){
					//getServer().getLogger().info("Door detection failed! Either the door is longer than the frame or not a full row.");
					return;
				}
				
				//check blocks in front of lastDoorCorner's row are air				
				for (int c = 0; c < doorWidth; c++){
					//TODO: air checking
					Block testingBlock = lastDoorCorner.getRelative(direction).getRelative(adjacentRightFace, c);
					if (testingBlock.getType() != Material.AIR){	//if air is not found in front of door
						//getServer().getLogger().info("Block at X:" + testingBlock.getX() + ", Z:" + testingBlock.getZ() + " is not air!");
						return;										//finish
					}
				}
				
				ArrayList<Block> cornerBlockArray = new ArrayList<>();
				cornerBlockArray.add(firstDoorCorner);
				cornerBlockArray.add(lastDoorCorner);				
				nudgeBlocks(direction, cornerBlockArray, doorWidth);
				
			}		
		}
	}
	
	private BlockFace getAdjacentLeftFace(BlockFace startingFace){
		if (startingFace == BlockFace.NORTH) return BlockFace.EAST;
		else if (startingFace == BlockFace.EAST) return BlockFace.SOUTH;
		else if (startingFace == BlockFace.SOUTH) return BlockFace.WEST;
		else return BlockFace.NORTH;
	}
	
	private HashMap<String, Integer> getCoordinateChangeFromFace(BlockFace face){
		HashMap<String, Integer> map = new HashMap<>();
		int xShift;
		int zShift;
		
		if (face == BlockFace.NORTH){		//if going north
			xShift = 0;
			zShift = -1;
		} 
		else if (face == BlockFace.SOUTH){	//if going south
			xShift = 0;
			zShift = 1;
		} 
		else if (face == BlockFace.EAST){	//if going east
			xShift = 1;
			zShift = 0;
		} else if (face == BlockFace.WEST){	//if going west
			xShift = -1;
			zShift = 0;
		} 
		else if (face == BlockFace.NORTH_EAST){ 	//if going NE
			xShift = 1;
			zShift = -1;
		}
		else if (face == BlockFace.NORTH_WEST){ 	//if going NW
			xShift = -1;
			zShift = -1;
		}
		else if (face == BlockFace.SOUTH_WEST){ 	//if going SW
			xShift = -1;
			zShift = 1;
		}
		else { 										//if going SE
			xShift = 1;
			zShift = 1;
		}
		
		
		map.put("x", xShift);
		map.put("z", zShift);
		
		return map;
	}
	
	
	private void nudgeBlocks(BlockFace directionTowards, ArrayList<Block> cornerBlocks, int doorWidth){
		int xShiftTo = getCoordinateChangeFromFace(directionTowards).get("x");
		int zShiftTo = getCoordinateChangeFromFace(directionTowards).get("z");
		
		int lowestX = cornerBlocks.get(0).getX();
		int highestX = cornerBlocks.get(0).getX();
		int lowestZ = cornerBlocks.get(0).getZ();
		int highestZ = cornerBlocks.get(0).getZ();
		
		//get corners
		for (Block block : cornerBlocks){
			if 		(block.getX() < lowestX) lowestX = block.getX();
			else if (block.getX() > highestX) highestX= block.getX();
			if 		(block.getZ() < lowestZ) lowestZ = block.getZ();
			else if (block.getZ() > highestZ) highestZ= block.getZ();
		}
		//getServer().getLogger().info("lowestX: " + lowestX +", highestX: " + highestX+", lowestZ: " + lowestZ +", highestZ: " + highestZ);
		
		String copyCommand = "clone";		//command to copy blocks
		copyCommand = copyCommand + " " + String.valueOf(highestX );					//x1
		copyCommand = copyCommand + " " + String.valueOf(cornerBlocks.get(0).getY());	//y1
		copyCommand = copyCommand + " " + String.valueOf(highestZ);						//z1
		
		copyCommand = copyCommand + " " + String.valueOf(lowestX);						//x2
		copyCommand = copyCommand + " " + String.valueOf(cornerBlocks.get(0).getY());	//y2
		copyCommand = copyCommand + " " + String.valueOf(lowestZ);						//z2
		
		copyCommand = copyCommand + " " + String.valueOf(lowestX + xShiftTo);			//x
		copyCommand = copyCommand + " " + String.valueOf(cornerBlocks.get(0).getY());	//y
		copyCommand = copyCommand + " " + String.valueOf(lowestZ + zShiftTo);			//z
		copyCommand = copyCommand + " replace force";									//replace and force
		
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), copyCommand);
		
		
		ArrayList<Block> blocksUpForDeletion = new ArrayList<>();
		BlockFace adjacentLeftFace = getAdjacentLeftFace(directionTowards);
		for (int i = 0; i < doorWidth; i++){
			blocksUpForDeletion.add(cornerBlocks.get(0).getRelative(adjacentLeftFace, i));
		}
		//getServer().getLogger().info("Deletion blocks: " + blocksUpForDeletion.toString());
		lowestX = cornerBlocks.get(0).getX();
		highestX = cornerBlocks.get(0).getX();
		lowestZ = cornerBlocks.get(0).getZ();
		highestZ = cornerBlocks.get(0).getZ();
		//get corners
		for (Block block : blocksUpForDeletion){
			if 		(block.getX() < lowestX) lowestX = block.getX();
			else if (block.getX() > highestX) highestX= block.getX();
			if 		(block.getZ() < lowestZ) lowestZ = block.getZ();
			else if (block.getZ() > highestZ) highestZ= block.getZ();
		}
		
		
		String fillCommand = "fill";		//command to fill space behind door with air
		fillCommand = fillCommand + " " + String.valueOf(lowestX);						//x
		fillCommand = fillCommand + " " + String.valueOf(cornerBlocks.get(0).getY());	//y
		fillCommand = fillCommand + " " + String.valueOf(lowestZ);						//z
		fillCommand = fillCommand + " " + String.valueOf(highestX);						//x
		fillCommand = fillCommand + " " + String.valueOf(cornerBlocks.get(0).getY());	//y
		fillCommand = fillCommand + " " + String.valueOf(highestZ);						//z
		fillCommand = fillCommand + " minecraft:air";
		
		//getServer().getLogger().info("Fill cmd: " + fillCommand);
		
		
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), fillCommand);
		
	}
	
	
	
}
