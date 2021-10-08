package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

public final class AntiPotionHack extends Hack implements UpdateListener
{
	
	public final CheckboxSetting levitation =
		new CheckboxSetting("Levitation", "Prevent levitation.", true);
	public final CheckboxSetting jumpBoost =
		new CheckboxSetting("Jump Boost", "Disable higher jumping.", false);
	public final CheckboxSetting slowFall = new CheckboxSetting("Slow Falling",
		"Disable falling slowly with slow falling.", true);
	public final CheckboxSetting dolphinsGrace = new CheckboxSetting(
		"Dolphins Grace", "Disable dolphins grace fast swimming.", false);
	public final CheckboxSetting slowness = new CheckboxSetting("Slowness",
		"Disable all slowness attributes.", false);
	public final CheckboxSetting speed = new CheckboxSetting("Speed",
		"Disable all speed attributes except sprinting.", false);
	
	public AntiPotionHack()
	{
		super("AntiPotion", "Locally disable select movement potion effects.");
		setCategory(Category.MOVEMENT);
		addSetting(levitation);
		addSetting(jumpBoost);
		addSetting(slowFall);
		addSetting(dolphinsGrace);
		addSetting(slowness);
		addSetting(speed);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(slowness.isChecked() || speed.isChecked())
		{
			ClientPlayerEntity player = MC.player;
			EntityAttributeInstance att = player.getAttributes()
				.getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
			
			Set<EntityAttributeModifier> mods = att.getModifiers();
			
			List<EntityAttributeModifier> toRemove = new ArrayList<>();
			
			for(EntityAttributeModifier m : mods)
			{
				if(m.getValue() < 0 && slowness.isChecked())
				{
					toRemove.add(m);
				}
				if(m.getValue() > 0 && speed.isChecked()
					&& !m.getName().equals("Sprinting speed boost")
					&& !m.getName().equals("Fastness"))
				{
					toRemove.add(m);
				}
			}
			toRemove.forEach(att::removeModifier);
		}
	}
	
}
