package com.majalis.character;

import com.badlogic.gdx.utils.ObjectMap;
import com.majalis.character.AbstractCharacter.Stat;

/*
 * Represents an individual, discrete item.
 */
public abstract class Item {

	public abstract int getValue();
	protected abstract ItemEffect getUseEffect();
	public abstract String getName();
	
	public static class Weapon extends Item {
		
		private WeaponType type;
		private String name;

		public Weapon(){}
		
		public Weapon(WeaponType type){
			this.type = type;
			this.name = type.toString();
		}
		
		@Override
		public int getValue() {
			return 10;
		}

		@Override
		protected ItemEffect getUseEffect() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		public int getDamage(ObjectMap<Stat, Integer> stats) {
			switch (type){
				case Rapier: return (stats.get(Stat.AGILITY)) / 3 + 1;
				case Cutlass: return (stats.get(Stat.STRENGTH) + stats.get(Stat.AGILITY)) / 5 + 1;
				case Broadsword: return (stats.get(Stat.STRENGTH)) / 3 + 1;
				default: return 0;
			}
		}
	}
	
	public enum WeaponType {
		Rapier,
		Cutlass,
		Broadsword
	}
	
	public static class Potion extends Item {

		private final int magnitude;
		private final EffectType effect;
		public Potion() {
			this(10);
		}
		
		public Potion(int magnitude){
			this(magnitude, EffectType.HEALING);
		}
		
		public Potion(int magnitude, EffectType effect){
			this.magnitude = magnitude;
			this.effect = effect;	
		}
		
		@Override
		public int getValue() {
			switch (effect){
			case BONUS_AGILITY:			
			case BONUS_ENDURANCE:
			case BONUS_STRENGTH:
				return magnitude * 5;
			case HEALING:
				return magnitude / 2;
			default:
				return 0;
			}
		}

		@Override
		protected ItemEffect getUseEffect() {
			return new ItemEffect(effect, magnitude);
		}

		@Override
		public String getName() {
			return effect.getDisplay() + " Potion (" + magnitude + ")"; 
		}
	}
	
	public enum EffectType {
		HEALING ("Healing"),
		BONUS_STRENGTH ("Ox"),
		BONUS_AGILITY ("Cat"),
		BONUS_ENDURANCE ("Bear");
		
		private final String display;
		private EffectType (String display){
			this.display = display;
		}
		
		public String getDisplay(){ return display; }
		
	}
	
	public class ItemEffect {
	
		private final EffectType type;
		private final int magnitude;
		
		private ItemEffect(EffectType type, int magnitude){
			this.type = type;
			this.magnitude = magnitude;
		}
		
		public EffectType getType() { return type; }
		public int getMagnitude() { return magnitude; }
	}	
}
