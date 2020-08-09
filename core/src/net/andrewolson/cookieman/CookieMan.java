package net.andrewolson.cookieman;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Random;

public class CookieMan extends ApplicationAdapter {
	// Ads
	AdHandler handler;
	boolean toggle;

	public CookieMan(AdHandler handler) {
		this.handler = handler;
	}

	SpriteBatch batch;
	Texture background;
	float sourceX = 0;

	int score = 0;
	int prevScore = 0;
	int gameSpeed = 4;
	int gameState = 0;
	int lives = 3;
	int bombCollision = 0;
	BitmapFont bitmapFont;
	Preferences pref;

	// Coin Man
	Texture[] man;
	Texture manDizzy;
	Rectangle manRectangle;
	int manState = 0;
	int pause = 0;
	float gravity = 0.8f;
	float velocity = 0;
	int screenWidth = 0;
	int screenHeight = 0;
	int manY = 0;
	Random random;

	// Coins
	ArrayList<Integer> coinsXs = new ArrayList<Integer>();
	ArrayList<Integer> coinYs = new ArrayList<Integer>();
	ArrayList<Rectangle> coinRectangles = new ArrayList<Rectangle>();
	Texture coin;
	int coinCount;

	// Bombs
	ArrayList<Integer> bombXs = new ArrayList<Integer>();
	ArrayList<Integer> bombYs = new ArrayList<Integer>();
	ArrayList<Rectangle> bombRectangles = new ArrayList<Rectangle>();
	Texture bomb;
	int bombCount;

	// Sound
	Sound coinSound;
	Sound bombSound;
	Music music;

	@Override
	public void create () {
		batch = new SpriteBatch();
		pref = Gdx.app.getPreferences("net.andrewolson.cookieman");
		background = new Texture("bg.png");
		background.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

		screenWidth = Gdx.graphics.getWidth();
		screenHeight = Gdx.graphics.getHeight();

		// Coin Man Textures
		man = new Texture[4];
		manDizzy = new Texture("dizzy-1.png");
		man[0] = new Texture("frame-1.png");
		man[1] = new Texture("frame-2.png");
		man[2] = new Texture("frame-3.png");
		man[3] = new Texture("frame-4.png");
		manY = screenHeight / 2;

		// Coin / Bomb
		coin = new Texture("cookie_128.png");
		bomb = new Texture("carrot.png");
		random = new Random();
		manRectangle = new Rectangle();

		bitmapFont = new BitmapFont(Gdx.files.internal("btm_label.fnt"),Gdx.files.internal("btm_label.png"),false);
		bitmapFont.getData().setScale(4);
		bitmapFont.setColor(Color.WHITE);


		// Music
		coinSound= Gdx.audio.newSound(Gdx.files.internal("Coin_Collect.wav"));
		bombSound= Gdx.audio.newSound(Gdx.files.internal("Toxic_Bomb_Explosion.wav"));
		music = Gdx.audio.newMusic(Gdx.files.internal("City_Life.wav"));
		music.setVolume(0.5f);
		music.setLooping(true);
		music.play();
	}

	@Override
	public void render () {
		batch.begin();
		// Draw in the background
		int bgSpeed = (int) (gameSpeed * 1.75);
		if (gameState != 1) {
			bgSpeed = 0;
		}

		sourceX = (sourceX + Gdx.graphics.getDeltaTime() * bgSpeed) % background.getWidth();

		// you probably want to draw texture in full screen
		batch.draw(background,
				// position and size of texture
				0, 0, screenWidth, screenHeight,
				// srcX, srcY, srcWidth, srcHeight
				(int) sourceX, 0, background.getWidth(), background.getHeight(),
				// flipX, flipY
				false, false);

		if (gameState == 0) {
			handler.showAds(true);
			// Game is waiting to start
			if (Gdx.input.justTouched()) {
				gameState = 1;
				handler.showAds(false);
			}
			// Display the start screen
			drawStartScreen();

		} else if (gameState == 1) {
			// Game is active
			play();
		} else if (gameState == 2) {
			handler.showAds(true);
			// Game is waiting to start
			if (Gdx.input.justTouched()) {
				gameState = 1;
				handler.showAds(false);
				endGame();
			}
		} else if (gameState == 3) {
			// Dizzy state
			play();
		}

		if (gameState == 1 || gameState == 3) {
			// Draw the state labels
			bitmapFont.draw(batch, String.valueOf(score), 50, 150);
			bitmapFont.draw(batch,"Lives: " + String.valueOf(lives), 50, screenHeight - 20);
		}

		// Draw Coin Man
		int manWidth = man[manState].getWidth();
		int manHeight = man[manState].getHeight();

		if (gameState == 2 || gameState == 3) {
			batch.draw(manDizzy,(screenWidth  / 2) - manWidth / 2, manY);
			if (bombCollision > 0) {
				bombCollision --;
			} else {
				bombCollision = 0;
				if (gameState == 3) {
					gameState = 1;
				}
			}
			if (gameState == 2) {
				drawEndScreen();
			}

		} else {
			batch.draw(man[manState],(screenWidth  / 2) - manWidth / 2, manY);
		}

		manRectangle = new Rectangle(screenWidth / 2 - manWidth, manY, manWidth, manHeight);

		for (int i = 0; i < coinRectangles.size(); i++) {
			if (Intersector.overlaps(manRectangle, coinRectangles.get(i))) {
				Gdx.app.log("Coin!", "Collision");
				score ++;
				coinRectangles.remove(i);
				coinsXs.remove(i);
				coinYs.remove(i);

				// Sound
				long id = coinSound.play(1.0f);
				coinSound.setLooping(id,false);
				break;
			}
		}

		for (int i = 0; i < bombRectangles.size(); i++) {
			if (Intersector.overlaps(manRectangle, bombRectangles.get(i))) {
				Gdx.app.log("Bomb!", "Collision");
				// Remove the Bomb
				bombRectangles.remove(i);
				bombXs.remove(i);
				bombYs.remove(i);
				batch.draw(manDizzy,(screenWidth  / 2) - manWidth / 2, manY);


				// Sound
				long id = bombSound.play(1.0f);
				bombSound.setLooping(id,false);


				// Set Game state
				if (gameState == 1) {
					lives--;
				}
				if (lives <= 0) {
					gameState = 2;
				} else {
					gameState = 3;
					bombCollision = 85;
				}
			}
		}
		batch.end();
	}

	public void play() {
		// Bomb
		if (bombCount < setBombCount()) {
			bombCount ++;
		} else {
			bombCount = 0;
			makeBomb();
		}

		bombRectangles.clear();

		gameSpeed = getSpeed();

		for (int i = 0; i < bombXs.size(); i++) {
			batch.draw(bomb, bombXs.get(i), bombYs.get(i));
			bombXs.set(i, bombXs.get(i) - gameSpeed - 4);
			Rectangle bombRectangle = new Rectangle(bombXs.get(i), bombYs.get(i), bomb.getWidth() * 0.85f, bomb.getHeight() * 0.85f);
			bombRectangles.add(bombRectangle);
		}

		// Coin
		if (coinCount < setCoinCount()) {
			coinCount ++;
		} else {
			coinCount = 0;
			makeCoin();
		}
		coinRectangles.clear();
		for (int i = 0; i < coinsXs.size(); i++) {
			batch.draw(coin, coinsXs.get(i), coinYs.get(i));
			coinsXs.set(i, coinsXs.get(i) - gameSpeed);
			Rectangle coinRectangle = new Rectangle(coinsXs.get(i), coinYs.get(i), coin.getWidth(), coin.getHeight());
			coinRectangles.add(coinRectangle);
		}


		// Allow Coin man to jump up and down
		if (Gdx.input.justTouched() && ! (gameState == 3)) {
			velocity = -25;
		}

		// Slow the movement down
		if(pause < 4) {
			pause ++;
		} else {
			pause = 0;
			if(manState < 3) {
				manState ++;
			} else {
				manState = 0;
			}
		}

		// Allows Coin man to fall at a quicker rate
		velocity = velocity + gravity;
		manY -= velocity;

		// Keep Coin Man on the screen
		if (manY <=0) {
			manY = 0;
		} else if (manY > screenHeight) {
			manY = screenHeight;
		}
	}

	public void endGame() {
		score = 0;
		prevScore = 0;
		lives = 3;
		bombCollision = 0;
		velocity = 0;
		coinCount = 0;
		bombCount = 0;
		gameSpeed = 4;
		manY = screenHeight / 2;
		coinsXs.clear();
		coinYs.clear();
		coinRectangles.clear();
		bombXs.clear();
		bombYs.clear();
		bombRectangles.clear();
	}

	public void makeCoin() {
		float height = random.nextFloat() * screenHeight - coin.getHeight();
		if (height > screenHeight){
			height = screenHeight - coin.getHeight() - 20;
		}
		if (height <= 0){
			height = coin.getHeight() + 20;
		}

		coinYs.add((int) height);
		coinsXs.add(screenWidth);
	}

	public void makeBomb() {
		float height = random.nextFloat() * screenHeight;
		if (height > screenHeight){
			height = screenHeight - bomb.getHeight() - 20;
		}
		if (height <= 0){
			height = bomb.getHeight() + 20;
		}
		bombYs.add((int) height);
		bombXs.add(screenWidth);
	}
	public int getSpeed() {
		int speed = gameSpeed;
		if (score > prevScore && score != 0 && score % 10 == 0) {
			speed += 2;
			prevScore = score;
		}

		return speed;
	}

	public int setBombCount() {
		int bombs = 250;
		if (score > prevScore && score != 0 && score % 10 == 0) {
			bombs -= 1;
			if (bombs < 10) {
				bombs = 10;
			}
		}

		return bombs;
	}

	public int setCoinCount() {
		int coins = 100;
		if (score > prevScore && score != 0 && score % 10 == 0) {
			coins -= 2;
			if (coins < 10) {
				coins = 10;
			}
		}

		return coins;
	}
	public void drawStartScreen() {
		// Draw the state labels
		int highScore = getHighScore();
		displayText("High Score " + String.valueOf(highScore), 0, 0, false);
		displayText("Press to Start Game", 0, -100, false);
	}
	public void drawEndScreen() {
		// Draw the state labels
		int highScore = getHighScore();
		if (highScore <= score) {
			highScore = score;
			setHighScore(highScore);
		}
		displayText("High Score " + String.valueOf(highScore), 0, 0, false);
		displayText("Score" + String.valueOf(score), 0, -100, false);
	}
	public void displayText(String message,float x, float y, boolean bottom) {
		// Set up game Label
		if (! bottom) {
			GlyphLayout layout = new GlyphLayout(bitmapFont, message);
			float fontWidth = layout.width;
			float fontHight = layout.height;
			float position_x = (screenWidth / 2 - fontWidth / 2) + x;
			float position_y = (screenHeight / 2 - fontHight / 2) + y;
			bitmapFont.draw(batch, message, position_x, position_y);

		} else {
			bitmapFont.draw(batch, message, x, y);
		}
	}
	public int getHighScore() {
		pref.flush();
		int highScore = pref.getInteger("highScore", 0);
		return highScore;
	}
	public void setHighScore(int highScore) {
		if (highScore <= score) {
			highScore = score;
			pref.putInteger("highScore", highScore);
			pref.flush();
		}
	}

	@Override
	public void dispose () {
		batch.dispose();
		music.dispose();
		bombSound.dispose();
		coinSound.dispose();
	}
}
