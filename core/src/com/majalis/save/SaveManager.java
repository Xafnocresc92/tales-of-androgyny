package com.majalis.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.majalis.battle.BattleCode;
import com.majalis.character.EnemyCharacter;
import com.majalis.character.PlayerCharacter;
import com.badlogic.gdx.utils.ObjectSet;
/*
 * Used for file handling, both reading and writing - both game files and encounter replay files.
 */
public class SaveManager implements SaveService, LoadService{
    
	private boolean encoded;
    private final FileHandle file;   
    private GameSave save;
   
    public SaveManager(boolean encoded, String path){
        this.encoded = encoded;
        file = Gdx.files.local(path);   
        save = getSave();
    }
    
    @SuppressWarnings("unchecked")
	public void saveDataValue(SaveEnum key, Object object){
    	switch (key){
	    	case PLAYER: 			save.player = (PlayerCharacter) object; break;
	    	case ENEMY: 			save.enemy = (EnemyCharacter) object; break;
	    	case SCENE_CODE: 		save.sceneCode = (Integer) object; break;
	    	case CONTEXT: 			save.context = (GameContext) object; break;
	    	case NODE_CODE: 		save.nodeCode = (Integer) object; break;
	    	case ENCOUNTER_CODE:	save.encounterCode = (Integer) object; break;
	    	case VISITED_LIST:		save.visitedList = castToIntArray((ObjectSet<Integer>) object); break;
	    	case BATTLE_CODE:		save.battleCode = (BattleCode) object; break;
	    	case CLASS:				save.jobClass = (JobClass) object; break;
	    	case WORLD_SEED:		save.worldSeed = (Integer) object; break;
    	}	
        saveToJson(save); //Saves current save immediately.
    }
    
    @SuppressWarnings("unchecked")
    public <T> T loadDataValue(SaveEnum key, Class<?> type){
    	switch (key){
	    	case PLAYER: 			return (T) (PlayerCharacter)save.player;
	    	case ENEMY: 			return (T) (EnemyCharacter)save.enemy;
	    	case SCENE_CODE: 		return (T) (Integer)save.sceneCode;
	    	case CONTEXT: 			return (T) save.context;
	    	case NODE_CODE: 		return (T) (Integer)save.nodeCode;
	    	case ENCOUNTER_CODE:	return (T) (Integer) save.encounterCode;
	    	case VISITED_LIST:		ObjectSet<Integer> set = new ObjectSet<Integer>();
	    							for (int member : save.visitedList){
	    								set.add(member);
	    							}
	    							return (T) set;
	    	case BATTLE_CODE:		return (T) save.battleCode;
	    	case CLASS:				return (T) save.jobClass;
	    	case WORLD_SEED:		return (T) (Integer) save.worldSeed;
    	}	
    	return null;
    }
    
    private void saveToJson(GameSave save){
        Json json = new Json();
        json.setOutputType(OutputType.json);
        if(encoded) file.writeString(Base64Coder.encodeString(json.prettyPrint(save)), false);
        else {   	
        	file.writeString(json.prettyPrint(save), false);
        }
    }
    
    private GameSave getSave(){
        GameSave save;
        if(file.exists()){
	        Json json = new Json();
	        if(encoded)save = json.fromJson(GameSave.class, Base64Coder.decodeString(file.readString()));
	        else save = json.fromJson(GameSave.class,file.readString());
        }
        else {
        	save = getDefaultSave();
        }
        return save==null ? new GameSave(true) : save;
    }
    
    public void newSave(){
    	save = getDefaultSave();
    }
    
    private GameSave getDefaultSave(){
    	GameSave tempSave = new GameSave(true);
    	saveToJson(tempSave);
    	return tempSave;
    }
    
    private int[] castToIntArray(ObjectSet<Integer> set){
    	int[] array = new int[set.size];
    	int ii = 0;
    	for (int member : set){
    		array[ii++] = member;
    	}
    	return array;
    }
    
    public static class GameSave{
    	
		public GameContext context;
		public int worldSeed;
		public int sceneCode;
		public int encounterCode;
    	public int nodeCode;
    	public int[] visitedList;
    	public JobClass jobClass;
    	// this can probably be refactored to contain a particular battle, but may need to duplicate the player character
    	public BattleCode battleCode;
    	public PlayerCharacter player;
    	public EnemyCharacter enemy;
    	
    	// 0-arg constructor for JSON serialization: DO NOT USE
    	@SuppressWarnings("unused")
		private GameSave(){}
    	
    	// default save values-
    	public GameSave(boolean defaultValues){
    		if (defaultValues){
    			context = GameContext.ENCOUNTER;
    			worldSeed = (int) (Math.random()*10000);
    			System.out.println(worldSeed);
    			//worldSeed = 7;
    			// 0 sceneCode is the magic number to designate that a scene doesn't need to be loaded; just use the first (last) scene in the list
    			sceneCode = 0;
    			encounterCode = 0;
        		nodeCode = 1;
        		visitedList = new int[]{1};
        		player = new PlayerCharacter(true);
    		}
    	}
    }
    
	public enum JobClass {
		WARRIOR ("Warrior"),
		PALADIN ("Paladin"),
		THIEF ("Thief"),
		RANGER ("Ranger"),
		MAGE ("Mage"),
		ENCHANTRESS ("Enchanter");
		
		private final String label;

		JobClass(String label) {
		    this.label = label;
		 }
		public String getLabel(){return label;}
	}
	
	public enum GameContext {
		ENCOUNTER,
		WORLD_MAP,
		BATTLE
	}
}