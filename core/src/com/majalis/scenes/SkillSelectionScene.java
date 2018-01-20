package com.majalis.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.majalis.asset.AssetEnum;
import com.majalis.character.Perk;
import com.majalis.character.PlayerCharacter;
import com.majalis.character.Stance;
import com.majalis.character.Techniques;
import com.majalis.encounter.Background;
import com.majalis.save.SaveEnum;
import com.majalis.save.SaveService;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class SkillSelectionScene extends Scene {

	private static int tableHeight = 900;
	
	private final SaveService saveService;
	private final Skin skin;
	private final Sound buttonSound;
	private final PlayerCharacter character;
	private final Texture arrowImage;
	private final AssetManager assetManager;
	private final Group skillGroup;
	private final Group magicGroup;
	private final Group perkGroup;
	private Array<StanceSkillDisplay> allDisplay;
	private Array<Table> perkDisplay;
	private int stanceSelection;
	private int perkSelection;
	private Label skillDisplay;
	private Label bonusDisplay;
	private Table skillDisplayTable;
	private Table consoleTable;
	private Label console;
	private Label skillPointsDisplay;
	private int skillPoints;
	private int magicPoints;
	private int perkPoints;
	private ObjectMap<Techniques, Integer> cachedSkills;
	private ObjectMap<Perk, Integer> cachedPerks;
	private ObjectMap<Techniques, Integer> skills;
	private ObjectMap<Perk, Integer> perks;
	private ObjectMap<Techniques, Label> techniquesToButtons;
	private Row selectedRow;
	private boolean locked;
	private boolean justUnlocked;
	
	public SkillSelectionScene(OrderedMap<Integer, Scene> sceneBranches, int sceneCode, final SaveService saveService, Background background, AssetManager assetManager, PlayerCharacter character) {
		super(sceneBranches, sceneCode);
		this.saveService = saveService;
		this.character = character;
		this.addActor(background);
		this.assetManager = assetManager;
		this.arrowImage = assetManager.get(AssetEnum.STANCE_ARROW.getTexture());
		skillGroup = new Group();
		magicGroup = new Group();
		perkGroup = new Group();
		this.addActor(skillGroup);
		this.addActor(magicGroup);
		this.addActor(perkGroup);
		skin = assetManager.get(AssetEnum.UI_SKIN.getSkin());
		buttonSound = assetManager.get(AssetEnum.BUTTON_SOUND.getSound());
		allDisplay = new Array<StanceSkillDisplay>();
		perkDisplay  = new Array<Table>();
		techniquesToButtons = new ObjectMap<Techniques, Label>();
		locked = false;
		justUnlocked = false;
		stanceSelection = 0;
		perkSelection = 0;
	}
	
	private void setSelectedRow(Row row) { // row your boat
		if (selectedRow != null) selectedRow.setUnselected();
		selectedRow = row;
		row.setSelected();
	}
	
	private void setUnselectedRow(Row row) { // gently down the stream
		row.setUnselected();
		selectedRow = null;
	}
	
	private ClickListener getListener(final Row row) { // merrily merrily merrily merrily
		return new ClickListener() {
			@Override
	        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				setSelectedRow(row);
			}
			@Override
	        public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				if (!locked) {
					if (!justUnlocked) {
						setUnselectedRow(row);
					}
					else {
						justUnlocked = false;
					}
				}	
			}
		};
	}
	
	@Override
	public void setActive() {
		isActive = true;
		this.addAction(Actions.show());
		this.setBounds(0, 0, 2000, 2000);
		saveService.saveDataValue(SaveEnum.SCENE_CODE, sceneCode);
		
		this.skillPoints = character.getSkillPoints();
		this.magicPoints = character.getMagicPoints();
		this.perkPoints = character.getPerkPoints();
		
		skillPointsDisplay = addLabel("Skill Points: " + skillPoints, skin, null, Color.WHITE, 10, 90);
		final Label magicPointsDisplay = addLabel("Magic Points: " + magicPoints, skin, null, Color.WHITE, 10, 50);
		final Label perkPointsDisplay = addLabel("Perk Points: " + perkPoints, skin, null, Color.WHITE, 10, 10);

		addImage(skillGroup, assetManager.get(AssetEnum.SKILL_TITLE.getTexture()), Color.WHITE, 25, 1000);
		addImage(magicGroup, assetManager.get(AssetEnum.MAGIC_TITLE.getTexture()), Color.WHITE, 25, 1000);
		addImage(perkGroup, assetManager.get(AssetEnum.PERK_TITLE.getTexture()), Color.WHITE, 25, 1000);
		
		Image temp = addImage(assetManager.get(AssetEnum.SKILL_CONSOLE_BOX.getTexture()), Color.WHITE, 940 + 420, 0, 560, 1080); 
		temp.addAction(Actions.alpha(.9f));
		temp = addImage(skillGroup, assetManager.get(AssetEnum.SKILL_BOX_0.getTexture()), Color.WHITE, 200, 0);
		temp.addAction(Actions.alpha(.9f));
		temp = addImage(magicGroup, assetManager.get(AssetEnum.SKILL_BOX_1.getTexture()), Color.WHITE, 200, 0);
		temp.addAction(Actions.alpha(.9f));
		temp = addImage(perkGroup, assetManager.get(AssetEnum.SKILL_BOX_2.getTexture()), Color.WHITE, 200, 0);		
		temp.addAction(Actions.alpha(.9f));
		
		int consoleX = 1665;
		int consoleY = 975;
		int consoleWidth = 470;
		
		consoleTable = new Table();
		consoleTable.setPosition(consoleX,  consoleY);
		console = new Label("", skin);
		console.setWrap(true);
		console.setColor(Color.BLACK);
		consoleTable.add(console).width(consoleWidth);
		consoleTable.align(Align.top);
		this.addActor(consoleTable);
		
		skillDisplayTable = new Table();
		skillDisplayTable.setPosition(consoleX,  consoleY);
		skillDisplay = new Label("", skin);
		skillDisplay.setWrap(true);
		skillDisplay.setColor(Color.BLACK);
		skillDisplayTable.add(skillDisplay).width(consoleWidth).row();
		skillDisplayTable.align(Align.top);
		
		bonusDisplay = new Label("", skin);
		bonusDisplay.setWrap(true);
		bonusDisplay.setColor(Color.FOREST);
		skillDisplayTable.add(bonusDisplay).width(consoleWidth);		
		
		this.addActor(skillDisplayTable);
		
		this.cachedSkills = new ObjectMap<Techniques, Integer>(character.getSkills());
		this.cachedPerks = new ObjectMap<Perk, Integer>(character.getPerks());
		skills = new ObjectMap<Techniques, Integer>(cachedSkills);
		perks = new ObjectMap<Perk, Integer>(cachedPerks);
		
		for(Stance stance : Stance.values()) {
			if (!stance.hasLearnableSkills()) continue;
			StanceSkillDisplay newStanceSkillDisplay = new StanceSkillDisplay(stance, assetManager);
			newStanceSkillDisplay.setPosition(495, tableHeight - 210);
			newStanceSkillDisplay.addAction(Actions.hide());
			skillGroup.addActor(newStanceSkillDisplay);
			allDisplay.add(newStanceSkillDisplay);
		}
		
		Array<Perk> learnablePerks = new Array<Perk>();
		for (final Perk perk: Perk.values()) {
			if (perk.isLearnable()) {
				learnablePerks.add(perk);
			}
		}
		
		allDisplay.get(0).addAction(Actions.show());
		for (int ii = 0; ii < 2; ii++) {
			final Table perkTable = new Table();
			int jj = 0;
			for (final Perk perk: learnablePerks) {
				if (!(jj >= ii * learnablePerks.size / 2 && jj < (ii + 1) * learnablePerks.size / 2)) { jj++; continue; }
				Integer level = perks.get(perk, 0);
				final Label label = new Label(perk.getLabel(), skin);
				label.setColor(Color.WHITE);
				label.setAlignment(Align.right);
				final Label value = new Label(level > 0 ? "(" + level + ")" : "", skin);
				value.setAlignment(Align.right);
				
				final Row row = new Row(perk, label, skillDisplay, bonusDisplay, skillDisplayTable, consoleTable);
				final TextButton plusButton = new TextButton("+", skin);
				final TextButton minusButton = new TextButton("-", skin);
				
				label.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						locked = !locked;
						justUnlocked = !locked;
					}
				});
				label.addListener(getListener(row));
				plusButton.addListener(getListener(row));
				minusButton.addListener(getListener(row));
				
				plusButton.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
						if (perkPoints > 0) {
							Integer level = perks.get(perk, 0);
							if (level < perk.getMaxRank()) {
								if (level + 1 <= perkPoints) {
									perkPoints -= level + 1;
									perkPointsDisplay.setText("Perk Points: " + perkPoints);
									if (perk == Perk.SKILLED) {
										skillPoints += 2;
										skillPointsDisplay.setText("Skill Points: " + skillPoints);
									}
									perks.put(perk, ++level);						
									console.setText("You gained the " + perk.getLabel() + " Rank " + level +".");
									value.setText(level == 0 ? "" : "(" + level + ")");
								}
								else {
									console.setText("You do not have enough perk points!");
								}	
							}
							else {
								console.setText("You cannot improve on that perk any further!");
							}		
						}
						else {
							console.setText("You have no perk points!");
						}
			        }
				});
				minusButton.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
						
						Integer level = perks.get(perk, 0);
						if (level == 0) {
							console.setText("You do not yet possess that perk!");
						}
						else {
							Integer cachedLevel = cachedPerks.get(perk, 0);
							if (--level >= cachedLevel) {
								perkPoints += level + 1;
								perkPointsDisplay.setText("Perk Points: " + perkPoints);
								perks.put(perk, level);
								if (level > 0) {
									console.setText("You have reduced " + perk.getLabel() + " to Rank " + level +".");
								}
								else {
									console.setText("You have removed " + perk.getLabel() + ".");
								}
								if (perk == Perk.SKILLED) {
									skillPoints -= 2;
									skillPointsDisplay.setText("Skill Points: " + skillPoints);
									handleNegativeSkillPoints();
								}
								
								value.setText(level == 0 ? "" : "(" + level + ")");
							}
							else {
								console.setText("You cannot reduce " + perk.getLabel() + " below Rank " + cachedLevel +".");
							}
						}
			        }
				});
				perkTable.add(label).size(200, 45).padRight(10);
				perkTable.add(value).size(30, 45).padRight(10);
				perkTable.add(plusButton).size(45, 60);
				perkTable.add(minusButton).size(45, 60).row();
				jj++;
			}
			perkTable.setPosition(530, tableHeight - 215);
			perkTable.align(Align.top);
			perkTable.addAction(Actions.hide());
			perkDisplay.add(perkTable);
			perkGroup.addActor(perkTable);
		}
		
		
		if (character.hasMagic()) {
			final Table magicTable = new Table();
			
			for (final Techniques technique: Techniques.getLearnableSpells()) {
				Integer level = skills.get(technique, 0);
				final Label label = new Label(technique.getTrait().getName(), skin);
				label.setColor(Color.WHITE);
				label.setAlignment(Align.right);
				final Label value = new Label(level > 0 ? "(" + level + ")" : "", skin);
				value.setAlignment(Align.right);
				
				final Row row = new Row(technique, label, skillDisplay, bonusDisplay, skillDisplayTable, consoleTable);
				final TextButton plusButton = new TextButton("+", skin);
				final TextButton minusButton = new TextButton("-", skin);
				
				label.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						locked = !locked;
						justUnlocked = !locked;
					}
				});
				label.addListener(getListener(row));
				plusButton.addListener(getListener(row));
				minusButton.addListener(getListener(row));
				
				plusButton.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
						if (magicPoints > 0) {
							Integer level = skills.get(technique, 0);
							if (level < technique.getMaxRank()) {
								if (level + 1 <= magicPoints) {
									magicPoints -= level + 1;
									magicPointsDisplay.setText("Magic Points: " + magicPoints);
									skills.put(technique, ++level);						
									console.setText("You have learned " + technique.getTrait().getName() + " Rank " + level +".");
									value.setText(level == 0 ? "" : "(" + level + ")");
								}
								else {
									console.setText("You do not have enough magic points!");
								}
							}
							else {
								console.setText("You cannot improve on that spell any further!");
							}	
						}
						else {
							console.setText("You have no magic points!");
						}
			        }
				});
				minusButton.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
						
						Integer level = skills.get(technique, 0);
						if (level == 0) {
							console.setText("You do not yet possess that spell!");
						}
						else {
							Integer cachedLevel = cachedSkills.get(technique, 0);
							if (--level >= cachedLevel) {
								magicPoints += level + 1;
								magicPointsDisplay.setText("Magic Points: " + magicPoints);
								skills.put(technique, level);	
								if (level > 0) {
									console.setText("You have reduced " + technique.getTrait().getName() + " to Rank " + level +".");
								}
								else {
									console.setText("You have unlearned " + technique.getTrait().getName() + ".");
								}
								value.setText(level == 0 ? "" : "(" + level + ")");
							}
							else {
								console.setText("You cannot reduce " + technique.getTrait().getName() + " below Rank " + cachedLevel +".");
							}
						}
			        }
				});
				magicTable.add(label).size(200, 45).padRight(10);
				magicTable.add(value).size(30, 45).padRight(10);
				magicTable.add(plusButton).size(45, 60);
				magicTable.add(minusButton).size(45, 60).row();
			}
			magicTable.setPosition(530, tableHeight - 215);
			magicTable.align(Align.top);
			magicGroup.addActor(magicTable);
		}
		Image arrow = new Image(arrowImage);
        arrow.setHeight(300);
        arrow.setWidth(120);
        arrow.setPosition(780, 320);
        arrow.addListener(new ClickListener() {
        	@Override
	        public void clicked(InputEvent event, float x, float y) {
        		changeStanceDisplay(1);
        	}
        });
        skillGroup.addActor(arrow);
        
        TextureRegion flipped = new TextureRegion(arrowImage);
        flipped.flip(true, false);
        Image arrow2 = new Image(flipped);
        arrow2.setHeight(300);
        arrow2.setWidth(120);
        arrow2.setPosition(140, 320);
        arrow2.addListener(new ClickListener() {
        	@Override
	        public void clicked(InputEvent event, float x, float y) {
        		changeStanceDisplay(-1);
        	}
        });
        skillGroup.addActor(arrow2);
        
        Image arrow3 = new Image(arrowImage);
        arrow3.setHeight(300);
        arrow3.setWidth(120);
        arrow3.setPosition(780, 320);
        arrow3.addListener(new ClickListener() {
        	@Override
	        public void clicked(InputEvent event, float x, float y) {
        		changePerkDisplay(1);
        	}
        });
        perkGroup.addActor(arrow3);
        
        Image arrow4 = new Image(flipped);
        arrow4.setHeight(300);
        arrow4.setWidth(120);
        arrow4.setPosition(140, 320);
        arrow4.addListener(new ClickListener() {
        	@Override
	        public void clicked(InputEvent event, float x, float y) {
        		changePerkDisplay(-1);
        	}
        });
        perkGroup.addActor(arrow4);
        
        final Table navigationButtons = new Table();
		final TextButton showSkills = new TextButton("Skills", skin);
		showSkills.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					skillGroup.addAction(Actions.show());
					magicGroup.addAction(Actions.hide());
					perkGroup.addAction(Actions.hide());
		        }
			}
		);
		final TextButton showMagic = new TextButton("Magic", skin);
		showMagic.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					skillGroup.addAction(Actions.hide());
					magicGroup.addAction(Actions.show());
					perkGroup.addAction(Actions.hide());
		        }
			}
		);
		final TextButton showPerks = new TextButton("Perks", skin);
		showPerks.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					perkDisplay.get(perkSelection).addAction(Actions.show());
					skillGroup.addAction(Actions.hide());
					magicGroup.addAction(Actions.hide());
					perkGroup.addAction(Actions.show());
		        }
			}
		);
		final TextButton done = new TextButton("Done", skin);
		done.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					nextScene();		   
		        }
			}
		);
	
		navigationButtons.setPosition(1675, 75);
		navigationButtons.add(showSkills).size(125, 75);
		navigationButtons.add(showMagic).size(125, 75);
		navigationButtons.add(showPerks).size(125, 75).row();
		navigationButtons.add().size(50);
		navigationButtons.add(done).width(200);
		this.addActor(navigationButtons);
		
		magicGroup.addAction(Actions.hide());
		perkGroup.addAction(Actions.hide());
	}

	private void changeStanceDisplay(int delta) {
		allDisplay.get(stanceSelection).addAction(Actions.hide());
		stanceSelection += delta;
		if (stanceSelection < 0) stanceSelection += allDisplay.size;
		if (stanceSelection >= allDisplay.size) stanceSelection -= allDisplay.size;
		allDisplay.get(stanceSelection).addAction(Actions.show());
	}
	
	private void changePerkDisplay(int delta) {
		perkDisplay.get(perkSelection).addAction(Actions.hide());
		perkSelection += delta;
		if (perkSelection < 0) perkSelection += perkDisplay.size;
		if (perkSelection >= perkDisplay.size) perkSelection -= perkDisplay.size;
		perkDisplay.get(perkSelection).addAction(Actions.show());
	}
	
	private void handleNegativeSkillPoints() {
		while (skillPoints < 0) {
			for (Techniques technique : skills.keys()) {
				Integer cachedLevel = cachedSkills.get(technique, 0);
				Integer currentLevel = skills.get(technique, 0);
				if (currentLevel > cachedLevel) {
					skillPoints += currentLevel;
					skills.put(technique, --currentLevel);
					console.setText(console.getText() + (currentLevel == 0 ? "\nRemoved " + technique.getTrait().getName() + "." : "\nReduced " + technique.getTrait().getName() + " to Rank " + currentLevel +"."));
					techniquesToButtons.get(technique).setText(currentLevel == 0 ? "" : "(" + currentLevel + ")" );
					break;
				}
			}
		}
		skillPointsDisplay.setText("Skill Points: " + skillPoints);
	}
	
	private void nextScene() {
		character.setSkillPoints(skillPoints);
		character.setMagicPoints(magicPoints);
		character.setPerkPoints(perkPoints);
		character.setSkills(skills);
		character.setPerks(perks);
		saveService.saveDataValue(SaveEnum.PLAYER, character);
	
		sceneBranches.get(sceneBranches.orderedKeys().get(0)).setActive();
		isActive = false;
		addAction(Actions.hide());
	}
	
	private class StanceSkillDisplay extends Group {
		private final Stance stance;
		private final Table table;
		private StanceSkillDisplay(Stance stance, AssetManager assetManager) { 
			this.stance = stance;
			this.table = new Table();
			this.addActor(table);
			Image stanceIcon = new Image(assetManager.get(stance.getTexture()));
			stanceIcon.setPosition(-40, 115);
			stanceIcon.setScale(.75f);
			this.addActor(stanceIcon);
			init();
		}
		
		private void init() {			
			for (final Techniques technique: Techniques.values()) {
				if (!technique.isLearnable() || technique.getTrait().getUsableStance() != stance) continue;
				Integer level = skills.get(technique, 0);
				final Label label = new Label(technique.getTrait().getName(), skin);
				label.setAlignment(Align.right);
				label.setColor(Color.WHITE);
				
				final Label value = new Label(level > 0 ? "(" + level + ")" : "", skin);
				label.setAlignment(Align.right);
				
				techniquesToButtons.put(technique, value);
				final TextButton plusButton = new TextButton("+", skin);
				final TextButton minusButton = new TextButton("-", skin);
				
				final Row row = new Row(technique, label, skillDisplay, bonusDisplay, skillDisplayTable, consoleTable);
				
				label.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						locked = !locked;
						justUnlocked = !locked;
					}
				});
				label.addListener(getListener(row));
				plusButton.addListener(getListener(row));
				minusButton.addListener(getListener(row));
				plusButton.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
						if (skillPoints > 0) {
							Integer level = skills.get(technique);
							if (level == null) level = 0;
							if (level < technique.getMaxRank()) {
								if (level + 1 <= skillPoints) {
									skillPoints -= level + 1;
									skillPointsDisplay.setText("Skill Points: " + skillPoints);
									skills.put(technique, ++level);						
									console.setText("You have learned " + technique.getTrait().getName() + " Rank " + level +".");
									value.setText(level == 0 ? "" : "(" + level + ")");
								}
								else {
									console.setText("You do not have enough skill points!");
								}
							}
							else {
								console.setText("You cannot improve on that skill any further!");
							}	
						}
						else {
							console.setText("You have no skill points!");
						}
			        }
				});
				
				minusButton.addListener(new ClickListener() {
					@Override
			        public void clicked(InputEvent event, float x, float y) {
						buttonSound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
						
						Integer level = skills.get(technique, 0);
						if (level == 0) {
							console.setText("You do not yet possess that skill!");
						}
						else {
							Integer cachedLevel = cachedSkills.get(technique, 0);
							if (--level >= cachedLevel) {
								skillPoints += level + 1;
								skillPointsDisplay.setText("Skill Points: " + skillPoints);
								skills.put(technique, level);						
								if (level > 0) {
									console.setText("You have reduced " + technique.getTrait().getName() + " to Rank " + level +".");
								}
								else {
									console.setText("You have unlearned " + technique.getTrait().getName() + ".");
								}
								value.setText(level == 0 ? "" : "(" + level + ")");
							}
							else {
								console.setText("You cannot reduce " + technique.getTrait().getName() + " below Rank " + cachedLevel +".");
							}
						}
			        }
				});
				table.add();
				table.add(label).size(260, 45).padRight(10);
				table.add(value).size(30, 45).padRight(10);
				table.add(plusButton).size(45, 60);
				table.add(minusButton).size(45, 60).row();
			}
			table.align(Align.top);
		}
		
	}
	
	private class Row {
		private final Techniques technique;
		private final Perk perk;
		private final Label label;
		private final Label skillDisplay;
		private final Label bonusDisplay;
		private final Table skillDisplayTable;
		private final Table consoleTable;
		
		private Row(Techniques technique, Label label, Label skillDisplay, Label bonusDisplay, Table skillDisplayTable, Table consoleTable) {
			this(technique, null, label, skillDisplay, bonusDisplay, skillDisplayTable, consoleTable);
		}
		
		private Row(Perk perk, Label label, Label skillDisplay, Label bonusDisplay, Table skillDisplayTable, Table consoleTable) {
			this(null, perk, label, skillDisplay, bonusDisplay, skillDisplayTable, consoleTable);
		}
		
		private Row(Techniques technique, Perk perk, Label label, Label skillDisplay, Label bonusDisplay, Table skillDisplayTable, Table consoleTable) {
			this.technique = technique;
			this.perk = perk;
			this.label = label;
			this.skillDisplay = skillDisplay;
			this.bonusDisplay = bonusDisplay;
			this.skillDisplayTable = skillDisplayTable;
			this.consoleTable = consoleTable;
		}

		private void setSelected() {
			label.setColor(Color.FOREST);
			if (technique != null) {
				skillDisplay.setText(technique.getTrait().getDescription());
				bonusDisplay.setText(technique.getTrait().getBonusInfo());
			}
			else {
				skillDisplay.setText(perk.getDescription());
				bonusDisplay.setText("");
			}

			skillDisplayTable.addAction(Actions.show());
			consoleTable.addAction(Actions.hide());
		}		
		
		private void setUnselected() {
			label.setColor(Color.WHITE);
			consoleTable.addAction(Actions.show());
			skillDisplayTable.addAction(Actions.hide());
		}
	}
}
