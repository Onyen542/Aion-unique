/*
 * This file is part of aion-unique <aion-unique.com>.
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
package com.aionemu.gameserver.dataholders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.skillengine.model.learn.SkillClass;
import com.aionemu.gameserver.skillengine.model.learn.SkillLearnTemplate;
import com.aionemu.gameserver.skillengine.model.learn.SkillRace;

/**
 * @author ATracer
 *
 */
@XmlRootElement(name = "skill_tree")
@XmlAccessorType(XmlAccessType.FIELD)
public class SkillTreeData
{
	private static final Logger log = Logger.getLogger(SkillTreeData.class);
	
	@XmlElement(name = "skill")
	private List<SkillLearnTemplate> skillTemplates;
	
	private final Map<Integer, ArrayList<SkillLearnTemplate>> templates = new HashMap<Integer, ArrayList<SkillLearnTemplate>>();
	
	void afterUnmarshal(Unmarshaller u, Object parent)
	{
		for(SkillLearnTemplate template : skillTemplates)
		{
			addTemplate(template);
		}
		skillTemplates = null;
	}
	
	private void addTemplate(SkillLearnTemplate template)
	{
		SkillRace race = template.getRace();
		if(race == null)
			race = SkillRace.ALL;

		int hash = makeHash(template.getClassId().ordinal(), race.ordinal(), template.getMinLevel());
		ArrayList<SkillLearnTemplate> value = templates.get(hash);
		if(value == null)
		{
			value = new ArrayList<SkillLearnTemplate>();
			templates.put(hash, value);
		}
			
		value.add(template);
	}

	/**
	 * @return the templates
	 */
	public Map<Integer, ArrayList<SkillLearnTemplate>> getTemplates()
	{
		return templates;
	}
	
	public SkillLearnTemplate[] getTemplatesFor(PlayerClass playerClass, int level, Race race)
	{
		List<SkillLearnTemplate> temps = new ArrayList<SkillLearnTemplate>();
		
		List<SkillLearnTemplate> classSpecificTemplates = 
			templates.get(makeHash(playerClass.ordinal(), race.ordinal(), level));
		List<SkillLearnTemplate> generalTemplates = 
			templates.get(makeHash(SkillClass.ALL.ordinal(), SkillRace.ALL.ordinal(), level));
		
		if(classSpecificTemplates != null)
			temps.addAll(classSpecificTemplates);
		if(generalTemplates != null)
			temps.addAll(generalTemplates);
		return temps.toArray(new SkillLearnTemplate[temps.size()]);
	}

	public int size()
	{
		int size = 0;
		for(Integer key : templates.keySet())
			size += templates.get(key).size();
		return size;
	}
	
	private static int makeHash(int classId, int race, int level)
	{
		int result = classId << 8;
        result = (result | race) << 8;
        return result | level;
	}
}
