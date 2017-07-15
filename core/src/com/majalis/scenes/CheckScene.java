package com.majalis.scenes;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.OrderedMap;
import com.majalis.character.PlayerCharacter;
import com.majalis.character.EnemyEnum;
import com.majalis.character.AbstractCharacter.Stat;
import com.majalis.character.PlayerCharacter.QuestType;
import com.majalis.character.Perk;
import com.majalis.encounter.Background;
import com.majalis.save.SaveService;
import com.majalis.screens.TimeOfDay;

public class CheckScene extends AbstractTextScene {

	private final PlayerCharacter character;
	private final Scene defaultScene;
	private Stat statToCheck;
	private Perk perkToCheck;
	private OrderedMap<Integer, Scene> checkValues;
	private CheckType checkType;
	private Scene clearScene;
	private Scene nextScene;
	private Background background;
	
	public CheckScene(OrderedMap<Integer, Scene> sceneBranches, int sceneCode, AssetManager assetManager, SaveService saveService, BitmapFont font, Background background, Stat stat, OrderedMap<Integer, Scene> checkValues, Scene defaultScene, PlayerCharacter character) {
		this(sceneBranches, sceneCode, assetManager, saveService, font, background, stat, null, null, checkValues, null, defaultScene, character);
	}
	
	public CheckScene(OrderedMap<Integer, Scene> sceneBranches, int sceneCode, AssetManager assetManager, SaveService saveService, BitmapFont font, Background background, Perk perk, OrderedMap<Integer, Scene> checkValues, Scene defaultScene, PlayerCharacter character) {
		this(sceneBranches, sceneCode, assetManager, saveService, font, background, null, perk, null, checkValues, null, defaultScene, character);
	}
	
	public CheckScene(OrderedMap<Integer, Scene> sceneBranches, int sceneCode, AssetManager assetManager, SaveService saveService, BitmapFont font, Background background, CheckType checkType, Scene clearScene, Scene defaultScene, PlayerCharacter character) {
		this(sceneBranches, sceneCode, assetManager, saveService, font, background, null, null, checkType, null, clearScene, defaultScene, character);
	}
	
	public CheckScene(OrderedMap<Integer, Scene> sceneBranches, int sceneCode, AssetManager assetManager, SaveService saveService, BitmapFont font, Background background, Stat stat, Perk perk, CheckType checkType, OrderedMap<Integer, Scene> checkValues, Scene clearScene, Scene defaultScene, PlayerCharacter character) {
		super(sceneBranches, sceneCode, assetManager, font, character, saveService, background);
		this.checkValues = checkValues;
		this.clearScene = clearScene;
		this.defaultScene = defaultScene;
		this.character = character;
		this.statToCheck = stat;
		this.perkToCheck = perk;
		this.checkType = checkType;
		this.background = background;
	}
	
	@Override
	public void setActive() {
		super.setActive();
		nextScene = getNextScene();	
		background.setColor(TimeOfDay.getTime(character.getTime()).getColor());
	}
	
	private Scene getNextScene() {
		String passValue = "PASSED!\n";
		String failValue = "FAILURE!\n";
		String toDisplay = "";
		if (checkType != null) {
			if (checkType.getCheck(character)) {
				toDisplay += checkType.getSuccess();
				display.setText(toDisplay);
				return clearScene;
			}
			else {
				toDisplay += checkType.getFailure();
				display.setText(toDisplay);
				return defaultScene;
			}			
		}
		else if (perkToCheck != null) {
			int amount = character.getPerks().get(perkToCheck, 0);
			toDisplay += "Your " + perkToCheck.getLabel() + " score: " + amount + "\n\n";
			for (Integer threshold : checkValues.keys()) {
				if (threshold == 0) break;
				toDisplay += perkToCheck.getLabel() + " check (" + threshold + "): ";
				if (amount >= threshold) {
					toDisplay += perkToCheck.isPositive() ? passValue : failValue;
					display.setText(toDisplay);
					return checkValues.get(threshold);
				}
				else {
					toDisplay += perkToCheck.isPositive() ? failValue : passValue;
				}
			}
			display.setText(toDisplay);
			return checkValues.get(0);
		}
		else {
			int amount = statToCheck == Stat.CHARISMA ? character.getLewdCharisma() : character.getRawStat(statToCheck);
			int baseAmount = character.getBaseStat(statToCheck);
			toDisplay += "Your current " + statToCheck.toString() + " score: " + amount + " (Base: " + baseAmount + ")" + "\n" + character.getStatPenaltyDisplay() + "\n\n";
			for (Integer threshold : checkValues.keys()) {
				if (threshold == 0) break;
				toDisplay += statToCheck.toString() + " check (" + threshold + "): ";
				if (amount >= threshold) {
					toDisplay += passValue;
					display.setText(toDisplay);
					return checkValues.get(threshold);
				}
				else {
					toDisplay += failValue;
				}
			}
			display.setText(toDisplay);
			display.addAction(Actions.moveBy(0, 200));
			return checkValues.get(0);
		}
	}
	
	@Override
	protected void nextScene() {
		nextScene.setActive();
		isActive = false;
		addAction(Actions.hide());	
	}
	
	public enum CheckType {
		VIRGIN ("Are you an anal virgin? PASSED!", "Are you an anal virgin? FAILURE!") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.isVirgin(); } 
		},
		GOBLIN_VIRGIN ("You've never had goblin cock up the ass before. (PASSED)", "You've had goblin cock up the ass before. (FAILURE)") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.isVirgin(EnemyEnum.GOBLIN); } 
		}, 
		GOBLIN_KNOWN (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.GOBLIN) == 0; } 
		}, 
		ORC_ENCOUNTERED ("You come across a bizarre sight.", "You see her, again.  The orc.") {
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.ORC) == 0; } 
		},
		ORC_COWARD ("You confidently approach her.", "You recall your cowardice.") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.ORC) == 1; } 
		}, 
		CRIER (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.CRIER) == 0; }  
		}, 
		CRIER_QUEST (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.CRIER) == 1; }  
		}, 
		INN_0 (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.INNKEEP) == 0; }  
		}, 
		INN_1 ("..", "..") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.INNKEEP) == 1; }  
		}, 
		INN_2 ("...", "...") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.INNKEEP) == 2; }  
		}, 
		INN_3 ("....", "....") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.INNKEEP) == 3; }  
		}, 
		ADVENTURER_ENCOUNTERED (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.TRUDY) == 0; }  
		}, 
		ADVENTURER_HUNT ("..", "..") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.TRUDY) == 1; }  
		},
		TRUDY_GOT_IT ("...", "...") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.TRUDY) == 2; }  
		},
		PLAYER_GOT_IT ("....", "....") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.TRUDY) == 3; }  
		},
		OGRE_DONE (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.OGRE) == 0; }  
		},
		SPIDER (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.SPIDER) == 0; }  
		},
		ALIVE ("You're still conscious.", "You fall unconscious!") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getCurrentHealth() > 0; }  
		}, 
		HAVE_DEBT ("You owe a debt.", "You are debt free.") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getCurrentDebt() > 0; }  
		}, 
		BIG_DEBT ("You owe a tremendous debt.", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getCurrentDebt() >= 100; }  
		}, 
		PROSTITUTE (".", ".") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getQuestStatus(QuestType.BROTHEL) != 0; }  
		}, 
		SCOUT_LEVEL_2 ("You are keenly aware of this area. (Scouting success!)", "This area is unknown to you.") { 
			@Override
			protected boolean getCheck(PlayerCharacter character) { return character.getScoutingScore() >= 2; }  
		},
		;
		
		private final String success;
		private final String failure;
		
		private CheckType(String success, String failure) { this.success = success; this.failure = failure; }
		
		protected abstract boolean getCheck(PlayerCharacter character);
		protected String getSuccess() {
			return success;
		}
		protected String getFailure() {
			return failure;
		}
	}
}
