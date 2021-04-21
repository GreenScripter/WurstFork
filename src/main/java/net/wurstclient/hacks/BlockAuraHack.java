package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IPersistentProjectileEntity;
import net.wurstclient.util.FakePlayerEntity;

@SearchTags({"shieldaura"})
public final class BlockAuraHack extends Hack
	implements UpdateListener, PacketOutputListener, PacketInputListener
{
	public BlockAuraHack()
	{
		super("ShieldAura",
			"Triangulates the location of the end portal based on the directions of two thrown eyes of ender.");
		setCategory(Category.RENDER);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		if(wasBlocking)
		{
			MC.getNetworkHandler().sendPacket(
				new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(
					Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
			wasBlocking = false;
		}
	}
	
	boolean wasBlocking = false;
	
	@Override
	public void onUpdate()
	{
		
		ClientPlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		double rangeSq = Math.pow(10, 2);
		Stream<Entity> stream = StreamSupport
			.stream(world.getOtherEntities(player,
				player.getBoundingBox().expand(6), null).spliterator(), true)
			.filter(e -> !e.removed)
			.filter(e -> (e instanceof LivingEntity
				&& ((LivingEntity)e).getHealth() > 0)
				|| e instanceof EndCrystalEntity || e instanceof EnderDragonPart
				|| e instanceof FireworkRocketEntity || e instanceof TntEntity)
			.filter(e -> player.squaredDistanceTo(e) <= rangeSq)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> !(e instanceof EnderDragonEntity))
			.filter(e -> !WURST.getFriends().contains(e.getEntityName()));
		
		if(true)
			stream = stream.filter(e -> !(e instanceof PlayerEntity
				&& ((PlayerEntity)e).isSleeping()));
		
		if(true)
			stream = stream.filter(
				e -> !(e instanceof AnimalEntity || e instanceof AmbientEntity
					|| e instanceof WaterCreatureEntity)
					|| e instanceof HoglinEntity);
		
		if(true)
			stream = stream.filter(e -> !(e instanceof MerchantEntity));
		
		if(true)
			stream = stream.filter(e -> !e.isInvisible());
		
		if(true)
			stream = stream.filter(e -> !(e instanceof ArmorStandEntity));
		
		Entity target =
			stream.min((e, e2) -> (int)(MC.player.squaredDistanceTo(e)
				- MC.player.squaredDistanceTo(e2))).orElse(null);
		
		if(target == null)
		{
			{
				
				List<ProjectileEntity> projectiles = new ArrayList<>();
				@SuppressWarnings("unchecked")
				Stream<ProjectileEntity> stream2 =
					(Stream<ProjectileEntity>)(Object)(StreamSupport
						.stream(MC.world.getEntities().spliterator(), true)
						.filter(e -> e instanceof ProjectileEntity)
						.filter(e -> !(e instanceof ShulkerBulletEntity))
						.filter(e -> !(e instanceof PersistentProjectileEntity)
							|| !((IPersistentProjectileEntity)e).inGround()));
				projectiles.addAll(stream2.collect(Collectors.toList()));
				for(ProjectileEntity e : projectiles)
				{
					WURST.getHax().flightPathsHack.getPath(0, e);
					if(WURST.getHax().flightPathsHack.pathHitsUs)
					{
						target = e;
					}
				}
			}
			if(target == null)
			{
				if(wasBlocking)
				{
					MC.getNetworkHandler().sendPacket(
						new net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket(
							Action.RELEASE_USE_ITEM, BlockPos.ORIGIN,
							Direction.DOWN));
					wasBlocking = false;
				}
				return;
			}
		}
		if(!MC.player.getItemCooldownManager().isCoolingDown(Items.SHIELD))
		{
			if(MC.player.inventory.getStack(40).getItem().equals(Items.SHIELD))
			{
				if(!wasBlocking)
				{
					MC.getNetworkHandler().sendPacket(
						new PlayerInteractItemC2SPacket(Hand.OFF_HAND));
					wasBlocking = true;
				}
				
				WURST.getRotationFaker()
					.faceVectorPacket(target.getBoundingBox().getCenter());
			}
		}
		
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event
			.getPacket() instanceof net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket)
		{
			PlayerActionC2SPacket p = (PlayerActionC2SPacket)event.getPacket();
			if(p.getAction().equals(Action.RELEASE_USE_ITEM))
			{
				wasBlocking = false;
			}
		}
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(event
			.getPacket() instanceof net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket)
		{
			CooldownUpdateS2CPacket packet =
				(CooldownUpdateS2CPacket)event.getPacket();
			if(Registry.ITEM.getId(packet.getItem()).toString()
				.equals("minecraft:shield"))
			{
				wasBlocking = false;
			}
		}
		if(event
			.getPacket() instanceof net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket)
		{
			EntityTrackerUpdateS2CPacket packet =
				(EntityTrackerUpdateS2CPacket)event.getPacket();
			if(packet.id() == MC.player.getEntityId())
			{
				packet.getTrackedValues().forEach(e -> {
					System.out.println(e.getData());
					if(wasBlocking)
					{
						if(e.getData().getId() == 7)
						{
							event.cancel();
						}
					}
				});
			}
		}
		
	}
	
}
