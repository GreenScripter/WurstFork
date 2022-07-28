/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.mixinterface.IMinecraftClient;

@SearchTags({"bedrock breaker"})
public final class BedrockBreakHack extends Hack
	implements UpdateListener, BlockBreakingProgressListener
{
	public BedrockBreakHack()
	{
		super("BedrockBreak",
			"Break bedrock using pistons and redstone torches.\nRequires efficiency 5 and works best with a haste 2 beacon.");
		setCategory(Category.BLOCKS);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(BlockBreakingProgressListener.class, this);
		BreakingFlowController.turnOn();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(BlockBreakingProgressListener.class, this);
		BreakingFlowController.turnOff();
	}
	
	@Override
	public void onUpdate()
	{
		BreakingFlowController.tick();
	}
	
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		if(im.getCurrentBreakingProgress() >= 1)
			return;
		
		BlockPos blockPos = event.getBlockPos();
		BreakingFlowController.addBlockPosToList(blockPos);
	}
}

class TargetBlock
{
	private BlockPos blockPos;
	private BlockPos redstoneTorchBlockPos;
	private BlockPos pistonBlockPos;
	private ClientWorld world;
	private Status status;
	private BlockPos slimeBlockPos;
	private int tickTimes;
	private boolean hasTried;
	private int stuckTicksCounter;
	public Block type = Blocks.BEDROCK;
	
	public TargetBlock(BlockPos pos, ClientWorld world)
	{
		this.hasTried = false;
		this.stuckTicksCounter = 0;
		this.status = Status.UNINITIALIZED;
		this.blockPos = pos;
		this.world = world;
		this.pistonBlockPos = pos.up();
		this.redstoneTorchBlockPos = CheckingEnvironment
			.findNearbyFlatBlockToPlaceRedstoneTorch(this.world, this.blockPos);
		if(redstoneTorchBlockPos == null)
		{
			this.slimeBlockPos =
				CheckingEnvironment.findPossibleSlimeBlockPos(world, pos);
			if(slimeBlockPos != null)
			{
				BlockPlacer.simpleBlockPlacement(slimeBlockPos,
					Blocks.NETHERRACK);
				redstoneTorchBlockPos = slimeBlockPos.up();
			}else
			{
				this.status = Status.FAILED;
			}
		}
	}
	
	public Status tick()
	{
		this.tickTimes++;
		updateStatus();
		System.out.println(this.status);
		switch(this.status)
		{
			case UNINITIALIZED:
			InventoryManager.switchToItem(Blocks.PISTON);
			BlockPlacer.pistonPlacement(this.pistonBlockPos, Direction.UP);
			InventoryManager.switchToItem(Blocks.REDSTONE_TORCH);
			BlockPlacer.simpleBlockPlacement(this.redstoneTorchBlockPos,
				Blocks.REDSTONE_TORCH);
			break;
			case UNEXTENDED_WITH_POWER_SOURCE:
			break;
			case EXTENDED:
			InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
			float delta = this.world.getBlockState(this.pistonBlockPos)
				.calcBlockBreakingDelta(MinecraftClient.getInstance().player,
					MinecraftClient.getInstance().player.world,
					this.pistonBlockPos);
			IClientPlayerInteractionManager im =
				((IMinecraftClient)MinecraftClient.getInstance())
					.getInteractionManager();
			if(im.getCurrentBreakingProgress() + delta >= 1)
			{
				ArrayList<BlockPos> nearByRedstoneTorchPosList =
					CheckingEnvironment.findNearbyRedstoneTorch(world,
						pistonBlockPos);
				for(BlockPos pos : nearByRedstoneTorchPosList)
				{
					BlockBreaker.breakBlock(world, pos);
				}
			}
			net.wurstclient.util.BlockBreaker
				.breakOneBlock(this.pistonBlockPos);
			
			if(im.getCurrentBreakingProgress() == 0)
			{
				System.out.println("next");
				
				// BlockBreaker.breakBlock(this.world, this.pistonBlockPos);
				BlockPlacer.pistonPlacement(this.pistonBlockPos,
					Direction.DOWN);
				this.hasTried = true;
			}
			break;
			case RETRACTED:
			net.wurstclient.util.BlockBreaker
				.breakBlocksWithPacketSpam(Arrays.asList(pistonBlockPos));
			IClientPlayerInteractionManager im2 =
				((IMinecraftClient)MinecraftClient.getInstance())
					.getInteractionManager();
			if(im2.getCurrentBreakingProgress() == 0)
			{
				System.out.println("completing");
				BlockBreaker.breakBlock(world, pistonBlockPos.up());
				if(this.slimeBlockPos != null)
				{
					BlockBreaker.breakBlock(world, slimeBlockPos);
				}
				status = Status.DONE;
				return Status.DONE;
			}
			return Status.RETRACTED;
			
			case RETRACTING:
			return Status.RETRACTING;
			case UNEXTENDED_WITHOUT_POWER_SOURCE:
			InventoryManager.switchToItem(Blocks.REDSTONE_TORCH);
			BlockPlacer.simpleBlockPlacement(this.redstoneTorchBlockPos,
				Blocks.REDSTONE_TORCH);
			break;
			case FAILED:
			BlockBreaker.breakBlock(world, pistonBlockPos);
			BlockBreaker.breakBlock(world, pistonBlockPos.up());
			return Status.FAILED;
			case STUCK:
			BlockBreaker.breakBlock(world, pistonBlockPos);
			BlockBreaker.breakBlock(world, pistonBlockPos.up());
			break;
			case NEEDS_WAITING:
			break;
			case DONE:
			break;
			default:
			break;
		}
		return null;
	}
	
	enum Status
	{
		FAILED,
		UNINITIALIZED,
		UNEXTENDED_WITH_POWER_SOURCE,
		UNEXTENDED_WITHOUT_POWER_SOURCE,
		EXTENDED,
		NEEDS_WAITING,
		RETRACTING,
		RETRACTED,
		STUCK,
		DONE;
	}
	
	public BlockPos getBlockPos()
	{
		return blockPos;
	}
	
	public ClientWorld getWorld()
	{
		return world;
	}
	
	public Status getStatus()
	{
		return status;
	}
	
	private void updateStatus()
	{
		if(this.tickTimes > 40)
		{
			this.status = Status.FAILED;
			return;
		}
		this.redstoneTorchBlockPos = CheckingEnvironment
			.findNearbyFlatBlockToPlaceRedstoneTorch(this.world, this.blockPos);
		if(this.redstoneTorchBlockPos == null)
		{
			this.slimeBlockPos =
				CheckingEnvironment.findPossibleSlimeBlockPos(world, blockPos);
			if(slimeBlockPos != null)
			{
				BlockPlacer.simpleBlockPlacement(slimeBlockPos,
					Blocks.NETHERRACK);
				redstoneTorchBlockPos = slimeBlockPos.up();
			}else
			{
				this.status = Status.FAILED;
				Messager.actionBar("Failed to place redstone torch.");
			}
		}else if(!this.world.getBlockState(this.blockPos).isOf(type)
			&& this.world.getBlockState(this.pistonBlockPos)
				.isOf(Blocks.PISTON))
		{
			if(!status.equals(Status.DONE))
				this.status = Status.RETRACTED;
		}else if(this.world.getBlockState(this.pistonBlockPos)
			.isOf(Blocks.PISTON)
			&& this.world.getBlockState(this.pistonBlockPos)
				.get(PistonBlock.EXTENDED))
		{
			this.status = Status.EXTENDED;
		}else if(this.world.getBlockState(this.pistonBlockPos)
			.isOf(Blocks.MOVING_PISTON))
		{
			this.status = Status.RETRACTING;
		}else if(this.world.getBlockState(this.pistonBlockPos)
			.isOf(Blocks.PISTON)
			&& !this.world.getBlockState(this.pistonBlockPos)
				.get(PistonBlock.EXTENDED)
			&& CheckingEnvironment
				.findNearbyRedstoneTorch(this.world, this.pistonBlockPos)
				.size() != 0
			&& this.world.getBlockState(this.blockPos).isOf(type))
		{
			this.status = Status.UNEXTENDED_WITH_POWER_SOURCE;
		}else if(this.hasTried
			&& this.world.getBlockState(this.pistonBlockPos).isOf(Blocks.PISTON)
			&& this.stuckTicksCounter < 15)
		{
			this.status = Status.NEEDS_WAITING;
			this.stuckTicksCounter++;
		}else if(this.world.getBlockState(this.pistonBlockPos)
			.isOf(Blocks.PISTON)
			&& this.world.getBlockState(this.pistonBlockPos)
				.get(PistonBlock.FACING) == Direction.DOWN
			&& !this.world.getBlockState(this.pistonBlockPos)
				.get(PistonBlock.EXTENDED)
			&& CheckingEnvironment
				.findNearbyRedstoneTorch(this.world, this.pistonBlockPos)
				.size() != 0
			&& this.world.getBlockState(this.blockPos).isOf(type))
		{
			this.status = Status.STUCK;
			this.hasTried = false;
			this.stuckTicksCounter = 0;
		}else if(this.world.getBlockState(this.pistonBlockPos)
			.isOf(Blocks.PISTON)
			&& !this.world.getBlockState(this.pistonBlockPos)
				.get(PistonBlock.EXTENDED)
			&& this.world.getBlockState(this.pistonBlockPos)
				.get(PistonBlock.FACING) == Direction.UP
			&& CheckingEnvironment
				.findNearbyRedstoneTorch(this.world, this.pistonBlockPos)
				.size() == 0
			&& this.world.getBlockState(this.blockPos).isOf(type))
		{
			this.status = Status.UNEXTENDED_WITHOUT_POWER_SOURCE;
		}else if(CheckingEnvironment.has2BlocksOfPlaceToPlacePiston(world,
			this.blockPos))
		{
			this.status = Status.UNINITIALIZED;
		}else if(!CheckingEnvironment.has2BlocksOfPlaceToPlacePiston(world,
			this.blockPos))
		{
			this.status = Status.FAILED;
			Messager.actionBar("Failed to place piston.");
		}else
		{
			this.status = Status.FAILED;
		}
	}
	
}

class Messager
{
	public static void actionBar(String message)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		minecraftClient.inGameHud.setOverlayMessage(Text.literal(message),
			false);
	}
	
	public static void rawchat(String message)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		Text text = Text.literal(message);
		minecraftClient.inGameHud.getChatHud().addMessage(text);
	}
}

class InventoryManager
{
	public static boolean switchToItem(ItemConvertible item)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		PlayerInventory playerInventory = minecraftClient.player.getInventory();
		
		int i = playerInventory.getSlotWithStack(new ItemStack(item));
		
		if("diamond_pickaxe".equals(item.toString()))
		{
			i = getEfficientTool(playerInventory);
		}
		
		if(i != -1)
		{
			if(PlayerInventory.isValidHotbarIndex(i))
			{
				playerInventory.selectedSlot = i;
			}else
			{
				minecraftClient.interactionManager.pickFromInventory(i);
			}
			minecraftClient.getNetworkHandler().sendPacket(
				new UpdateSelectedSlotC2SPacket(playerInventory.selectedSlot));
			return true;
		}
		return false;
	}
	
	private static int getEfficientTool(PlayerInventory playerInventory)
	{
		float best = 0;
		int bestI = -1;
		for(int i = 0; i < playerInventory.main.size(); ++i)
		{
			if(getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) > best)
			{
				best =
					getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i);
				bestI = i;
			}
			if(getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) > 45f)
			{
				return i;
			}
		}
		
		return best >= 32f ? bestI : -1;
	}
	
	public static boolean canInstantlyMinePiston()
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		PlayerInventory playerInventory = minecraftClient.player.getInventory();
		
		for(int i = 0; i < playerInventory.size(); i++)
		{
			if(getBlockBreakingSpeed(Blocks.PISTON.getDefaultState(), i) >= 32f)
			{
				return true;
			}
		}
		return false;
	}
	
	private static float getBlockBreakingSpeed(BlockState block, int slot)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		PlayerEntity player = minecraftClient.player;
		ItemStack stack = player.getInventory().getStack(slot);
		
		float f = stack.getMiningSpeedMultiplier(block);
		if(f > 1.0F)
		{
			int i = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
			ItemStack itemStack = player.getInventory().getStack(slot);
			if(i > 0 && !itemStack.isEmpty())
			{
				f += (float)(i * i + 1);
			}
		}
		
		if(StatusEffectUtil.hasHaste(player))
		{
			f *= 1.0F + (float)(StatusEffectUtil.getHasteAmplifier(player) + 1)
				* 0.2F;
		}
		
		if(player.hasStatusEffect(StatusEffects.MINING_FATIGUE))
		{
			float k;
			switch(player.getStatusEffect(StatusEffects.MINING_FATIGUE)
				.getAmplifier())
			{
				case 0:
				k = 0.3F;
				break;
				case 1:
				k = 0.09F;
				break;
				case 2:
				k = 0.0027F;
				break;
				case 3:
				default:
				k = 8.1E-4F;
			}
			
			f *= k;
		}
		
		if(player.isSubmergedIn(FluidTags.WATER)
			&& !EnchantmentHelper.hasAquaAffinity(player))
		{
			f /= 5.0F;
		}
		
		if(!player.isOnGround())
		{
			f /= 5.0F;
		}
		
		return f;
	}
	
	public static int getInventoryItemCount(ItemConvertible item)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		PlayerInventory playerInventory = minecraftClient.player.getInventory();
		return playerInventory.count(item.asItem());
	}
	
	public static String warningMessage()
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		if(!minecraftClient.interactionManager.getCurrentGameMode()
			.equals(GameMode.SURVIVAL))
		{
			return "Switch to survival mode to use the bedrock breaker.";
		}
		
		if(InventoryManager.getInventoryItemCount(Blocks.PISTON) < 2)
		{
			return "You must have at least 2 pistons.";
		}
		
		if(InventoryManager.getInventoryItemCount(Blocks.REDSTONE_TORCH) < 1)
		{
			return "You must have a redstone torch.";
		}
		
		if(InventoryManager.getInventoryItemCount(Blocks.NETHERRACK) < 1)
		{
			return "You must have a netherrack block.";
		}
		
		if(!InventoryManager.canInstantlyMinePiston())
		{
			return "You must be able to instantly mine the piston.";
		}
		return null;
	}
	
}

class CheckingEnvironment
{
	
	public static BlockPos findNearbyFlatBlockToPlaceRedstoneTorch(
		ClientWorld world, BlockPos blockPos)
	{
		if((Block.sideCoversSmallSquare(world, blockPos.east(), Direction.UP)
			&& (world.getBlockState(blockPos.east().up()).getMaterial()
				.isReplaceable())
			|| world.getBlockState(blockPos.east().up())
				.isOf(Blocks.REDSTONE_TORCH)))
		{
			return blockPos.east();
		}else if((Block.sideCoversSmallSquare(world, blockPos.west(),
			Direction.UP)
			&& (world.getBlockState(blockPos.west().up()).getMaterial()
				.isReplaceable())
			|| world.getBlockState(blockPos.west().up())
				.isOf(Blocks.REDSTONE_TORCH)))
		{
			return blockPos.west();
		}else if((Block.sideCoversSmallSquare(world, blockPos.north(),
			Direction.UP)
			&& (world.getBlockState(blockPos.north().up()).getMaterial()
				.isReplaceable())
			|| world.getBlockState(blockPos.north().up())
				.isOf(Blocks.REDSTONE_TORCH)))
		{
			return blockPos.north();
		}else if((Block.sideCoversSmallSquare(world, blockPos.south(),
			Direction.UP)
			&& (world.getBlockState(blockPos.south().up()).getMaterial()
				.isReplaceable())
			|| world.getBlockState(blockPos.south().up())
				.isOf(Blocks.REDSTONE_TORCH)))
		{
			return blockPos.south();
		}
		return null;
	}
	
	public static BlockPos findPossibleSlimeBlockPos(ClientWorld world,
		BlockPos blockPos)
	{
		if(world.getBlockState(blockPos.east()).getMaterial().isReplaceable()
			&& (world.getBlockState(blockPos.east().up()).getMaterial()
				.isReplaceable()))
		{
			return blockPos.east();
		}else if(world.getBlockState(blockPos.west()).getMaterial()
			.isReplaceable()
			&& (world.getBlockState(blockPos.west().up()).getMaterial()
				.isReplaceable()))
		{
			return blockPos.west();
		}else if(world.getBlockState(blockPos.south()).getMaterial()
			.isReplaceable()
			&& (world.getBlockState(blockPos.south().up()).getMaterial()
				.isReplaceable()))
		{
			return blockPos.south();
		}else if(world.getBlockState(blockPos.north()).getMaterial()
			.isReplaceable()
			&& (world.getBlockState(blockPos.north().up()).getMaterial()
				.isReplaceable()))
		{
			return blockPos.north();
		}
		return null;
	}
	
	public static boolean has2BlocksOfPlaceToPlacePiston(ClientWorld world,
		BlockPos blockPos)
	{
		if(world.getBlockState(blockPos.up()).getHardness(world,
			blockPos.up()) == 0)
		{
			BlockBreaker.breakBlock(world, blockPos.up());
		}
		return world.getBlockState(blockPos.up()).getMaterial().isReplaceable()
			&& world.getBlockState(blockPos.up().up()).getMaterial()
				.isReplaceable();
	}
	
	public static ArrayList<BlockPos> findNearbyRedstoneTorch(ClientWorld world,
		BlockPos pistonBlockPos)
	{
		ArrayList<BlockPos> list = new ArrayList<>();
		if(world.getBlockState(pistonBlockPos.east())
			.isOf(Blocks.REDSTONE_TORCH))
		{
			list.add(pistonBlockPos.east());
		}
		if(world.getBlockState(pistonBlockPos.west())
			.isOf(Blocks.REDSTONE_TORCH))
		{
			list.add(pistonBlockPos.west());
		}
		if(world.getBlockState(pistonBlockPos.south())
			.isOf(Blocks.REDSTONE_TORCH))
		{
			list.add(pistonBlockPos.south());
		}
		if(world.getBlockState(pistonBlockPos.north())
			.isOf(Blocks.REDSTONE_TORCH))
		{
			list.add(pistonBlockPos.north());
		}
		return list;
	}
}

class BreakingFlowController
{
	private static ArrayList<TargetBlock> cachedTargetBlockList =
		new ArrayList<>();
	
	public static boolean isWorking()
	{
		return working;
	}
	
	public static void turnOn()
	{
		working = true;
	}
	
	public static void turnOff()
	{
		working = false;
	}
	
	private static boolean working = false;
	
	public static void addBlockPosToList(BlockPos pos)
	{
		@SuppressWarnings("resource")
		ClientWorld world = MinecraftClient.getInstance().world;
		if(world.getBlockState(pos).isOf(Blocks.BEDROCK)
			|| world.getBlockState(pos).isOf(Blocks.BARRIER)
			|| world.getBlockState(pos).isOf(Blocks.END_PORTAL_FRAME)
			|| world.getBlockState(pos).isOf(Blocks.END_PORTAL)
			|| world.getBlockState(pos).isOf(Blocks.END_GATEWAY)
			|| world.getBlockState(pos).isOf(Blocks.END_PORTAL)
			|| world.getBlockState(pos).isOf(Blocks.COMMAND_BLOCK)
			|| world.getBlockState(pos).isOf(Blocks.REPEATING_COMMAND_BLOCK)
			|| world.getBlockState(pos).isOf(Blocks.CHAIN_COMMAND_BLOCK)
			|| world.getBlockState(pos).isOf(Blocks.LIGHT)
			|| world.getBlockState(pos).isOf(Blocks.STRUCTURE_BLOCK)
			|| world.getBlockState(pos).isOf(Blocks.JIGSAW))
		{
			String haveEnoughItems = InventoryManager.warningMessage();
			if(haveEnoughItems != null)
			{
				Messager.actionBar(haveEnoughItems);
				return;
			}
			
			if(shouldAddNewTargetBlock(pos))
			{
				TargetBlock targetBlock = new TargetBlock(pos, world);
				targetBlock.type = world.getBlockState(pos).getBlock();
				cachedTargetBlockList.add(targetBlock);
			}
		}else
		{
		}
	}
	
	@SuppressWarnings("resource")
	public static void tick()
	{
		if(InventoryManager.warningMessage() != null)
		{
			return;
		}
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		PlayerEntity player = minecraftClient.player;
		
		if(!minecraftClient.interactionManager.getCurrentGameMode()
			.equals(GameMode.SURVIVAL))
		{
			return;
		}
		
		for(int i = 0; i < cachedTargetBlockList.size(); i++)
		{
			TargetBlock selectedBlock = cachedTargetBlockList.get(i);
			
			if(selectedBlock.getWorld() != MinecraftClient.getInstance().world)
			{
				cachedTargetBlockList = new ArrayList<TargetBlock>();
				break;
			}
			
			if(blockInPlayerRange(selectedBlock.getBlockPos(), player, 3.4f))
			{
				TargetBlock.Status status = cachedTargetBlockList.get(i).tick();
				if(status == TargetBlock.Status.RETRACTING)
				{
					continue;
				}else if(status == TargetBlock.Status.FAILED
					|| status == TargetBlock.Status.DONE)
				{
					cachedTargetBlockList.remove(i);
				}else
				{
					break;
				}
				
			}
		}
	}
	
	private static boolean blockInPlayerRange(BlockPos blockPos,
		PlayerEntity player, float range)
	{
		return(blockPos.getSquaredDistance(
			player.getPos().add(0.5, 0.5, 0.5)) <= range * range);
	}
	
	private static boolean shouldAddNewTargetBlock(BlockPos pos)
	{
		for(int i = 0; i < cachedTargetBlockList.size(); i++)
		{
			if(cachedTargetBlockList.get(i).getBlockPos()
				.getSquaredDistance(pos.getX(), pos.getY(), pos.getZ()) == 0)
			{
				return false;
			}
		}
		return true;
	}
	
}

class BlockPlacer
{
	public static void simpleBlockPlacement(BlockPos pos, ItemConvertible item)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		
		InventoryManager.switchToItem(item);
		BlockHitResult hitResult =
			new BlockHitResult(new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
				Direction.UP, pos, false);
		// minecraftClient.interactionManager.interactBlock(minecraftClient.player,
		// minecraftClient.world, Hand.MAIN_HAND, hitResult);
		placeBlockWithoutInteractingBlock(minecraftClient, hitResult);
	}
	
	public static void pistonPlacement(BlockPos pos, Direction direction)
	{
		MinecraftClient minecraftClient = MinecraftClient.getInstance();
		double x = pos.getX();
		
		PlayerEntity player = minecraftClient.player;
		float pitch = direction.equals(Direction.DOWN) ? -90f : 90f;
		
		minecraftClient.getNetworkHandler().sendPacket(
			new PlayerMoveC2SPacket.LookAndOnGround(player.getYaw(1.0f), pitch,
				player.isOnGround()));
		
		Vec3d vec3d = new Vec3d(x, pos.getY(), pos.getZ());
		
		InventoryManager.switchToItem(Blocks.PISTON);
		BlockHitResult hitResult =
			new BlockHitResult(vec3d, Direction.UP, pos, false);
		placeBlockWithoutInteractingBlock(minecraftClient, hitResult);
	}
	
	private static void placeBlockWithoutInteractingBlock(
		MinecraftClient minecraftClient, BlockHitResult hitResult)
	{
		ClientPlayerEntity player = minecraftClient.player;
		ItemStack itemStack = player.getStackInHand(Hand.MAIN_HAND);
		((IClientPlayerInteractionManager)minecraftClient.interactionManager).sendPlayerInteractBlockPacket(Hand.MAIN_HAND, hitResult);
		// ((IClientPlayNetworkHandler)minecraftClient.getNetworkHandler())
		// .sendSequencePacket(WurstClient.MC.world,
		// i -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult,
		// i));
		
		if(!itemStack.isEmpty() && !player.getItemCooldownManager()
			.isCoolingDown(itemStack.getItem()))
		{
			ItemUsageContext itemUsageContext =
				new ItemUsageContext(player, Hand.MAIN_HAND, hitResult);
			itemStack.useOnBlock(itemUsageContext);
			
		}
	}
}

class BlockBreaker
{
	@SuppressWarnings("resource")
	public static void breakBlock(ClientWorld world, BlockPos pos)
	{
		InventoryManager.switchToItem(Items.DIAMOND_PICKAXE);
		MinecraftClient.getInstance().interactionManager.attackBlock(pos,
			Direction.UP);
	}
	
}
