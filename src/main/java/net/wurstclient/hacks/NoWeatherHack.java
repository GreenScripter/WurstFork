/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

public final class NoWeatherHack extends Hack
{
	private final EnumSetting<Weather> weather = new EnumSetting<>("Weather",
		"Determines what the weather should be.\n"
			+ "\u00a7lNo Change\u00a7r - Weather remains the same.\n"
			+ "\u00a7lClear\u00a7r - Weather stays clear.\n"
			+ "\u00a7lRain\u00a7r - Weather stays stormy.",
			Weather.values(), Weather.CLEAR);
	
	private final CheckboxSetting changeTime =
		new CheckboxSetting("Change World Time", false);
	
	private final SliderSetting time =
		new SliderSetting("Time", 6000, 0, 23900, 100, ValueDisplay.INTEGER);
	
	private final CheckboxSetting changeMoonPhase =
		new CheckboxSetting("Change Moon Phase", false);
	
	private final SliderSetting moonPhase =
		new SliderSetting("Moon Phase", 0, 0, 7, 1, ValueDisplay.INTEGER);
	
	public NoWeatherHack()
	{
		super("NoWeather");
		setCategory(Category.RENDER);
		
		addSetting(weather);
		addSetting(changeTime);
		addSetting(time);
		addSetting(changeMoonPhase);
		addSetting(moonPhase);
	}
	
	public boolean isRainDisabled()
	{
		return isEnabled() && weather.getSelected().equals(Weather.CLEAR);
	}
	
	public boolean isRainForced()
	{
		return isEnabled() && weather.getSelected().equals(Weather.RAIN);
	}
	
	public boolean isTimeChanged()
	{
		return isEnabled() && changeTime.isChecked();
	}
	
	public long getChangedTime()
	{
		return time.getValueI();
	}
	
	public boolean isMoonPhaseChanged()
	{
		return isEnabled() && changeMoonPhase.isChecked();
	}
	
	public int getChangedMoonPhase()
	{
		return moonPhase.getValueI();
	}
	
	static enum Weather {
		NO_CHANGE,CLEAR,RAIN
	}
}
