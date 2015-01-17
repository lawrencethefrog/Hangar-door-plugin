package com.lawrencethefrog.hangardoorplugin;

import java.util.ArrayList;

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
				
				int firstCornerRow = 0;
				int doorLength = 0;
				
				ArrayList<Block> doorBlocks = new ArrayList<>();
				
				//find door corners
				r: for(int r = 1; r <= frameLength; r++){
					for(int c = 1; c <= doorWidth; c++){
						Block testingBlock = poweredBlock.getRelative(direction, r).getRelative(adjacentLeftFace, c);
						if (testingBlock.getType() == allowedMaterial){						//if door block found
							if (firstDoorCorner == null){									//if first allowed block found
								firstDoorCorner = testingBlock;	 							//register as first corner
								firstCornerRow = r;
							}
							doorBlocks.add(testingBlock);
							if (c == doorWidth) lastDoorCorner = testingBlock;				//if on end of row register as last
						} else if (firstDoorCorner != null)  {								//if not allowed block and  finish detection
							doorLength = r-firstCornerRow;
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
					Block testingBlock = lastDoorCorner.getRelative(direction).getRelative(adjacentRightFace, c);
					if (testingBlock.getType() != Material.AIR){	//if air is not found in front of door
						//getServer().getLogger().info("Block at X:" + testingBlock.getX() + ", Z:" + testingBlock.getZ() + " is not air!");
						return;										//finish
					}
				}
					
				nudgeBlocks(direction, firstDoorCorner, lastDoorCorner, doorBlocks, doorWidth, doorLength);
				
			}		
		}
	}
	
	private BlockFace getAdjacentLeftFace(BlockFace startingFace){
		if (startingFace == BlockFace.NORTH) return BlockFace.EAST;
		else if (startingFace == BlockFace.EAST) return BlockFace.SOUTH;
		else if (startingFace == BlockFace.SOUTH) return BlockFace.WEST;
		else return BlockFace.NORTH;
	}
	
	
	@SuppressWarnings("deprecation")
	private void nudgeBlocks(BlockFace directionTowards, Block firstCorner, Block lastCorner, ArrayList<Block> allBlocks, int doorWidth, int doorLength){
		
		
		
		BlockFace directionFrom = directionTowards.getOppositeFace();
		BlockFace directionFromLeft = getAdjacentLeftFace(directionFrom);
		Block firstDestinationBlock = lastCorner.getRelative(directionTowards);
		
		
		for (int destRow = 0; destRow < doorLength; destRow++){
			for(int destCol = 0; destCol < doorWidth; destCol++){
				Block destinationBlock = firstDestinationBlock.getRelative(directionFrom, destRow).getRelative(directionFromLeft, destCol);
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
