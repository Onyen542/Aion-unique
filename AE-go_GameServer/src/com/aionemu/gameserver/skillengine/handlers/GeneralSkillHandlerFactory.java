/*
 * This file is part of aion-unique <aion-unique.smfnew.com>.
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
package com.aionemu.gameserver.skillengine.handlers;

import com.aionemu.gameserver.skillengine.SkillHandler;
import com.aionemu.gameserver.skillengine.model.SkillHandlerType;

/**
 * @author ATracer
 *
 */
public class GeneralSkillHandlerFactory
{
	public static SkillHandler createSkillHandler(SkillHandlerType skillHandlerType)
	{
		switch(skillHandlerType)
		{
			case BUFF:
				return new BuffSkillHandler();
			case CREATE:
				return new CreateSkillHandler();
			case MDAM:
				return new MagDamageSkillHandler();
			case MISC:
				return new MiscSkillHandler();
			case PDAM:
				return new PhysDamageSkillHandler();
			default : 
				return new NotImplementedSkillHandler();
		}
	}
}
