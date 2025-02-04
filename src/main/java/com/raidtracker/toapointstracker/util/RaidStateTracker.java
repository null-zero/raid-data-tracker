/*
BSD 2-Clause License

Copyright (c) 2022, LlemonDuck
Copyright (c) 2022, TheStonedTurtle
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.raidtracker.toapointstracker.util;

import com.raidtracker.toapointstracker.module.PluginLifecycleComponent;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RaidStateTracker implements PluginLifecycleComponent
{

	private static final int REGION_LOBBY = 13454;
	private static final int WIDGET_PARENT_ID = 481;
	private static final int WIDGET_CHILD_ID = 40;

	private static final RaidState DEFAULT_STATE = new RaidState(false, false, null, 0);

	private final Client client;
	private final EventBus eventBus;

	@Getter
	private RaidState currentState = DEFAULT_STATE;

	// delay inRaid = false by 3 ticks to alleviate any unexpected delays between rooms
	private int raidLeaveTicks = 0;

	@Override
	public void startUp()
	{
		eventBus.register(this);
	}

	@Override
	public void shutDown()
	{
		eventBus.unregister(this);
	}

	@Subscribe(priority = 5)
	public void onGameTick(GameTick e)
	{
		LocalPoint lp = client.getLocalPlayer().getLocalLocation();
		int region = lp == null ? -1 : WorldPoint.fromLocalInstance(client, lp).getRegionID();

		Widget w = client.getWidget(WIDGET_PARENT_ID, WIDGET_CHILD_ID);

		boolean inLobby = region == REGION_LOBBY;
		RaidRoom currentRoom = RaidRoom.forRegionId(region);
		boolean inRaidRaw = currentRoom != null || (w != null && !w.isHidden());

		raidLeaveTicks = inRaidRaw ? 3 : raidLeaveTicks - 1;
		boolean inRaid = raidLeaveTicks > 0;

		setRaidState(new RaidState(inLobby, inRaid, currentRoom, countPlayers()), false);
	}

	public int getPlayerCount()
	{
		return this.currentState.getPlayerCount();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		switch (e.getGameState())
		{
			case LOGGING_IN:
			case HOPPING:
				raidLeaveTicks = 0;
				setRaidState(DEFAULT_STATE, true);
		}
	}

	private void setRaidState(RaidState newValue, boolean forceEvent)
	{
		RaidState previous = currentState;
		currentState = newValue;

		if (forceEvent || !currentState.equals(previous))
		{
			eventBus.post(new RaidStateChanged(previous, currentState));
		}
	}

	private int countPlayers()
	{
		return 1 +
			(client.getVarbitValue(Varbits.TOA_MEMBER_1_HEALTH) != 0 ? 1 : 0) +
			(client.getVarbitValue(Varbits.TOA_MEMBER_2_HEALTH) != 0 ? 1 : 0) +
			(client.getVarbitValue(Varbits.TOA_MEMBER_3_HEALTH) != 0 ? 1 : 0) +
			(client.getVarbitValue(Varbits.TOA_MEMBER_4_HEALTH) != 0 ? 1 : 0) +
			(client.getVarbitValue(Varbits.TOA_MEMBER_5_HEALTH) != 0 ? 1 : 0) +
			(client.getVarbitValue(Varbits.TOA_MEMBER_6_HEALTH) != 0 ? 1 : 0) +
			(client.getVarbitValue(Varbits.TOA_MEMBER_7_HEALTH) != 0 ? 1 : 0);
	}
}