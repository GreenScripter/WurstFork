package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

public final class AntiPotionHack extends Hack
{	
	
	public final CheckboxSetting levitation = new CheckboxSetting("Levitation", "Prevent levitation.", true);
	public final CheckboxSetting jumpBoost = new CheckboxSetting("Jump Boost", "Disable higher jumping.", false);
	public final CheckboxSetting slowFall = new CheckboxSetting("Slow Falling", "Disable falling slowly with slow falling.", true);
	public final CheckboxSetting dolphinsGrace = new CheckboxSetting("Dolphins Grace", "Disable dolphins grace fast swimming.", false);

	public AntiPotionHack()
	{
		super("AntiPotion", "Locally disable select movement potion effects.");
		setCategory(Category.MOVEMENT);
		addSetting(levitation);
		addSetting(jumpBoost);
		addSetting(slowFall);
		addSetting(dolphinsGrace);
	}
	
}
