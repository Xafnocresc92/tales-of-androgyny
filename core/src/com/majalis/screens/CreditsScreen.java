package com.majalis.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.majalis.asset.AssetEnum;

public class CreditsScreen extends AbstractScreen{
	public static final Array<AssetDescriptor<?>> resourceRequirements = new Array<AssetDescriptor<?>>();
	static {
		resourceRequirements.add(AssetEnum.UI_SKIN.getSkin());
		resourceRequirements.add(AssetEnum.BUTTON_SOUND.getSound());
		resourceRequirements.add(AssetEnum.MAIN_MENU_MUSIC.getMusic());
		resourceRequirements.add(AssetEnum.CAMP_BG0.getTexture());
		resourceRequirements.add(AssetEnum.CAMP_BG1.getTexture());
		resourceRequirements.add(AssetEnum.CAMP_BG2.getTexture());
	}
	
	private final String credits;
	protected CreditsScreen(ScreenFactory screenFactory, ScreenElements elements) {
		super(screenFactory, elements, null);
		this.addActor(getCampBackground());
		Skin skin = assetManager.get(AssetEnum.UI_SKIN.getSkin());
		final Sound sound = assetManager.get(AssetEnum.BUTTON_SOUND.getSound());
		final TextButton done = new TextButton("Done", skin);
		
		done.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					sound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					exitScreen();	   
		        }
			}
		);
		done.setWidth(150);
		done.setPosition(1623, 50);
		this.addActor(done);
		
		final TextButton patrons = new TextButton("Patrons", skin);
		
		patrons.addListener(
			new ClickListener() {
				@Override
		        public void clicked(InputEvent event, float x, float y) {
					sound.play(Gdx.app.getPreferences("tales-of-androgyny-preferences").getFloat("volume") *.5f);
					showScreen(ScreenEnum.PATRON);	   
		        }
			}
		);
		patrons.setWidth(150);
		patrons.setPosition(1423, 50);
		this.addActor(patrons);
		
		credits = "\"Broken Reality\", \"Perspectives\", \"Floating Cities\", \"Enchanted Valley\", \"Brittle Rille\", \"For Originz\", \"Phantom from Space\",\n\"Mechanolith\", \"Fearless First\", \"Danger Storm\", \"Killers\", \"One-eyed Maestro\", \"Immersed\", \"Shadowlands 3 - Machine\", \n\"Temple of the Manes\", \"Ascending the Vale\", \"Division\""
				+ "\nKevin MacLeod (incompetech.com)"
				+ "\nLicensed under Creative Commons: By Attribution 3.0"
				+ "\nhttp://creativecommons.org/licenses/by/3.0/"
				+ "\n\nSuccubus-Tier patrons that helped make this happen:"
				+ "\nDarksideX, JennaTran, Joel Fields, Laersect, and special thanks to crufl and Ace for moral support, advice and friendship! Love you both!"
				+ "\nMucho kudos to official contributor Dern for programming, debugging and support!"
				+ "\nAlso special thanks to NobleIntentions and UpsideDownArbys for listening to Maj's frustrations!"				
				+ "\n\nCopy-editing and play-testing by T3mp3st, Legion, Fattycakes, and anonymous others"
				+ "\nAlso thanks to all of our patrons, who are making this possible!"
				+ "\nDragon-Tier+ patrons: The Daskling"
				;
	}

	@Override
	public void buildStage() {
		
	}
	
	@Override
	public void render(float delta) {
		super.render(delta);
		OrthographicCamera camera = (OrthographicCamera) getCamera();
		batch.setTransformMatrix(camera.view);
		camera.update();
		batch.begin();
		font.setColor(Color.WHITE);
		font.draw(batch, credits, 1100, 1300);
		batch.end();
		
		if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
			exitScreen();
		}
	}
	
	private void exitScreen() {
		showScreen(ScreenEnum.MAIN_MENU);
	}

	
	@Override
	public void show() {
		super.show();
	    getRoot().getColor().a = 0;
	    getRoot().addAction(Actions.fadeIn(0.5f));
	}
	
	@Override
	public void dispose() {
		for(AssetDescriptor<?> path: resourceRequirements) {
			if (path.fileName.equals(AssetEnum.BUTTON_SOUND.getSound().fileName) || path.type == Music.class) continue;
			assetManager.unload(path.fileName);
		}
	}
}
