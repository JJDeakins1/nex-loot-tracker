package com.nexloottracker;

import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.gameval.NpcID;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NexKillContributionTrackerTest
{
	@Mock
	private Client client;

	@Mock
	private Player localPlayer;

	@Test
	public void getContributionPercentUsesLocalOverTotalDamage()
	{
		NexKillContributionTracker tracker = new NexKillContributionTracker();
		tracker.onHitsplatApplied(hitsplat(NpcID.NEX, 80, true, false), client, Collections.singleton(NpcID.NEX));
		tracker.onHitsplatApplied(hitsplat(NpcID.NEX, 20, false, true), client, Collections.singleton(NpcID.NEX));

		assertEquals(80.0, tracker.getContributionPercent(), 0.001);
	}

	@Test
	public void ignoresNonNexNpcHits()
	{
		NexKillContributionTracker tracker = new NexKillContributionTracker();
		tracker.onHitsplatApplied(hitsplat(NpcID.CORP_BEAST, 100, true, false), client, Collections.singleton(NpcID.NEX));

		assertNull(tracker.getContributionPercent());
	}

	@Test
	public void resetClearsTrackedDamage()
	{
		NexKillContributionTracker tracker = new NexKillContributionTracker();
		tracker.onHitsplatApplied(hitsplat(NpcID.NEX, 50, true, false), client, Collections.singleton(NpcID.NEX));
		tracker.reset();

		assertNull(tracker.getContributionPercent());
	}

	private HitsplatApplied hitsplat(int npcId, int amount, boolean mine, boolean others)
	{
		NPC npc = mock(NPC.class);
		when(npc.getId()).thenReturn(npcId);

		Hitsplat hitsplat = mock(Hitsplat.class);
		when(hitsplat.getAmount()).thenReturn(amount);
		when(hitsplat.isMine()).thenReturn(mine);
		when(hitsplat.isOthers()).thenReturn(others);

		HitsplatApplied event = mock(HitsplatApplied.class);
		when(event.getActor()).thenReturn(npc);
		when(event.getHitsplat()).thenReturn(hitsplat);
		when(client.getLocalPlayer()).thenReturn(localPlayer);

		return event;
	}
}
