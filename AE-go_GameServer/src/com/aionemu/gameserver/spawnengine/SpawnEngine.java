/*
 * This file is part of aion-emu <aion-emu.com>.
 *
 *  aion-emu is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-emu is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.spawnengine;

import org.apache.log4j.Logger;

import com.aionemu.gameserver.controllers.ActionitemController;
import com.aionemu.gameserver.controllers.BindpointController;
import com.aionemu.gameserver.controllers.CitizenController;
import com.aionemu.gameserver.controllers.EffectController;
import com.aionemu.gameserver.controllers.GatherableController;
import com.aionemu.gameserver.controllers.MonsterController;
import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.PostboxController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.GatherableData;
import com.aionemu.gameserver.dataholders.NpcData;
import com.aionemu.gameserver.dataholders.SpawnsData;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Citizen;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Monster;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.templates.GatherableTemplate;
import com.aionemu.gameserver.model.templates.NpcTemplate;
import com.aionemu.gameserver.model.templates.SpawnTemplate;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.spawn.SpawnGroup;
import com.aionemu.gameserver.services.DropService;
import com.aionemu.gameserver.services.ItemService;
import com.aionemu.gameserver.utils.idfactory.IDFactory;
import com.aionemu.gameserver.utils.idfactory.IDFactoryAionObject;
import com.aionemu.gameserver.world.KnownList;
import com.aionemu.gameserver.world.World;
import com.google.inject.Inject;

/**
 * 
 * This class is responsible for NPCs spawn management. Current implementation is temporal and will be replaced in the
 * future.
 * 
 * @author Luno
 * 
 * modified by ATracer
 * 
 */
public class SpawnEngine
{
	private static Logger log = Logger.getLogger(SpawnEngine.class);
		
	@Inject
	private World	world;
	@IDFactoryAionObject
	@Inject
	private IDFactory aionObjectsIDFactory;
	@Inject
	private DropService dropService;
	@Inject
	private ItemService itemService;
	@Inject
	private SpawnsData spawns;
	@Inject
	private GatherableData gatherableData;
	@Inject
	private NpcData npcData;
	
	/** Counter counting number of npc spawns */
	private int npcCounter		= 0;
	/** Counter counting number of gatherable spawns */
	private int gatherableCounter		= 0;
	
	
	/**
	 * Creates VisibleObject instance and spawns it using given {@link SpawnTemplate} instance.
	 * 
	 * @param spawn
	 * @return created and spawned VisibleObject
	 */
	public VisibleObject spawnObject(SpawnTemplate spawn)
	{
		VisibleObjectTemplate template = null;
		int objectId = spawn.getSpawnGroup().getNpcid();
		
		if(objectId > 400000 && objectId < 499999)// gatherable
		{
			template = gatherableData.getGatherableTemplate(objectId);
			if(template == null)
				return null;
			gatherableCounter++;
		}
		else // npc
		{
			template = npcData.getNpcTemplate(objectId);
			if(template == null)
				return null;
			npcCounter++;
		}
		spawn.setObjectTemplate(template);
		spawn.setSpawned(true);
		if(template instanceof NpcTemplate)
		{
			NpcType npcType = ((NpcTemplate)template).getNpcType();	
			Npc npc = null;
			
			switch(npcType)
			{
				case AGGRESSIVE:
				case ATTACKABLE:
					MonsterController mosnterController = new MonsterController();
					mosnterController.setDropService(dropService);
					npc = new Monster(aionObjectsIDFactory.nextId(), mosnterController, spawn);
					break;
				case NON_ATTACKABLE:
					npc = new Citizen(aionObjectsIDFactory.nextId(), new CitizenController(), spawn);
					break;
				case POSTBOX:
					npc = new Npc(aionObjectsIDFactory.nextId(), new PostboxController(), spawn);
					break;
				case RESURRECT:
					BindpointController bindPointController = new BindpointController();
					bindPointController.setBindPointTemplate(DataManager.BIND_POINT_DATA.getBindPointTemplate(spawn.getObjectTemplate().getTemplateId()));
					npc = new Npc(aionObjectsIDFactory.nextId(), bindPointController, spawn);
					break;
				case USEITEM:
					ActionitemController  actionitemController = new ActionitemController();
					actionitemController.setDropService(dropService);
					npc = new Npc(aionObjectsIDFactory.nextId(), actionitemController, spawn);
					break;
				default: 
					npc = new Npc(aionObjectsIDFactory.nextId(), new NpcController(), spawn);
						
			}

			npc.setKnownlist(new KnownList(npc));
			npc.setEffectController(new EffectController(npc));
			npc.getController().onRespawn();
			bringIntoWorld(npc, spawn);
			return npc;
		}
		else if(spawn.getObjectTemplate() instanceof GatherableTemplate)
		{
			GatherableController gatherableController = new GatherableController();
			gatherableController.setItemService(itemService);
			Gatherable gatherable = new Gatherable(spawn, aionObjectsIDFactory.nextId(), gatherableController);
			gatherable.setKnownlist(new KnownList(gatherable));
			bringIntoWorld(gatherable, spawn);
			return gatherable;
		}
		return null;
	}

	/**
	 * 
	 * @param worldId
	 * @param objectId
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param walkerid
	 * @param randomwalk
	 * @return
	 */
	public SpawnTemplate addNewSpawn(int worldId, int objectId, float x, float y, float z, byte heading, int walkerid, int randomwalk)
	{
		VisibleObjectTemplate template = null;
		if(objectId > 400000 && objectId < 499999)// gatherable
		{
			template = gatherableData.getGatherableTemplate(objectId);
			if(template == null)
				return null;
			gatherableCounter++;
		}
		else // npc
		{
			template = npcData.getNpcTemplate(objectId);
			if(template == null)
				return null;
			npcCounter++;
		}
		
		SpawnTemplate spawnTemplate = new SpawnTemplate(template, x, y, z, heading, walkerid, randomwalk);		
		spawns.addNewTemplate(spawnTemplate, worldId);
		
		return spawnTemplate;
	}
	
	private void bringIntoWorld(VisibleObject visibleObject, SpawnTemplate spawn)
	{
		world.storeObject(visibleObject);
		world.setPosition(visibleObject, spawn.getWorldId(), spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getHeading());
		world.spawn(visibleObject);
	}
	
	public void spawnAll()
	{
		for(SpawnGroup spawnGroup : spawns)
		{
			int pool = spawnGroup.getPool();
			for(int i = 0; i < pool; i++)
			{
				spawnObject(spawnGroup.getNextAvailableTemplate());
			}
		}
		
		log.info("Loaded " + npcCounter + " npc spawns");
		log.info("Loaded " + gatherableCounter + " gatherable spawns");
	}

	/**
	 * 
	 */
	public void clearAll()
	{
		spawns.clear();
	}

}
