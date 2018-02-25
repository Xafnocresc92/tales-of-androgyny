package com.majalis.character;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class HealthBar extends Group {
	private final AssetManager assetManager;
	private final AbstractCharacter character;
	private final ProgressBar bar;
	private final Image icon;
	private final Label label;
	public HealthBar(AbstractCharacter character, AssetManager assetManager, Skin skin) {
		this.character = character;
		this.assetManager = assetManager;
		bar = new ProgressBar(0, 1, .01f, false, skin);
		bar.setWidth(350);
		bar.setValue(character.getHealthPercent());
		this.addActor(bar);
		
		icon = new Image(assetManager.get(character.getHealthDisplay()));
		icon.setPosition(3, 7.5f);
		this.addActor(icon);
		
		label = new Label(character.getCurrentHealth() + " / " + character.getMaxHealth(), skin);
		label.setColor(Color.BROWN);
		label.setPosition(75, 8);
		this.addActor(label);
		bar.setColor(character.getHealthColor());
	}
	
	@Override
	public void draw(Batch batch, float parentAlpha) {
		float characterHealthPercent = character.getHealthPercent();
		if(Math.abs(bar.getValue() - characterHealthPercent) > .01) {
			if (bar.getValue() < characterHealthPercent) {
				bar.setValue(bar.getValue() + .01f);
			}
			else {
				bar.setValue(bar.getValue() - .01f);
			}
		}
		icon.setDrawable(new TextureRegionDrawable(new TextureRegion(assetManager.get(character.getHealthDisplay()))));
		label.setText(character.getCurrentHealth() + " / " + character.getMaxHealth());
		bar.setColor(character.getHealthColor());
		super.draw(batch, parentAlpha);
	}
}