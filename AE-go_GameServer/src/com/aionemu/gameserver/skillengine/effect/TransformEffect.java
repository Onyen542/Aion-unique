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
package com.aionemu.gameserver.skillengine.effect;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author Sweetkr
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransformEffect")
public class TransformEffect extends EffectTemplate
{
	/** duration is in seconds **/
	@XmlAttribute(required = true)
    protected int duration;
	@XmlAttribute
	protected int model;

	@Override
	public void endEffect(Effect effect)
	{
		Creature effected = effect.getEffected();
		effected.getEffectController().unsetAbnormal(EffectId.TRANSFORM.getEffectId());

		if(effected instanceof Npc)
		{
			effected.setTransformedModelId(effected.getObjectTemplate().getTemplateId());
		}
		else if(effected instanceof Player)
		{
			effected.setTransformedModelId(0);
			//PacketSendUtility.sendPacket((Player)effected, new SM_PLAYER_INFO((Player)effected, false));
		}
		PacketSendUtility.broadcastPacket(effected, new SM_TRANSFORM(effected));
	}

	@Override
	public void startEffect(final Effect effect)
	{
		final Creature effected = effect.getEffected();
		effected.getEffectController().setAbnormal(EffectId.TRANSFORM.getEffectId());
		effected.setTransformedModelId(model);	
		PacketSendUtility.broadcastPacket(effected, new SM_TRANSFORM(effected));
		if(effected instanceof Player)
		{
			//PacketSendUtility.sendPacket((Player)effected, new SM_PLAYER_INFO((Player)effected, false));
		}
	}

	@Override
	public void onPeriodicAction(Effect effect)
	{
		// TODO Auto-generated method stub
	}
}