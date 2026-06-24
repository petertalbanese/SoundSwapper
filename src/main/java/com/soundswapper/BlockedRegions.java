/*
 * Copyright (c) 2026, Rikten X <https://github.com/riktenx>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.soundswapper;

import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class BlockedRegions
{
	private static final List<Integer> BLOCKED_REGIONS = List.of(
		// Alchemical Hydra
		5536,
		// Vardorvis
		4405,
		// The Leviathan
		8291,
		// The Whisperer
		10595,
		// Duke Sucellus
		12132,
		// Vorkath
		9023,
		// Inferno
		9043,
		// Fight Caves
		9551,
		// Fortis Colosseum
		7216,
		// Kalphite Queen
		13972,
		// CoX
		13136, 13137, 13393, 13138, 13394, 13139, 13395, 13140, 13396, 13141, 13397, 13145, 13401, 12889,
		// ToB
		12613, 13125, 13122, 13123, 13379, 12612, 12611,
		// ToA
		14160, 15698, 15700, 14162, 14164, 15186, 15188, 14674, 14676, 15184, 15696,
		// Yama
		6045,
		// Doom of Mokhaiotl
		5269, 13668, 14180,
		// Nightmare
		15515,
		// Phantom Muspah
		11330
	);

	public static boolean inBlockedRegion(Client client)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return false;
		}

		LocalPoint lp = player.getLocalLocation();
		if (lp == null)
		{
			return false;
		}

		int id = WorldPoint.fromLocalInstance(client, lp).getRegionID();
		return BLOCKED_REGIONS.contains(id);
	}
}
