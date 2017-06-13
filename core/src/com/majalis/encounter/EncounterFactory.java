package com.majalis.encounter;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ObjectMap;
import com.majalis.character.PlayerCharacter;
import com.majalis.encounter.EncounterBuilder2.EncounterReader2;
import com.majalis.save.LoadService;
import com.majalis.save.SaveEnum;
import com.majalis.save.SaveManager;
import com.majalis.save.SaveManager.GameContext;
import com.majalis.save.SaveManager.GameMode;
import com.majalis.save.SaveService;
import com.majalis.scenes.ShopScene.Shop;
/*
 * Retrieves encounters from internal files given an encounterId.  Need to create some kind of encounter builder helper class.
 */
public class EncounterFactory {
	
	private final EncounterReader reader;
	private final AssetManager assetManager;
	private final SaveService saveService;
	private final LoadService loadService;

	public EncounterFactory(EncounterReader reader, AssetManager assetManager, SaveManager saveManager) {
		this.reader = reader;
		this.assetManager = assetManager;
		this.saveService = saveManager;
		this.loadService = saveManager;
	}
	
	@SuppressWarnings("unchecked")
	public Encounter getEncounter(EncounterCode encounterCode, BitmapFont font) {
		Integer sceneCode = loadService.loadDataValue(SaveEnum.SCENE_CODE, Integer.class);
		GameContext context = loadService.loadDataValue(SaveEnum.RETURN_CONTEXT, GameContext.class);
		EncounterBuilder builder = new EncounterBuilder(reader, assetManager, saveService, font, sceneCode, (ObjectMap<String, Shop>)loadService.loadDataValue(SaveEnum.SHOP, Shop.class), (PlayerCharacter) loadService.loadDataValue(SaveEnum.PLAYER, PlayerCharacter.class), context);
		switch (encounterCode) {
			default: return
					encounterCode == EncounterCode.WERESLUT || encounterCode == EncounterCode.HARPY || encounterCode == EncounterCode.SLIME || encounterCode == EncounterCode.BRIGAND || encounterCode == EncounterCode.DRYAD || encounterCode == EncounterCode.CENTAUR || encounterCode == EncounterCode.GOBLIN
					|| encounterCode == EncounterCode.ORC || encounterCode == EncounterCode.ADVENTURER || encounterCode == EncounterCode.OGRE || encounterCode == EncounterCode.BEASTMISTRESS || encounterCode == EncounterCode.GADGETEER || encounterCode == EncounterCode.INN
					|| encounterCode == EncounterCode.TOWN_CRIER || encounterCode == EncounterCode.CRIER_QUEST || encounterCode == EncounterCode.INITIAL || encounterCode == EncounterCode.COTTAGE_TRAINER || encounterCode == EncounterCode.COTTAGE_TRAINER_VISIT || encounterCode == EncounterCode.LEVEL_UP 
					|| encounterCode == EncounterCode.DEFAULT || encounterCode == EncounterCode.TOWN_STORY || encounterCode == EncounterCode.MERI_COTTAGE || encounterCode == EncounterCode.MERI_COTTAGE_VISIT || encounterCode == EncounterCode.FIRST_BATTLE_STORY
					|| encounterCode == EncounterCode.OGRE_WARNING_STORY || encounterCode == EncounterCode.OGRE_STORY || encounterCode == EncounterCode.ECCENTRIC_MERCHANT || encounterCode == EncounterCode.STORY_FEM || encounterCode == EncounterCode.STORY_SIGN || encounterCode == EncounterCode.WEST_PASS || encounterCode == EncounterCode.SOUTH_PASS 	
					//||
					? 
					 new EncounterBuilder2(new EncounterReader2("script/encounters2.json"), assetManager, saveService, font, sceneCode == 0 ? -1 : sceneCode, (ObjectMap<String, Shop>)loadService.loadDataValue(SaveEnum.SHOP, Shop.class), (PlayerCharacter) loadService.loadDataValue(SaveEnum.PLAYER, PlayerCharacter.class), context, (GameMode) loadService.loadDataValue(SaveEnum.MODE, GameMode.class)).getEncounter(encounterCode) : 
					builder.getRandomEncounter(encounterCode);
		}
	}
}
