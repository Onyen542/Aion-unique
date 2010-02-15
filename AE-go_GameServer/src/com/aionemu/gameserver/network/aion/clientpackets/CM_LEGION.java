/*
 * This file is part of aion-unique <aion-unique.org>.
 *
 *  aion-unique is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-unique is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-unique.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.network.aion.clientpackets;

import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.aionemu.gameserver.configs.LegionConfig;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.legion.Legion;
import com.aionemu.gameserver.model.legion.LegionMember;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHANGE_NICKNAME;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CHANGE_SELF_INTRODUCTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EDIT_LEGION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEAVE_LEGION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGIONMEMBER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_CREATED;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEGION_MEMBER;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_UPDATE_LEGION_TITLE;
import com.aionemu.gameserver.services.LegionService;
import com.aionemu.gameserver.services.PlayerService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.utils.idfactory.IDFactoryAionObject;
import com.aionemu.gameserver.world.World;
import com.google.inject.Inject;

/**
 * 
 * @author Simple
 * 
 */
@SuppressWarnings("unused")
public class CM_LEGION extends AionClientPacket
{
	private static final Logger	log							= Logger.getLogger(CM_LEGION.class);

	/** Legion based information **/
	@Inject
	private LegionService		legionService;

	@Inject
	private PlayerService		playerService;

	@Inject
	private World				world;

	@Inject
	@IDFactoryAionObject
	private IDFactory			aionObjectsIDFactory;

	/**
	 * exOpcode and the rest
	 */
	private int					exOpcode;
	private int					unk1;
	private int					unk2;
	private int					legionarPermission1;
	private int					legionarPermission2;
	private int					centurionPermission1;
	private int					centurionPermission2;
	private int					rank;
	private String				legionName;
	private String				charName;
	private String				newNickname;
	private String				announcement;
	private String				newSelfIntro;

	/**
	 * Constructs new instance of CM_LEGION packet
	 * 
	 * @param opcode
	 */
	public CM_LEGION(int opcode)
	{
		super(opcode);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void readImpl()
	{
		exOpcode = readC();

		switch(exOpcode)
		{
			/** Create a legion **/
			case 0x00:
				unk2 = readD(); // time? 00 78 19 00 40
				legionName = readS();
				break;
			/** Invite to legion **/
			case 0x01:
				unk1 = readD(); // empty
				charName = readS();
				break;
			/** Leave legion **/
			case 0x02:
				unk1 = readD(); // empty
				unk2 = readH(); // empty
				break;
			/** Kick member from legion **/
			case 0x04:
				unk1 = readD(); // empty
				charName = readS();
				break;
			/** Appoint a new Brigade General **/
			case 0x05:
				unk2 = readD();
				charName = readS();
				break;
			/** Appoint Centurion **/
			case 0x06:
				rank = readD();
				charName = readS();
				break;
			/** Demote to Legionary **/
			case 0x07:
				unk2 = readD(); // char id? 00 78 19 00 40
				charName = readS();
				break;
			/** Refresh legion info **/
			case 0x08:
				break;
			/** Edit announcements **/
			case 0x09:
				unk1 = readD(); // empty or char id?
				announcement = readS();
				break;
			/** Change self introduction **/
			case 0x0A:
				unk1 = readD(); // empty char id?
				newSelfIntro = readS();
				break;
			/** Edit permissions **/
			case 0x0D:
				centurionPermission1 = readC(); // 0x60 - 0x7C
				centurionPermission2 = readC(); // 0x00 - 0x0E
				legionarPermission1 = readC(); // can't be set is static 0x40
				legionarPermission2 = readC(); // 0x00 - 0x08
				break;
			/** Level legion up **/
			case 0x0E:
				unk1 = readD(); // empty
				unk2 = readH(); // empty
				break;
			case 0x0F:
				charName = readS();
				newNickname = readS();
				break;
			default:
				log.info("Unknown Legion exOpcode? 0x" + Integer.toHexString(exOpcode).toUpperCase());
				break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void runImpl()
	{
		final Player activePlayer = getConnection().getActivePlayer();
		if(activePlayer.isLegionMember())
		{
			final Legion legion = activePlayer.getLegionMember().getLegion();
			if(charName != null)
			{
				Player targetPlayer = world.findPlayer(charName);
				switch(exOpcode)
				{
					/** Invite to legion **/
					case 0x01:
						if(targetPlayer != null)
						{
							legionService.invitePlayerToLegion(activePlayer, targetPlayer, playerService);
						}
						else
						{
							sendPacket(SM_SYSTEM_MESSAGE.LEGION_NO_USER_TO_INVITE());
						}
						break;	
						/** Kick member from legion **/
					case 0x04:
						if(targetPlayer != null)
						{
							legionService.kickPlayer(activePlayer, targetPlayer);
						}
						else
						{
							return;
							// Player off line / does not exist? send message to player?
						}
						break;
					/** Appoint a new Brigade General **/
					case 0x05:
						if(targetPlayer != null)
						{
							legionService.appointBrigadeGeneral(activePlayer, targetPlayer);
						}
						else
						{
							sendPacket(SM_SYSTEM_MESSAGE.LEGION_CHANGE_MEMBER_RANK_NO_USER());
						}
						break;
					/** Appoint Centurion/Legionairy **/
					case 0x06:
						if(targetPlayer == null)
						{
							legionService.appointRank(activePlayer, targetPlayer, rank);
						}
						else
						{
							sendPacket(SM_SYSTEM_MESSAGE.LEGION_CHANGE_MEMBER_RANK_NO_USER());
						}
						break;
					/** Set nickname **/
					case 0x0F:
						if(targetPlayer == null || targetPlayer.getLegionMember().getLegion() != legion)
							// Player offline or NOT in same legion as player
							return;
						legionService.changeNickname(activePlayer, targetPlayer, newNickname);
						break;				
				}
			}
			else
			{
				switch(exOpcode)
				{
					/** Leave legion **/
					case 0x02:
						legionService.leaveLegion(activePlayer);
						break;
						/** Refresh legion info **/
					case 0x08:
						sendPacket(new SM_LEGION_INFO(legion));
						break;
					/** Edit announcements **/
					case 0x09:
						legionService.changeAnnouncement(activePlayer, announcement);
						break;
					/** Change self introduction **/
					case 0x0A:
						legionService.changeSelfIntro(activePlayer, newSelfIntro);
						break;
					/** Edit permissions **/
					case 0x0D:
						legionService.changePermissions(legion, legionarPermission2, centurionPermission1, centurionPermission2);
						break;
					/** Level legion up **/
					case 0x0E:
						legionService.changeLevel(activePlayer, activePlayer.getInventory().getKinahItem().getItemCount());
						break;				
				}				
			}
		}
		else
		{
			switch(exOpcode)
			{
				/** Create a legion **/
				case 0x00:
					legionService.createLegion(activePlayer, legionName);
					break;
			}
		}
	}
}