package com.lawrencethefrog.hangardoorplugin;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.material.Diode;


public class MoveTask extends BukkitRunnable{
	
	
	Block poweredBlock;
	BlockRedstoneEvent event;
	MainClass plugin;
	
	
	public MoveTask(BlockRedstoneEvent event, MainClass plugin){
		this.plugin = plugin;
		poweredBlock = event.getBlock();
		this.runTaskTimer(plugin, 0, plugin.getTicksBetweenCycles().longValue());
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
	
	@Override
	public void run() {
		if (poweredBlock.getType() == Material.DIODE_BLOCK_ON){
			
			@SuppressWarnings("deprecation")
			Diode repeater = new Diode(poweredBlock.getTypeId(), poweredBlock.getData());
			
			BlockFace direction = repeater.getFacing();
			Block firstIronBlock = poweredBlock.getRelative(direction);
			
			if(firstIronBlock.getType() == Material.IRON_BLOCK){
				
				BlockFace adjacentRightFace = getAdjacentRightFace(direction);
				int doorWidth = 0;
				//gets width of door
				for (int i = 1; i <= plugin.maxWidth.intValue() + 1; i++){
					Block testingBlock = firstIronBlock.getRelative(adjacentRightFace, i);
					if (testingBlock.getType() == Material.IRON_BLOCK) {
						doorWidth = i -1;		//saves door width (minus removes width of end rail)
						break;
					}
				}
				//if no door width found, finishes
				if (doorWidth == 0) {
					cancel();
					return;
				}
				
				int frameLength = 0;
				//finds frame length and checks validity
				for (int row = 1; row <= plugin.maxFrameLength.intValue(); row++){
					Block leftRailBlock = poweredBlock.getRelative(direction, row);
					Block rightRailBlock = leftRailBlock.getRelative(adjacentRightFace, doorWidth+1);
					//if the rail blocks on the sides are present, set frame length to current row
					if(plugin.getAllowedFrameMaterials().contains(leftRailBlock.getType()) && plugin.getAllowedFrameMaterials().contains(rightRailBlock.getType())){
						frameLength=row;
					} else {
						frameLength=row-1;
						break;
					}
				}
				//returns if the frame has length 0 (does not exist) or is length 1 (useless)
				if (frameLength == 0 || frameLength == 1) {
					cancel();
					return;
				}
				
				
				Block firstDoorCorner = null;
				Block lastDoorCorner = null;
				
				int firstCornerRow = 0;
				int firstCornerCol = 0;
				int doorLength = 0;
				
				ArrayList<Block> doorBlocks = new ArrayList<>();
				//finds door corners
				rowLoop: for(int row = 1; row < frameLength; row++){
					for(int c = 1; c <= doorWidth; c++){
						Block testingBlock = poweredBlock.getRelative(direction, row).getRelative(adjacentRightFace, c);
						if (plugin.getAllowedDoorMaterials().contains(testingBlock.getType())){					//if door block found
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
					cancel();
					return;
				}
				
				
				BlockFace adjacentLeftFace = adjacentRightFace.getOppositeFace();
				//check blocks in front of lastDoorCorner's row are air				
				for (int c = 0; c < doorWidth; c++){
					Block testingBlock = lastDoorCorner.getRelative(direction).getRelative(adjacentLeftFace, c);
					if (testingBlock.getType() != Material.AIR){	//if air is not found in front of door
						cancel();
						return;										//finishes
					}
				}
									
				nudgeBlocks(direction, firstDoorCorner, lastDoorCorner, doorBlocks, doorWidth, doorLength);
				
			}		
		} else {
			cancel();
			return;
		}
	}
	
}
