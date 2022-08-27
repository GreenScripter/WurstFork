/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EmptyBlockView;
import net.wurstclient.Category;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockBreaker;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;

public final class NukerHack extends Hack
	implements UpdateListener, LeftClickListener, RenderListener
{
	private final SliderSetting range =
		new SliderSetting("Range", 5, 1, 6, 0.05, ValueDisplay.DECIMAL);

	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lNormal\u00a7r mode simply breaks everything\n" + "around you.\n"
			+ "\u00a7lID\u00a7r mode only breaks the selected block\n"
			+ "type. Left-click on a block to select it.\n"
			+ "\u00a7lMultiID\u00a7r mode only breaks the block types\n"
			+ "in your MultiID List.\n"
			+ "\u00a7lFlat\u00a7r mode flattens the area around you,\n"
			+ "but won't dig down.\n"
			+ "\u00a7lSmash\u00a7r mode only breaks blocks that\n"
			+ "can be destroyed instantly (e.g. tall grass).",
		Mode.values(), Mode.NORMAL);

	private final BlockSetting id =
		new BlockSetting("ID", "The type of block to break in ID mode.\n"
			+ "air = won't break anything", "minecraft:air", true);

	private final CheckboxSetting lockId =
		new CheckboxSetting("Lock ID", "Prevents changing the ID by clicking\n"
			+ "on blocks or restarting Nuker.", false);

	private final BlockListSetting multiIdList = new BlockListSetting(
		"MultiID List", "The types of blocks to break in MultiID mode.",
		"minecraft:ancient_debris", "minecraft:bone_block", "minecraft:clay",
		"minecraft:coal_ore", "minecraft:diamond_ore", "minecraft:emerald_ore",
		"minecraft:glowstone", "minecraft:gold_ore", "minecraft:iron_ore",
		"minecraft:lapis_ore", "minecraft:nether_gold_ore",
		"minecraft:nether_quartz_ore", "minecraft:redstone_ore");

	private final ArrayDeque<Set<BlockPos>> prevBlocks = new ArrayDeque<>();
	private BlockPos currentBlock;
	private float progress;
	private float prevProgress;

	public NukerHack()
	{
		super("Nuker");
		setCategory(Category.BLOCKS);
		addSetting(range);
		addSetting(mode);
		addSetting(id);
		addSetting(lockId);
		addSetting(multiIdList);
	}

	@Override
	public String getRenderName()
	{
		return mode.getSelected().getRenderName(this);
	}

	@Override
	protected void onEnable()
	{
		WURST.getHax().autoMineHack.setEnabled(false);
		WURST.getHax().excavatorHack.setEnabled(false);
		WURST.getHax().nukerLegitHack.setEnabled(false);
		WURST.getHax().speedNukerHack.setEnabled(false);
		WURST.getHax().tunnellerHack.setEnabled(false);

		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(LeftClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}

	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(LeftClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);

		if(currentBlock != null)
		{
			IMC.getInteractionManager().setBreakingBlock(true);
			MC.interactionManager.cancelBlockBreaking();
			currentBlock = null;
		}

		prevBlocks.clear();

		if(!lockId.isChecked())
			id.setBlock(Blocks.AIR);
	}

	@Override
	public void onUpdate()
	{
		// abort if using IDNuker without an ID being set
		if(mode.getSelected() == Mode.ID && id.getBlock() == Blocks.AIR)
			return;

		ClientPlayerEntity player = MC.player;

		currentBlock = null;
		Vec3d eyesPos = RotationUtils.getEyesPos().subtract(0.5, 0.5, 0.5);
		BlockPos eyesBlock = new BlockPos(RotationUtils.getEyesPos());
		double rangeSq = Math.pow(range.getValue(), 2);
		int blockRange = (int)Math.ceil(range.getValue());

		Vec3i rangeVec = new Vec3i(blockRange, blockRange, blockRange);
		BlockPos min = eyesBlock.subtract(rangeVec);
		BlockPos max = eyesBlock.add(rangeVec);

		ArrayList<BlockPos> blocks = BlockUtils.getAllInBox(min, max);
		Stream<BlockPos> stream = blocks.parallelStream();
		List<BlockPos> blocks2 = stream
			.filter(pos -> eyesPos.squaredDistanceTo(Vec3d.of(pos)) <= rangeSq)
			.filter(pos -> (BlockUtils.canBeClicked(pos) || (mode.getSelected().equals(Mode.ID) && id.getBlockName().contains("lava"))))
			.filter(mode.getSelected().getValidator(this))
			.sorted(Comparator.comparingDouble(
				pos -> eyesPos.squaredDistanceTo(Vec3d.of(pos))))
			.collect(Collectors.toList());
		if(player.getAbilities().creativeMode && !(mode.getSelected().equals(Mode.ID) && id.getBlockName().contains("lava")))
		{
			Stream<BlockPos> stream2 = blocks2.parallelStream();
			for(Set<BlockPos> set : prevBlocks)
				stream2 = stream2.filter(pos -> !set.contains(pos));
			List<BlockPos> blocks3 = stream2.collect(Collectors.toList());

			prevBlocks.addLast(new HashSet<>(blocks3));
			while(prevBlocks.size() > 5)
				prevBlocks.removeFirst();

			if(!blocks3.isEmpty())
				currentBlock = blocks3.get(0);

			MC.interactionManager.cancelBlockBreaking();
			progress = 1;
			prevProgress = 1;
			BlockBreaker.breakBlocksWithPacketSpam(blocks3);
			return;
		}

		for(BlockPos pos : blocks2) {
			if (mode.getSelected().equals(Mode.ID) && id.getBlockName().contains("lava")) {
				placeBlockFromHotbar(pos);
				return;
			} else if(BlockBreaker.breakOneBlock(pos))
			{
				currentBlock = pos;
				break;
			}
		}
		if (mode.getSelected().equals(Mode.ID) && id.getBlockName().contains("lava")){
			return;
		}

		if(currentBlock == null)
			MC.interactionManager.cancelBlockBreaking();

		if(currentBlock != null && BlockUtils.getHardness(currentBlock) < 1)
		{
			prevProgress = progress;
			progress = IMC.getInteractionManager().getCurrentBreakingProgress();

			if(progress < prevProgress)
				prevProgress = progress;

		}else
		{
			progress = 1;
			prevProgress = 1;
		}
	}

	private void placeBlockFromHotbar(BlockPos pos) {
		// search blocks in hotbar
		int newSlot = -1;
		for (int i = 0; i < 9; i++) {
			// filter out non-block items
			ItemStack stack = MC.player.getInventory().getStack(i);
			if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;



			// filter out non-solid blocks
			Block block = Block.getBlockFromItem(stack.getItem());

			BlockState state = block.getDefaultState();

			if (state.getMaterial().equals(Material.SHULKER_BOX)) {
				continue;
			}

			if (!state.isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) continue;

			// filter out blocks that would fall
			if (block instanceof FallingBlock) continue;

			newSlot = i;
			break;
		}

		// check if any blocks were found
		if (newSlot == -1) return;

		// set slot
		int oldSlot = MC.player.getInventory().selectedSlot;
		MC.player.getInventory().selectedSlot = newSlot;
		placeBlock(pos);

		// reset slot
		MC.player.getInventory().selectedSlot = oldSlot;
	}

	private boolean placeBlock(BlockPos pos)
	{
		Vec3d eyesPos = new Vec3d(MC.player.getX(),
			MC.player.getY() + MC.player.getEyeHeight(MC.player.getPose()),
			MC.player.getZ());

		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			Direction side2 = side.getOpposite();

			// check if side is visible (facing away from player)
			if(eyesPos.squaredDistanceTo(Vec3d.ofCenter(pos)) >= eyesPos
				.squaredDistanceTo(Vec3d.ofCenter(neighbor)))
				continue;

			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;

			Vec3d hitVec = Vec3d.ofCenter(neighbor)
				.add(Vec3d.of(side2.getVector()).multiply(0.5));

			// check if hitVec is within range (4.25 blocks)
			if(eyesPos.squaredDistanceTo(hitVec) > 18.0625)
				continue;

			// place block
			Rotation rotation = RotationUtils.getNeededRotations(hitVec);
			PlayerMoveC2SPacket.LookAndOnGround packet =
				new PlayerMoveC2SPacket.LookAndOnGround(rotation.getYaw(),
					rotation.getPitch(), MC.player.isOnGround());
			MC.player.networkHandler.sendPacket(packet);
			IMC.getInteractionManager().rightClickBlock(neighbor, side2,
				hitVec);
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);

			return true;
		}

		return false;
	}

	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(mode.getSelected() != Mode.ID)
			return;

		if(lockId.isChecked())
			return;

		if(MC.crosshairTarget == null
			|| MC.crosshairTarget.getType() != HitResult.Type.BLOCK)
			return;

		BlockHitResult blockHitResult = (BlockHitResult)MC.crosshairTarget;
		BlockPos pos = new BlockPos(blockHitResult.getBlockPos());
		id.setBlockName(BlockUtils.getName(pos));
	}

	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(currentBlock == null)
			return;

		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);

		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);

		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;

		Box box = new Box(BlockPos.ORIGIN);
		float p = prevProgress + (progress - prevProgress) * partialTicks;
		float red = p * 2F;
		float green = 2 - red;

		matrixStack.translate(currentBlock.getX() - regionX,
			currentBlock.getY(), currentBlock.getZ() - regionZ);
		if(p < 1)
		{
			matrixStack.translate(0.5, 0.5, 0.5);
			matrixStack.scale(p, p, p);
			matrixStack.translate(-0.5, -0.5, -0.5);
		}

		RenderSystem.setShader(GameRenderer::getPositionShader);

		RenderSystem.setShaderColor(red, green, 0, 0.25F);
		RenderUtils.drawSolidBox(box, matrixStack);

		RenderSystem.setShaderColor(red, green, 0, 0.5F);
		RenderUtils.drawOutlinedBox(box, matrixStack);

		matrixStack.pop();

		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);

	}

	private enum Mode
	{
		NORMAL("Normal", NukerHack::getName, (n, p) -> true),

		ID("ID",
			n -> "IDNuker [" + n.id.getBlockName().replace("minecraft:", "")
				+ "]",
			(n, p) -> BlockUtils.getName(p).equals(n.id.getBlockName())),

		MULTI_ID("MultiID",
			n -> "MultiIDNuker [" + n.multiIdList.getBlockNames().size()
				+ (n.multiIdList.getBlockNames().size() == 1 ? " ID]"
					: " IDs]"),
			(n, p) -> n.multiIdList.getBlockNames()
				.contains(BlockUtils.getName(p))),

		FLAT("Flat", n -> "FlatNuker",
			(n, p) -> p.getY() >= MC.player.getPos().getY()),

		SMASH("Smash", n -> "SmashNuker",
			(n, p) -> BlockUtils.getHardness(p) >= 1);

		private final String name;
		private final Function<NukerHack, String> renderName;
		private final BiPredicate<NukerHack, BlockPos> validator;

		private Mode(String name, Function<NukerHack, String> renderName,
			BiPredicate<NukerHack, BlockPos> validator)
		{
			this.name = name;
			this.renderName = renderName;
			this.validator = validator;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public String getRenderName(NukerHack n)
		{
			return renderName.apply(n);
		}

		public Predicate<BlockPos> getValidator(NukerHack n)
		{
			return p -> validator.test(n, p);
		}
	}
}
