package com.majalis.character;

import java.util.Comparator;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.majalis.character.Stance;
import com.majalis.character.AbstractCharacter.PhallusType;
import com.majalis.character.AbstractCharacter.Stat;
import com.majalis.character.Attack.AttackHeight;
import com.majalis.character.Attack.Status;
import com.majalis.character.Item.EffectType;
import com.majalis.character.SexualExperience.SexualExperienceBuilder;
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
	
	public Technique(TechniquePrototype technique, CharacterState currentState, int skillLevel) { this(technique, currentState, skillLevel, null); }
	public Technique(TechniquePrototype technique, CharacterState currentState, int skillLevel, Item useItem) {
		initialPayload = applyBonuses(technique, currentState, skillLevel);
		this.technique = technique;
		this.currentState = currentState;
		this.skillLevel = skillLevel;
		this.useItem = useItem;
	}
	
	private boolean validTarget(Technique otherTechnique) { return technique.isSpell() || (useItem != null ? (useItem.getUseEffect().getType() == EffectType.MAGIC || useItem.getUseEffect().getType() == EffectType.ARMOR_SUNDER) : false) || otherTechnique.isCorporeal(); }
	private boolean validTechniqueHeight(Technique otherTechnique) { 
		return technique.getTechniqueHeight() == TechniqueHeight.NONE ||
			(technique.getTechniqueHeight() == TechniqueHeight.HIGH && otherTechnique.getStance().receivesHighAttacks()) || 
			(technique.getTechniqueHeight() == TechniqueHeight.MEDIUM && otherTechnique.getStance().receivesMediumAttacks()) || 
			(technique.getTechniqueHeight() == TechniqueHeight.LOW && otherTechnique.getStance().receivesLowAttacks());
	}
	private boolean validRange() { return !technique.isMelee() || currentState.getRange() < 2; }
	
	// returns the resulting Attack generated by this Technique, having passed through an opposing technique
	public Array<Attack> resolve(Technique otherTechnique) {
		TechniquePayload otherPayload = otherTechnique.getPayload(this);
		TechniquePayload thisPayload = getPayload(otherTechnique);
		
		// this needs to determine dodged damage, parried damage, and blocked damage after damage has been split into quartiles		
		boolean isSuccessful = validTarget(otherTechnique) && validTechniqueHeight(otherTechnique) && validRange();
		boolean failure = false;
		// this is temporarily to prevent struggling from failing to work properly on the same term an eruption or knot happens
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
			thisPayload.getGrappleAmount() == otherPayload.getGrappleAmount() ? otherPayload.getCurrentGrappleStatus() :
			thisPayload.getGrappleAmount() > otherPayload.getGrappleAmount() ? thisPayload.getResultingGrappleStatus().inverse() : otherPayload.getResultingGrappleStatus();

		int blockAmount = isBlockable() ? otherPayload.getBlock() : 0;
		int parryAmount = isParryable() ? otherPayload.getParry() : 0;
		int evadeAmount = technique.getTechniqueHeight() != TechniqueHeight.NONE && isEvadeable() ? otherPayload.getEvasion() : 0;
		
		// rather than phallus type this should be setting enemyType and phallusType should be inferred
		
		// figure out what statuses are actually used and remove the ones that aren't
		Array<Attack> resultingAttacks = new Array<Attack>(new Attack[]{new Attack(
			evadeAmount >= 4 ? Status.EVADED : fizzle ? Status.FIZZLE : isSuccessful ? (blockAmount > 0 ? Status.BLOCKED : parryAmount > 0 ? Status.PARRIED : Status.SUCCESS) : failure ? Status.FAILURE : Status.MISSED, 
			technique.getName(), 
			thisPayload.getDamage(), // this is the damage before guard is applied
			blockAmount,
			parryAmount,
			evadeAmount,
			(technique.getSpellEffect() != null && technique.getSpellEffect().isDamaging()) || (useItem != null && useItem.getUseEffect().getType() == EffectType.MAGIC) ? otherPayload.getMagicResistance() : 0,
			useItem != null && useItem.getUseEffect().getType() == EffectType.KNOCKDOWN ? useItem.getUseEffect().getMagnitude() : ((int) ((thisPayload.getTotalPower()) * thisPayload.getKnockdown()))/2, 
			useItem != null && useItem.getUseEffect().getType() == EffectType.ARMOR_SUNDER ? useItem.getUseEffect().getMagnitude() : thisPayload.getArmorSunder(), 
			thisPayload.getTotalPower() * thisPayload.getGutCheck(), 
			technique.isHealing() ? thisPayload.getTotalPower() : 0,
			thisPayload.getSex().setPhallusType(thisPayload.getPhallusType()).increaseTeasing(thisPayload.getTotalPower()).build(), // the sex they'll receive
			otherPayload.getSelfSex().setPhallusType(thisPayload.getPhallusType()).build(), // their sex
			grappleResult,
			otherTechnique.isParryable() ? thisPayload.getDisarm() : 0,
			thisPayload.getTrip(),
			thisPayload.getBleeding(),
			thisPayload.getRemovePlug(),
			technique.getClimaxType(), 
			getAdvance() + otherTechnique.getAdvance(),
			technique.getRange(),
			getForceStance(),
			technique.getSpellEffect(),
			new Buff(technique.getSelfEffect(), thisPayload.getTotalPower()),
			new Buff(technique.getEnemyEffect(), thisPayload.getTotalPower()),
			technique.isDamaging() && !technique.doesSetDamage(), // is attack,
			technique.getTechniqueHeight() == TechniqueHeight.HIGH ? AttackHeight.HIGH : technique.getTechniqueHeight() == TechniqueHeight.MEDIUM ? AttackHeight.MEDIUM : technique.getTechniqueHeight() == TechniqueHeight.LOW ? AttackHeight.LOW : AttackHeight.NONE,
			technique.ignoresArmor() || ((useItem != null && useItem.getUseEffect() != null) ? (useItem.getUseEffect().getType() == EffectType.MAGIC || useItem.getUseEffect().getType() == EffectType.ARMOR_SUNDER) : false) || (technique.isDamaging() && technique.isSpell()) || technique.doesSetDamage(), // ignores armor
			thisPayload.getBonuses(),
			useItem, // only works for self item use
			currentState.getCharacter() 
		)});
		
		if (thisPayload.getCounter() >= 100 && otherTechnique.isParryable()) {
			// create a counter attack, ignore technique for now
			resultingAttacks.add(new Attack(
				Status.SUCCESS,
				"Counter", 
				thisPayload.getBasePower() + 2, // damage
				0, // block
				0, // parry
				0, // evade
				0, // magic resistance
				(thisPayload.getBasePower() + 2) / 2, // knockdown 
				1, // armor sunder
				0, // gut check 
				0, // healing
				new SexualExperience(), // sex
				new SexualExperience(), // self-sex
				GrappleStatus.NULL, // grapple
				0, // disarm
				0, // trip
				(thisPayload.getBasePower() + 2) / 3 + 1, // bleed
				0,
				null, // climax type
				0, // advance
				1, // range
				null, // force stance
				null, // spell effect
				null, // self effect
				null, // enemy effect
				true, // is attack
				AttackHeight.MEDIUM,
				false, // ignores armor
				null, // bonuses, maybe counters should do more damage based on bonuses
				null,
				currentState.getCharacter()
			));
		}
		
		return resultingAttacks;
	}
	public Stance getStance() { return technique.getResultingStance(); }
	public String getTechniqueName() { return useItem != null ? useItem.getName() : technique.getName(); }
	public String getTechniqueDescription() { return useItem == null ? technique.getLightDescription() : useItem.getDescription(); }
	public String getBonusDescription() { return technique.getBonusInfo(); }
	public int getAdvance() { return technique.getAdvance(); }
	
	private TechniquePayload applyBonuses(TechniquePrototype technique, CharacterState currentState, int skillLevel) {
		Array<Bonus> bonusesToApply = new Array<Bonus>();
		for ( ObjectMap.Entry<BonusCondition, Bonus> bonusToCheck: technique.getBonuses().entries()) {
			int bonusLevel = doesBonusApply(technique, currentState, skillLevel, bonusToCheck.key);
			if (bonusLevel > 0) { bonusesToApply.add(bonusToCheck.value.combine(bonusLevel)); }
		}
		return new TechniquePayload(technique, currentState, skillLevel, bonusesToApply);
	}
	
	private int doesBonusApply(TechniquePrototype technique, CharacterState currentState, int skillLevel, BonusCondition toCheck) {
		switch(toCheck) {
			case ENEMY_LOW_STABILITY: return currentState.getEnemyLowStability() ? 1 : 0;
			case ENEMY_ON_GROUND: return currentState.isEnemyOnGround() ? 1 : 0; // this should be changed to is enemy defenseless, based on enemy technique
			case SKILL_LEVEL: return skillLevel;
			case QUICKNESS: return currentState.getStat(Stat.AGILITY);
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
			case ENEMY_BLOODY: return 0;
			case ENEMY_LOW_STABILITY: return currentState.getEnemyLowStability() ? 1 : 0;
			case ENEMY_ON_GROUND: return currentState.isEnemyOnGround() ? 1 : 0;
			case SKILL_LEVEL: return skillLevel;
			case QUICKNESS: return currentState.getStat(Stat.AGILITY);
			case OUTMANEUVER: return currentState.getRawStat(Stat.AGILITY) + currentState.getPerks().get(Perk.COMBAT_FINESSE, 0) * 2 - (otherTechnique.getRawStat(Stat.AGILITY) + otherTechnique.getPerks().get(Perk.COMBAT_FINESSE, 0) * 2);
			case OUTMANUEVER_STRONG: return (currentState.getRawStat(Stat.AGILITY) + currentState.getPerks().get(Perk.COMBAT_FINESSE, 0) * 2 - (otherTechnique.getRawStat(Stat.AGILITY) + otherTechnique.getPerks().get(Perk.COMBAT_FINESSE, 0) * 2)) - 2;
			case STRENGTH_OVERPOWER: return currentState.getStat(Stat.STRENGTH) - otherTechnique.getStat(Stat.STRENGTH);
			case STRENGTH_OVERPOWER_STRONG: return (currentState.getStat(Stat.STRENGTH) - otherTechnique.getStat(Stat.STRENGTH)) - 2;
			default: return 0;
		}
	}
	
	private ObjectMap<Perk, Integer> getPerks() { return currentState.getPerks();  }
	private int getRawStat(Stat stat) { return currentState.getRawStat(stat); }
	private int getStat(Stat stat) { return currentState.getStat(stat); }

	// when a technique to interact with this one is established, this generates the final technique, which is used to extract 
	private TechniquePayload getPayload(Technique otherTechnique) {
		if (cachedPayload != null) return cachedPayload;
		TechniquePayload payload = applyBonuses(technique, currentState, skillLevel, otherTechnique);
		cachedPayload = payload;
		return payload;
	}
	
	protected int getStaminaCost() { return initialPayload.getStaminaCost(); } // should be payload.getStaminaCost
	protected int getStabilityCost() { return initialPayload.getStabilityCost(); } // should be payload.getStabiityCost
	protected int getManaCost() { return initialPayload.getManaCost(); }
	private boolean isCorporeal() { return currentState.isCorporeal(); }
	private boolean isBlockable() { return technique.isBlockable(); }
	private boolean isParryable() { return technique.isParryable(); }
	private boolean isEvadeable() { return technique.isEvadeable(); }
	private GrappleType getGrappleType() { return technique.getGrappleType(); }
	private Stance getForceStance() { return technique.getForceStance(); }	

	public static class StaminaComparator implements Comparator<Technique> {
		public int compare(Technique a, Technique b) { return Integer.compare(a.getStaminaCost(), b.getStaminaCost()); }
	}
	
	public static class StabilityComparator implements Comparator<Technique> { 
		public int compare(Technique a, Technique b) { return Integer.compare(a.getStabilityCost(), b.getStabilityCost()); }
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
		private final int evasion;
		private final int magicResistance;
		private final int armorSunder;
		private final int gutCheck;
		private final int disarm;
		private final int trip;
		private final int bleeding;
		private final int counter;
		private final double knockdown;
		private final int grapple;
		private final int removePlug;
		private int priority;
		
		//before bonuses
		//private final int powerModBeforeBonuses;
		
		private TechniquePayload(TechniquePrototype technique, CharacterState currentState, int skillLevel, Array<Bonus> toApply) {
			this.technique = technique;
			this.bonuses = toApply;
			this.basePower = technique.isSpell() ? (technique.getSelfEffect() != null ? currentState.getRawStat(Stat.MAGIC) : currentState.getStat(Stat.MAGIC)) : technique.getSex().isTeasing() ? currentState.getLewdCharisma() : 
				technique.isMelee() ? currentState.getStat(Stat.STRENGTH) + (currentState.getWeapon() != null ? currentState.getWeapon().getDamage(currentState.getStats()) : 0) : // should check if technique can use weapon, and the weapon base damage should come from currentState.getWeaponDamage() rather than exposing these weapons
				currentState.getRangedWeapon() != null ? currentState.getRangedWeapon().getDamage(currentState.getStats()) : 0;
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
			int evasionCalc = technique.getEvadeMod();
			int bleedingCalc = technique.isDamaging() && !technique.isSpell() ? basePower / 4 : 0;
			int counterCalc = 0;
			int priorityCalc = 0;
			int grappleCalc = technique.getGrappleType() == GrappleType.NULL || technique.getGrappleType() == GrappleType.SUBMIT || technique.getGrappleType() == GrappleType.PIN ? 0 : technique.getGrappleType() == GrappleType.HOLD ? 1 : technique.getGrappleType() == GrappleType.BREAK ? 100 : technique.getGrappleType() == GrappleType.WIN ? 101 : 2;
			int removePlugCalc = 0;
			
			for (Bonus bonusBundle : toApply) {	
				for (ObjectMap.Entry<BonusType, Integer> bonus : bonusBundle.getBonusMap()) {
					switch(bonus.key) {
						case ARMOR_SUNDER: armorSunderCalc += bonus.value; break;
						case GUARD_MOD: blockCalc  += bonus.value; break;
						case PARRY: parryCalc += bonus.value; break;
						case GUT_CHECK: gutCheckCalc += bonus.value; break;
						case KNOCKDOWN: knockdownCalc += bonus.value; break;
						case MANA_COST: manaCalc += bonus.value; break;
						case POWER_MOD: powerCalc += bonus.value; break;
						case STABILTIY_COST: stabilityCalc += bonus.value; break;
						case STAMINA_COST: staminaCalc += bonus.value; break;
						case PRIORITY: priorityCalc += bonus.value; break;
						case DISARM: disarmCalc += bonus.value; break;
						case TRIP: tripCalc += bonus.value; break;
						case EVASION: evasionCalc += bonus.value; break;
						case BLEEDING: bleedingCalc += bonus.value; break; 
						case COUNTER: counterCalc += bonus.value; break;
						case GRAPPLE: grappleCalc += bonus.value; break;
						case REMOVE_PLUG: removePlugCalc += bonus.value; break;
					}
				}
			}
			powerMod = powerCalc;
			block = currentState.getShieldScore() > 0 ? blockCalc : 0;
			parry = parryCalc;
			evasion = evasionCalc;
			magicResistance = currentState.getCharacter().getMagicResistance(); // needs to use current state
			staminaCost = staminaCalc;
			stabilityCost = stabilityCalc - (technique.getUsableStance() == Stance.BALANCED && technique.getResultingStance() != Stance.BALANCED ? currentState.getPerks().get(Perk.VERSATILE, 0) : 0);
			manaCost = manaCalc;
			armorSunder = armorSunderCalc * (1 + currentState.getPerks().get(Perk.SUNDERER, 0)); 
			gutCheck = gutCheckCalc;
			disarm = disarmCalc;
			trip = tripCalc;
			knockdown = knockdownCalc;
			bleeding = technique.causesBleed() ? (technique.getSetBleed() + (technique.isMelee() ? (currentState.getWeapon() != null && currentState.getWeapon().causesBleed() ? bleedingCalc : 0) : (currentState.getRangedWeapon() != null && currentState.getRangedWeapon().causesBleed() ? bleedingCalc : 0))) : 0;
			counter = counterCalc;
			priority = priorityCalc;
			grapple = technique.getGrappleType() == GrappleType.NULL ? 0 : grappleCalc + currentState.getGrappleMod();
			removePlug = removePlugCalc;
		}

		private int getDamage() {
			int damage = technique.doesSetDamage() || (useItem != null && useItem.getUseEffect().getType() == EffectType.ARMOR_SUNDER) ? 4 : 
				useItem != null && useItem.getUseEffect().getType() == EffectType.MAGIC ? useItem.getUseEffect().getMagnitude() :
				technique.isDamaging() && technique.getGutCheck() == 0 ? getTotalPower() : 0;
			if (damage < 0) damage = 0;
			return damage;
		}
		private SexualExperienceBuilder getSelfSex() { return technique.getSelfSex(); }
		private SexualExperienceBuilder getSex() { return technique.getSex(); }
		private PhallusType getPhallusType() { return currentState.getPhallusType(); }
		private GrappleStatus getCurrentGrappleStatus() { return currentState.getGrappleStatus(); }
		private int getStaminaCost() { return staminaCost; }
		private int getStabilityCost() { return stabilityCost; }
		private int getManaCost() { return manaCost; }
		private int getBlock() { return block; }
		private int getParry() { return parry; }
		private int getEvasion() { return evasion; }
		public int getMagicResistance() { return magicResistance; }
		private int getBasePower() { return basePower; }
		private int getTotalPower() { return basePower + powerMod; }
		private double getKnockdown() { return knockdown; }
		private int getArmorSunder() { return armorSunder; }
		private int getGutCheck() { return gutCheck; }
		private int getPriority() { return priority; }
		private Array<Bonus> getBonuses() { return bonuses; }
		private int getDisarm() { return disarm; }
		private int getTrip() { return trip; }
		
		private int getBleeding() { return bleeding; }
		private int getCounter() { return counter; }
		private GrappleStatus getResultingGrappleStatus() { return currentState.getGrappleStatus().modifyGrappleStatus(technique.getGrappleType()); }
		private int getGrappleAmount() { return grapple; }
		private int getRemovePlug() { return removePlug; }
		private void lose() { priority = 0; }
	}
}
