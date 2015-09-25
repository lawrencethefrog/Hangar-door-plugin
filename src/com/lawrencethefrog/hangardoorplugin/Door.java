package com.lawrencethefrog.hangardoorplugin;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.scheduler.BukkitRunnable;

public class Door extends BukkitRunnable{
	private static ArrayList<Door> allDoors = new ArrayList<>();
	
	private int maxLength;
	private boolean allowCrossings;
	
	private Material frameMaterial;
	private Material doorMaterial;
	
	private ArrayList<Block> farFrameBlocks;
	private ArrayList<Block> nearFrameBlocks;
	private ArrayList<Block> signBlocks;
	private int frameWidth;	
	private int doorWidth;
	
	private int doorStartRow;
	private int doorEndRow;
	
	private int currentLength;
	private HashMap<Integer, BlockFace> directionChanges;	//map of when the direction changes and what the old direction was
	
	private Block nearEndBlock;
	private Block farEndBlock;
	private BlockFace currentDirection;
	private BlockFace signDir;
	private BlockFace antiSignDir;
	
	private Block repeaterBlock;
	
	boolean doorFound;
	boolean doorCompletelyFound;
	
	
	Door(MainClass mainClass, int frameWidth, Block signBlock, Block repeaterBlock, Block nearFrameBlock, Block farFrameBlock, BlockFace signDir, Material doorMaterial, boolean allowCrossings, int maxLength){
		
		this.allowCrossings = allowCrossings;
		this.maxLength = maxLength;
		
		Sign sign = (Sign)signBlock.getState();		
		String dirStr = sign.getLine(1);
		
		currentDirection = parseDirection(dirStr, signDir);
		if(currentDirection == null) return;
		allDoors.add(this);
		
		signBlocks = new ArrayList<>();
		directionChanges = new HashMap<>();
		farFrameBlocks = new ArrayList<>();
		nearFrameBlocks = new ArrayList<>();
		
		signBlocks.add(signBlock);
		this.repeaterBlock = repeaterBlock;
		frameMaterial = mainClass.getFrameMaterial();
		this.frameWidth = frameWidth;
		doorWidth = frameWidth-2;
		
		this.signDir = signDir;
		antiSignDir = signDir.getOppositeFace();
		
		this.nearEndBlock = nearFrameBlock;
		this.farEndBlock = farFrameBlock;
		this.doorMaterial = doorMaterial;
						
		nearFrameBlocks.add(nearFrameBlock);
		farFrameBlocks.add(farFrameBlock);
		
		doorFound = false;
		doorCompletelyFound=false;
		
		doorStartRow =-1;
		doorEndRow =-1;
		
		currentLength = 1;	
		
		while (currentLength<=maxLength){
			
			if(doorFound){
				if(checkRow(currentLength, Material.AIR)) {			//checks for end of door					
					doorCompletelyFound = true;
					doorEndRow = currentLength-1;
					
					break;
				} else if (!checkRow(currentLength, doorMaterial)){	//checks for broken row in door
					
					return;					
				}
			} else {
				if(checkRow(currentLength, doorMaterial)){					
					doorStartRow = currentLength;
					
					doorFound = true;
				}
			}		
			boolean frameExtendSuccessful = extendFrame();
			if(!frameExtendSuccessful){
				
				return;	
			}
		}
		if(!doorCompletelyFound) return;
		
		long period = mainClass.getPeriod();
		this.runTaskTimer(mainClass, period, period);
	}
	
	private boolean checkRow(int row, Material material) {
		Block frameBlock = nearFrameBlocks.get(row-1);
		for (int col = 1; col <= doorWidth; col++){
			Block block = frameBlock.getRelative(signDir, col);	//if block not valid door block
			
			if(block.getType() != material){
				
				return false;	//return false
			}
		}		
		//if all blocks valid
		return true;
	}

	@Override
	public void run() {
		
		if (!doChecks()) cancel();
		else {
			nudgeDoor();				
			if (!extendFrame()) cancel();
		}
	}
	@Override
	public void cancel(){
		
		super.cancel();
		allDoors.remove(this);
	}
	
	@SuppressWarnings("deprecation")
	private void nudgeDoor(){
			
		for (int row = doorEndRow+1; row >= doorStartRow; row--){
			
			Block frontFrameBlock = nearFrameBlocks.get(row-1);
			Block backFrameBlock = null;
			if(row != doorStartRow) backFrameBlock = nearFrameBlocks.get(row-2);	//if at start of door, does not set back frame block as it would be off array
			
			for(int col = 1; col <= frameWidth-2; col++){
				Block newBlock = frontFrameBlock.getRelative(signDir, col);		 
				if (row == doorStartRow){
					newBlock.setType(Material.AIR);		//or air if at back of door
					
					
				} else {						
					
					
					Block oldBlock = backFrameBlock.getRelative(signDir, col);
					
					if (row == doorEndRow+1){
						newBlock.setType(doorMaterial);				//sets material if at front of door
						
						
					}
									
					newBlock.setData(oldBlock.getData());
				}
				
			}			
		}
		doorEndRow++;
		doorStartRow++;
	}
	
	private boolean doChecks(){
		
		if(currentLength > maxLength) return false;
		
		if(repeaterBlock.getType() != Material.DIODE_BLOCK_ON) return false;
		for(Block block:signBlocks){
			if(block.getType()!=Material.WALL_SIGN) return false;
		}
		for(Block block:nearFrameBlocks){
			if(block.getType()!=frameMaterial) return false;
		}
		for(Block block:farFrameBlocks){
			if(block.getType()!=frameMaterial) return false;
		}
		
		
		for (int row = doorStartRow; row <= doorEndRow; row++){
			if(!checkRow(row, doorMaterial)){
				
				return false;
			}
		}
		
		if(!checkRow(doorEndRow+1, Material.AIR)){
			
			return false;
		}
		
			
		//if all checks pass
		return true;
	}
	
	private boolean extendFrame(){		//returning false means error
		
		Block newNearEndBlock = nearEndBlock.getRelative(currentDirection);
		Block newFarEndBlock = farEndBlock.getRelative(currentDirection);
		if(newNearEndBlock.getType() != frameMaterial){
			
			return false;
		} else if (newFarEndBlock.getType() != frameMaterial){
			
			return false;
		}
		else {
			nearEndBlock = newNearEndBlock;
			farEndBlock= newFarEndBlock;
			if(!allowCrossings){							//if frame crossings disabled
				if(nearFrameBlocks.contains(nearEndBlock)) return false;	//fails extension if block is already part of frame
			}
			nearFrameBlocks.add(nearEndBlock);
			farFrameBlocks.add(farEndBlock);
			
			currentLength++;
			
			//new direction check
			Block potentialSign = nearEndBlock.getRelative(antiSignDir);
			if(potentialSign.getType() != Material.WALL_SIGN) return true;
			
			@SuppressWarnings("deprecation")
			org.bukkit.material.Sign materialSign = new org.bukkit.material.Sign(Material.WALL_SIGN, potentialSign.getData());
			if(materialSign.getFacing() != antiSignDir) return true;
			
			Sign sign = (Sign)potentialSign.getState();
			String str = sign.getLine(1);
			BlockFace newDir = parseDirection(str, signDir);
			if(newDir==null) return true;
			
			
			signBlocks.add(potentialSign);
			directionChanges.put(currentLength, currentDirection);
			
			currentDirection = newDir;
			return true;
		}
	}
	
	private BlockFace parseDirection(String str, BlockFace signDirection){
		str = str.replace("񖶄", "");	//filters leading formatting
		switch(str){
		case "UP":
			
			return  BlockFace.UP;	
		case "DOWN":
			
			return  BlockFace.DOWN;		
		case "LEFT":
			
			return  getAdjacentFace(signDirection, Side.LEFT);	
		case "RIGHT":
			
			return getAdjacentFace(signDirection, Side.RIGHT);
		default:	//if no direction specified
			
			return null;
		}		
	}
	
	private static BlockFace getAdjacentFace(BlockFace face, Side direction){
		switch(face){
		case NORTH:
			if(direction == Side.LEFT) return BlockFace.WEST;
			else return BlockFace.EAST;
		case EAST:
			if(direction == Side.LEFT) return BlockFace.NORTH;
			else return BlockFace.SOUTH;
		case SOUTH:
			if(direction==Side.LEFT) return BlockFace.EAST;
			else return BlockFace.WEST;
		case WEST:
			if(direction==Side.LEFT) return BlockFace.SOUTH;
			else return BlockFace.NORTH;
		default:
			return null;
		}
	}
	
	private static enum Side{
		LEFT, RIGHT;
	}
}
