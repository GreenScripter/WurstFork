/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"SeeBarriers"})
public final class BarrierSightHack extends Hack
{
	public final CheckboxSetting opaque = new CheckboxSetting("Opaque",
		"Makes barriers completely opaque. (Toggle hack to see effect.)",
		false);
	
	public BarrierSightHack()
	{
		super("BarrierSight", "Lets you see barrier blocks.");
		setCategory(Category.BLOCKS);
		addSetting(opaque);
	}
	
	@Override
	public void onEnable()
	{
		MC.worldRenderer.reload();
	}
	
	@Override
	public void onDisable()
	{
		MC.worldRenderer.reload();
	}
}
