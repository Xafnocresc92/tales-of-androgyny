package com.majalis.character;

import java.util.Comparator;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.majalis.character.Stance;
import com.majalis.character.AbstractCharacter.Stat;
import com.majalis.character.Attack.Status;
import com.majalis.technique.TechniquePrototype;
import com.majalis.technique.Bonus;
import com.majalis.technique.Bonus.BonusCondition;
import com.majalis.technique.Bonus.BonusType;
import com.majalis.technique.TechniquePrototype.TechniqueHeight;
/*
 * Represents an action taken by a character in battle.  Will likely need a builder helper.
 */
public class Technique {
	private final TechniquePayload initialPayload;
	private final TechniquePrototype technique;
	private final CharacterState currentState;
	private final int skillLevel;
	private final Item useItem;
	private TechniquePayload cachedPayload;
	
	public Technique(TechniquePrototype technique, CharacterState currentState, int skillLevel, Item useItem) {
		initialPayload = applyBonuses(technique, currentState, skillLevel);
		this.technique = technique;
		this.currentState = currentState;
		this.skillLevel = skillLevel;
		this.useItem = useItem;
	}
	
	public Technique(TechniquePrototype technique, CharacterState currentState, int skillLevel) {
		this(technique, currentState, skillLevel, null);
	}
	
	private TechniquePayload applyBonuses(TechniquePrototype technique, CharacterState currentState, int skillLevel) {
		Array<Bonus> bonusesToApply = new Array<Bonus>();
		for ( ObjectMap.Entry<BonusCondition, Bonus> bonusToCheck: technique.getBonuses().entries()) {
			int bonusLevel = doesBonusApply(technique, currentState, skillLevel, bonusToCheck.key);
			if (bonusLevel > 0) {
				bonusesToApply.add(bonusToCheck.value.combine(bonusLevel));
			}
		}
		return new TechniquePayload(technique, currentState, skillLevel, bonusesToApply);
	}
	
	private int doesBonusApply(TechniquePrototype technique, CharacterState currentState, int skillLevel, BonusCondition toCheck) {
		switch(toCheck) {
			case ENEMY_LOW_STABILITY:
				return currentState.getEnemyLowStability() ? 1 : 0;
			case ENEMY_ON_GROUND:
				return currentState.isEnemyOnGround() ? 1 : 0;
			case SKILL_LEVEL:
				return skillLevel;
			case OUTMANEUVER:
			case OUTMANUEVER_STRONG:
			case STRENGTH_OVERPOWER:
			case STRENGTH_OVERPOWER_STRONG:
			case ENEMY_BLOODY:
			default: return 0;
		}
	}
	
	private TechniquePayload applyBonuses(TechniquePrototype technique, CharacterState currentState, int skillLevel, Technique otherTechnique) {
		Array<Bonus> bonusesToApply = new Array<Bonus>();
		for ( ObjectMap.Entry<BonusCondition, Bonus> bonusToCheck: technique.getBonuses().entries()) {
			int bonusLevel = doesBonusApply(technique, currentState, skillLevel, bonusToCheck.key, otherTechnique);
			if (bonusLevel > 0) {
				bonusesToApply.add(bonusToCheck.value.combine(bonusLevel));
			}
		}
		return new TechniquePayload(technique, currentState, skillLevel, bonusesToApply);
	}
	
	private int doesBonusApply(TechniquePrototype technique, CharacterState currentState, int skillLevel, BonusCondition toCheck, Technique otherTechnique) {
		switch(toCheck) {
			case ENEMY_BLOODY:
				return 0;
			case ENEMY_LOW_STABILITY:
				return currentState.getEnemyLowStability() ? 1 : 0;
			case ENEMY_ON_GROUND:
				return currentState.isEnemyOnGround() ? 1 : 0;
			case SKILL_LEVEL:
				return skillLevel;
			case OUTMANEUVER:
				return currentState.getRawStat(Stat.AGILITY) - otherTechnique.getRawStat(Stat.AGILITY);
			case OUTMANUEVER_STRONG:
				return (currentState.getRawStat(Stat.AGILITY) - otherTechnique.getRawStat(Stat.AGILITY)) - 3;
			case STRENGTH_OVERPOWER:
				return currentState.getStat(Stat.STRENGTH) - otherTechnique.getStat(Stat.STRENGTH);
			case STRENGTH_OVERPOWER_STRONG:
				return (currentState.getStat(Stat.STRENGTH) - otherTechnique.getStat(Stat.STRENGTH)) - 3;
			default: return 0;
		}
	}
	
	private int getRawStat(Stat stat) {
		return currentState.getRawStat(stat);
	}

	private int getStat(Stat stat) {
		return currentState.getStat(stat);
	}

	// when a technique to interact with this one is established, this generates the final technique, which is used to extract 
	private TechniquePayload getPayload(Technique otherTechnique) {
		if (cachedPayload != null) return cachedPayload;
		TechniquePayload payload = applyBonuses(technique, currentState, skillLevel, otherTechnique);
		cachedPayload = payload;
		return payload;
	}
	// returns the resulting Attack generated by this Technique, having passed through an opposing technique
	public Array<Attack> resolve(Technique otherTechnique) {
		
		TechniquePayload otherPayload = otherTechnique.getPayload(this);
		TechniquePayload thisPayload = getPayload(otherTechnique);
		
		int rand = (int) Math.floor(Math.random() * 100);
		double blockMod = isBlockable() ? (otherPayload.getBlock() > rand * 2 ? 0 : otherPayload.getBlock() > rand ? .5 : 1) : 1;
		
		// instead of 100, this should be modified by Outmanuever
		boolean parried = isBlockable() && otherPayload.getParry() >= 100;
		boolean parryOther = otherTechnique.isBlockable() && thisPayload.getParry() >= 100;
		boolean evaded = technique.getTechniqueHeight() != TechniqueHeight.NONE && otherPayload.getEvasion() >= 100;		
		
		boolean isSuccessful = 
				technique.getTechniqueHeight() == TechniqueHeight.NONE ||
				(technique.getTechniqueHeight() == TechniqueHeight.HIGH && otherTechnique.getStance().receivesHighAttacks()) || 
				(technique.getTechniqueHeight() == TechniqueHeight.MEDIUM && otherTechnique.getStance().receivesMediumAttacks()) || 
				(technique.getTechniqueHeight() == TechniqueHeight.LOW && otherTechnique.getStance().receivesLowAttacks()) 
				;
		// this is temporarily to prevent struggling from failing to work properly on the same term an eruption or knot happens
		boolean failure = false;
		if (thisPayload.getPriority() == otherPayload.getPriority()) otherPayload.lose();
		if (isSuccessful) {
			isSuccessful = otherTechnique.getForceStance() == null || otherTechnique.getForceStance() == Stance.KNOTTED || otherTechnique.getForceStance() == Stance.KNEELING || otherPayload.getPriority() == 0 || (thisPayload.getPriority() > otherPayload.getPriority());
			failure = !isSuccessful; // to see if the attack was prevented by the other attack, rather than by other circumstances like a miss
		}
		boolean fizzle = thisPayload.getManaCost() > currentState.getMana();
		
		GrappleStatus grappleResult = 
			(technique.getGrappleType() == GrappleType.NULL && (otherTechnique.getGrappleType() == GrappleType.NULL || thisPayload.getPriority() > otherPayload.getPriority())) || 
			(otherTechnique.getGrappleType() == GrappleType.NULL && (technique.getGrappleType() == GrappleType.NULL || thisPayload.getPriority() < otherPayload.getPriority()))
			? GrappleStatus.NULL :
			thisPayload.getGrappleAmount() == otherPayload.getGrappleAmount() ? currentState.getGrappleStatus() :
			thisPayload.getGrappleAmount() > otherPayload.getGrappleAmount() ? thisPayload.getResultingGrappleStatus().inverse() : otherPayload.getResultingGrappleStatus();
			
		Array<Attack> resultingAttacks = new Array<Attack>(new Attack[]{new Attack(
			evaded ? Status.EVADED : parried ? Status.PARRIED : parryOther ? Status.PARRY : fizzle ? Status.FIZZLE : isSuccessful ? (blockMod < 1 ? Status.BLOCKED : Status.SUCCESS) : failure ? Status.FAILURE : Status.MISSED, 
			technique.getName(), 
			thisPayload.getDamage(),
			blockMod,
			((int) ((thisPayload.getTotalPower()) * thisPayload.getKnockdown()))/2, 
			thisPayload.getArmorSunder(), 
			thisPayload.getTotalPower() * thisPayload.getGutCheck(), 
			technique.isHealing() ? thisPayload.getTotalPower() : 0,
			technique.isTaunt() ? thisPayload.getTotalPower() : 0,  // lust
			grappleResult,
			otherTechnique.isBlockable() ? thisPayload.getDisarm() : 0,
			thisPayload.getTrip(),
			thisPayload.getBleeding(),
			technique.getClimaxType(), 
			getForceStance(),
			technique.isSpell(),
			new Buff(technique.getBuff(), thisPayload.getTotalPower()),
			technique.isDamaging() && !technique.doesSetDamage(), // is attack
			technique.isDamaging() && !technique.doesSetDamage() && technique.isSpell(), // ignores armor
			thisPayload.getBonuses(),
			useItem,
			currentState.getCharacter() 
		)});
		
		if (thisPayload.getCounter() >= 100 && otherTechnique.isBlockable()) {
			// create a counter attack, ignore technique for now
			resultingAttacks.add(new Attack(
				Status.SUCCESS,
				"Counter", 
				thisPayload.getBasePower() + 2, // damage
				1, // block
				(thisPayload.getBasePower() + 2) / 2, // knockdown 
				1, // armor sunder
				0, // gut check 
				0, // healing
				0, // lust
				GrappleStatus.NULL, // grapple
				0, // disarm
				0, // trip
				(thisPayload.getBasePower() + 2) / 3 + 1, // bleed
				null, // climax type
				null, // force stance
				false, // is magic
				null, // buff
				true, // is attack
				false, // ignores armor
				null, // bonuses, maybe counters should do more damage based on bonuses
				null,
				currentState.getCharacter()
			));
		}
		
		return resultingAttacks;
	}

	private boolean isBlockable() {
		return technique.isBlockable();
	}

	private GrappleType getGrappleType() {
		return technique.getGrappleType();
	}
	
	public Stance getStance() {
		return technique.getResultingStance();
	}

	protected int getStaminaCost() {
		// should be payload.getStaminaCost
		return initialPayload.getStaminaCost();
	}
	
	protected int getStabilityCost() {
		// should be payload.getStabiityCost
		return initialPayload.getStabilityCost();
	}
	
	protected int getManaCost() {
		return initialPayload.getManaCost();
	}
	
	private Stance getForceStance() {
		return technique.getForceStance();
	}	
	
	public String getTechniqueName() {
		return useItem != null ? useItem.getName() : technique.getName();
	}
	
	public String getTechniqueDescription() {
		return useItem == null ? technique.getLightDescription() : useItem.getDescription();
	}
	
	public static class StaminaComparator implements Comparator<Technique> {
		public int compare(Technique a, Technique b) {
	        return Integer.compare(a.getStaminaCost(), b.getStaminaCost());
	    }
	}
	
	public static class StabilityComparator implements Comparator<Technique> {
		public int compare(Technique a, Technique b) {
	        return Integer.compare(a.getStabilityCost(), b.getStabilityCost());
	    }
	}

	public String getBonusDescription() {
		return technique.getBonusInfo();
	}
	
	private class TechniquePayload {
		private final TechniquePrototype technique;
		private final Array<Bonus> bonuses;
		// after bonuses
		private final int basePower;
		private final int powerMod;
		private final int staminaCost;
		private final int stabilityCost;
		private final int manaCost;
		private final int block;
		private final int parry;
		private final int armorSunder;
		private final int gutCheck;
		private final int disarm;
		private final int trip;
		private final int evasion;
		private final int bleeding;
		private final int counter;
		private final double knockdown;
		private int priority;
		
		//before bonuses
		//private final int powerModBeforeBonuses;
		
		private TechniquePayload(TechniquePrototype technique, CharacterState currentState, int skillLevel, Array<Bonus> toApply) {
			this.technique = technique;
			this.bonuses = toApply;
			this.basePower = technique.isSpell() ? (technique.getBuff() != null ? currentState.getRawStat(Stat.MAGIC) : currentState.getStat(Stat.MAGIC)): technique.isTaunt() ? currentState.getRawStat(Stat.CHARISMA) : 
				// should check if technique can use weapon, and the weapon base damage should come from currentState.getWeaponDamage() rather than exposing these weapons
				currentState.getStat(Stat.STRENGTH) + (currentState.getWeapon() != null ? currentState.getWeapon().getDamage(currentState.getStats()) : 0);	
			int powerCalc = technique.getPowerMod();
			//powerModBeforeBonuses = powerCalc;
			int staminaCalc = technique.getStaminaCost();
			int stabilityCalc = technique.getStabilityCost();
			int manaCalc = technique.getManaCost();
			// this should also include + currentState.getGuardMod
			int blockCalc = technique.getGuardMod();
			int parryCalc = technique.getParryMod();
			int armorSunderCalc = technique.getArmorSunder();
			int gutCheckCalc = technique.getGutCheck();
			int disarmCalc = 0;
			double knockdownCalc = technique.getKnockdown();
			int tripCalc = 0;
			int evasionCalc = 0;
			int bleedingCalc = technique.isDamaging() && !technique.isSpell() ? basePower / 4 : 0;
			int counterCalc = 0;
			int priorityCalc = 0;
			
			for (Bonus bonusBundle : toApply) {	
				for (ObjectMap.Entry<BonusType, Integer> bonus : bonusBundle.getBonusMap()) {
					switch(bonus.key) {
						case ARMOR_SUNDER:
							armorSunderCalc += bonus.value;
							break;
						case GUARD_MOD:
							blockCalc  += bonus.value;
							break;
						case PARRY:
							parryCalc += bonus.value;
							break;
						case GUT_CHECK:
							gutCheckCalc += bonus.value;
							break;
						case KNOCKDOWN:
							knockdownCalc += bonus.value;
							break;
						case MANA_COST:
							manaCalc += bonus.value;
							break;
						case POWER_MOD:
							powerCalc += bonus.value;
							break;
						case STABILTIY_COST:
							stabilityCalc += bonus.value;
							break;
						case STAMINA_COST:
							staminaCalc += bonus.value;
							break;
						case PRIORITY:
							priorityCalc += bonus.value;
							break;
						case DISARM:
							disarmCalc += bonus.value;
							break;
						case TRIP:
							tripCalc += bonus.value;
							break;
						case EVASION:
							evasionCalc += bonus.value;
							break;
						case BLEEDING:
							bleedingCalc += bonus.value;
							break;
						case COUNTER:
							counterCalc += bonus.value;
							break;
					}
				}
			}
			
			powerMod = powerCalc;
			block = blockCalc;
			parry = parryCalc;
			staminaCost = staminaCalc;
			stabilityCost = stabilityCalc;
			manaCost = manaCalc;
			armorSunder = armorSunderCalc;
			gutCheck = gutCheckCalc;
			disarm = disarmCalc;
			trip = tripCalc;
			knockdown = knockdownCalc;
			evasion = evasionCalc;
			bleeding = currentState.getWeapon() != null && currentState.getWeapon().causesBleed()? bleedingCalc : 0;
			counter = counterCalc;
			priority = priorityCalc;
		}
		
		private int getStaminaCost() {
			return staminaCost;
		}
		
		private int getStabilityCost() {
			return stabilityCost;
		}
		
		private int getManaCost() {
			return manaCost;
		}
		
		private int getBlock() {
			return block;
		}
		
		private int getParry() {
			return parry;
		}
		
		private int getBasePower() {
			return basePower;
		}
		
		private int getTotalPower() {
			return basePower + powerMod;
		}
		
		private int getDamage() {
			int damage = technique.doesSetDamage() ? 4 : technique.isDamaging() && technique.getGutCheck() == 0 ? getTotalPower() : 0;
			if (damage < 0) damage = 0;
			return damage;
		}
		
		private double getKnockdown() {
			return knockdown;
		}
		
		private int getArmorSunder() {
			return armorSunder;
		}
		
		private int getGutCheck() {
			return gutCheck;
		}
		
		private int getPriority() {
			return priority;
		}
		
		private Array<Bonus> getBonuses() {
			return bonuses;
		}

		private int getDisarm() {
			return disarm; 
		}
		
		private int getTrip() {
			return trip; 
		}
		
		private int getEvasion() {
			return evasion;
		}
		
		private int getBleeding() {
			return bleeding;
		}
		
		private int getCounter() {
			return counter;
		}
		
		private GrappleStatus getResultingGrappleStatus() {
			return currentState.getGrappleStatus().modifyGrappleStatus(technique.getGrappleType());
		}
		
		private int getGrappleAmount() {
			return technique.getGrappleType() == GrappleType.NULL ? 0 : technique.getGrappleType() == GrappleType.HOLD ? 1 : technique.getGrappleType() == GrappleType.BREAK ? 100 : 2;
		}
		
		private void lose() {
			priority = 0;
		}
	}
}
