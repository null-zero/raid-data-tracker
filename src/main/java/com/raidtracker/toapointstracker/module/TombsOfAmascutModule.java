package com.raidtracker.toapointstracker.module;

import com.raidtracker.RaidTrackerConfig;
import com.raidtracker.toapointstracker.pointstracker.PointsTracker;
import com.raidtracker.toapointstracker.util.RaidStateTracker;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

@Slf4j
public class TombsOfAmascutModule extends AbstractModule
{

	@Override
	protected void configure()
	{
		Multibinder<PluginLifecycleComponent> lifecycleComponents = Multibinder.newSetBinder(binder(), PluginLifecycleComponent.class);
		lifecycleComponents.addBinding().to(PointsTracker.class);
		lifecycleComponents.addBinding().to(RaidStateTracker.class);
	}

}
