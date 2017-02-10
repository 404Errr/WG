package wg;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class WG extends JFrame {
	public static final String version = "v0";
	private static final String Title = "You This Read Wrong";
	public static boolean mouse = true, editMode = false, grid = false, gridType = false, miniMap = true, fullInventory = false, aiPlayers = true, autoEdge = false, notTheAimbot = true, debugText = false, fFire = true;
	public static final int FPS = 60, gameSpeed = 1000/FPS,  width = 1850+7, height = 950+29, cellSize = 32, playerSize = cellSize, collisionZoneSize = 2, healthMax = 10000, itemCooldown = 10;
	public static int wasdBind = 0, arrowBind = (aiPlayers)?-1:1, gameTick, stationaryX, stationaryY, windowX = 1, windowY = 1, visiblePlayerCount = 1, playerCount = 2;
	public static ArrayList<Panel> panels = new ArrayList<>();
	public static ArrayList<Player> players = new ArrayList<>();
	static JFrame frame;
	static Input inputs;
	public static boolean running = true;
	public static double dUpdate;
	public static final double moveSpeedChoice = 0.16d, moveSpeed = moveSpeedChoice*cellSize, playerMass = 68, healthRegen = 1.5d;

	public static final int[] genSizeX = {5,6}, genSizeY = {4,5};
	public static long SEED = 0L;//0 for random
	public static final int[][] selectedLevel = /*LevelLayout.test;//*/LevelGen.generateLevel(true, false);//LevelGen.editSegment();//LevelGen.generateLevel(true);//<set that to false

	public static void main(String[] args) throws InterruptedException {
		run();
	}

	public static void run() throws InterruptedException {
		if (editMode) {windowX = 1;windowY = 1;playerCount = 1;visiblePlayerCount = 1;}
		stationaryX = WG.width/WG.windowX/2;
		stationaryY = WG.height/WG.windowY/2;
		new LevelLayout();
		new Level();
		for (int i = 0;i<playerCount;i++) {
			panels.add(new Panel(i));
			new Thread(panels.get(i)).start();
			if (aiPlayers&&i!=wasdBind) players.add(new Player(i, true));
			else players.add(new Player(i, false));
		}
		frame = new JFrame();
		inputs = new Input();
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.setLayout(new GridLayout(windowY, windowX));
		frame.setSize(width, height);
		for (int i = 0;i<visiblePlayerCount;i++) frame.add(panels.get(i));
		frame.setTitle(Title/*+" "+version*/);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.addKeyListener(inputs);
		frame.addMouseMotionListener(inputs);
		frame.addMouseListener(inputs);
		frame.addMouseWheelListener(inputs);
		frame.setVisible(true);
		long initTime = System.currentTimeMillis();
		long startTime = 0, endTime = 0, timeChange = 0, wait;
		while (running) {
			timeChange = Math.abs(startTime-endTime);
			wait = gameSpeed-timeChange;
			if (wait>0) Thread.sleep(wait);
			startTime = System.currentTimeMillis()-initTime;
			dUpdate = 1+(double)timeChange/(double)gameSpeed*5/8;

		    if (Bullet.bullets.size()>0) for (int i = 0;i<Bullet.bullets.size();i++) Bullet.bullets.get(i).tick();
		    if (Grenade.grenades.size()>0) for (int i = 0;i<Grenade.grenades.size();i++) Grenade.grenades.get(i).tick();
		    if (Shard.shards.size()>0) for (int i = 0;i<Shard.shards.size();i++) Shard.shards.get(i).tick();
			if (Item.items.size()>0) for (Item item:Item.items) item.tick();
			if (Explosion.explosions.size()>0) for (int i = 0;i<Explosion.explosions.size();i++) Explosion.explosions.get(i).tick();
			for (Player player:players) player.tick();

			if (editMode) {
				Edit.tick();
				if (gameTick%1000==0) if (Segment.editSegment) Edit.printSegment(Edit.name);else Edit.printLevel();
			}
			gameTick++;
			endTime = System.currentTimeMillis()-initTime;
			if (debugText) System.out.println("Main|"+wait+"|"+timeChange+"|"+(float)dUpdate);
		}
	}
}

@SuppressWarnings("serial")
class Panel extends JPanel implements Runnable {
	int id, panelX = (WG.width-7)/WG.windowX, panelY = (WG.height-30)/WG.windowY, renderDistanceX, renderDistanceY, flashTimer, cellSize = WG.cellSize;
	static int sizeMM = 2+(3/Level.sizeX);
	static final double iconSizeMultiplyer = 0.2d;
	final static String flashText = "*obnoxious high-pitched ringing*";
	final static int explosionGrowthRate = 30;
	final int ringSize = ((panelY+panelX)/2)/5, ringWidth = ringSize/5, iconSize = (int) (iconSizeMultiplyer*ringSize), borderSize = 3, centerBarSize = 100, centerBarWidth = 5;
	double lastReload;
	boolean reloadSaved;
	Color hudColor = Color.DARK_GRAY;

	private static BufferedImage floor0, floor1, floor2, floor3, floor4, floor5, floor6, wall, window, shield, crate, item, rocket, c4;

	static {loadTextures();}
	static void loadTextures() {
		try {
//			final String rootDir = "S:\\Eclipse/WG/";
			final String rootDir = "src/data/";
			floor0 = ImageIO.read(new File(rootDir+"floor0.png"));
			floor1 = ImageIO.read(new File(rootDir+"floor1.png"));
			floor2 = ImageIO.read(new File(rootDir+"floor2.png"));
			floor3 = ImageIO.read(new File(rootDir+"floor3.png"));
			floor4 = ImageIO.read(new File(rootDir+"floor4.png"));
			floor5 = ImageIO.read(new File(rootDir+"floor5.png"));
			floor6 = ImageIO.read(new File(rootDir+"floor6.png"));
			wall = ImageIO.read(new File(rootDir+"wall.png"));
			window = ImageIO.read(new File(rootDir+"window.png"));
			shield = ImageIO.read(new File(rootDir+"shield.png"));
			crate = ImageIO.read(new File(rootDir+"crate.png"));
			item = ImageIO.read(new File(rootDir+"item.png"));
			c4 = ImageIO.read(new File(rootDir+"c4.png"));
			rocket = ImageIO.read(new File(rootDir+"rocket.png"));
		} catch (Exception e) {e.printStackTrace();System.out.println("Pictures no worky work.");}
	}

	@Override
	public void run() {
		long initTime = System.currentTimeMillis();
		long startTime = 0, endTime = 0, timeChange = 0, wait;
		while (WG.running) {
			timeChange = Math.abs(startTime-endTime);
			wait = WG.gameSpeed-timeChange;
			if (wait>0) try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {}
			startTime = System.currentTimeMillis()-initTime;
		    repaint();
			endTime = System.currentTimeMillis()-initTime;
			if (WG.debugText) System.out.println("Panel: "+id+"|"+wait+"|"+timeChange);
		}
	}

	Panel(int id) {
		this.id = id;
		//if (Main.playerCount==1) panelY-=108;
		if (WG.playerCount>2) panelY+=18;
		if (WG.playerCount==2&&WG.windowY==2) panelY+=15;
		if (WG.playerCount==2&&WG.windowX==2) panelX+=2;
	}

	@Override
	protected void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		this.setBackground(Color.GRAY);
		Graphics2D g = (Graphics2D) g0;
		renderDistanceX = panelX/cellSize/2+4;
		renderDistanceY = panelY/cellSize/2+4;
		drawCells(g);
		drawItems(g);
		drawC4(g);
		drawBullets(g);
		drawGrenades(g);
		drawShards(g);
		drawOthersGun(g);
		drawPlayers(g, false);
		if (!WG.editMode) drawOwnGun(g);
		drawPlayers(g, true);
		drawExplosions(g);
		drawInventory(g);
		drawHealthWheel(g);
		drawHealthText(g);
		drawReload(g);
		drawAmmo(g);
		drawDeaths(g);
		drawCloakBar(g);
		if (WG.miniMap) drawMiniMap(g);
		flashed(g);
		drawBorders(g);
		if (WG.debugText) drawDebugText(g);
		if (WG.editMode) drawEditType(g);
	}

	private void drawEditType(Graphics2D g) {
		int type = Input.blocks[Input.currentBlockList][Input.currentBlock], type1, type2 = 0;
		if (String.valueOf(type).length()>2) {
			String[] tempType = new String[4];
			for (int k = 0;k<4;k++) tempType[k] = String.valueOf(type).charAt(k)+"";
			type1 = Integer.valueOf(tempType[0]+tempType[1]);
			type2 = Integer.valueOf(tempType[2]+tempType[3]);
			g.drawImage(getImage(type1), WG.stationaryX+WG.cellSize/2, 10, null);
			if (type2!=90&&type2!=91) g.drawImage(getImage(type2), WG.stationaryX+WG.cellSize/2, 10, null);
		}
		else g.drawImage(getImage(type), WG.stationaryX+WG.cellSize/2, 10, null);
		StringBuilder subType = new StringBuilder();
		if (type==0) subType.append("VOID ");
		if (type==1) subType.append("WALL ");
		if (type==2) subType.append("CRATE ");
		if (type>=10&&type<=20) subType.append("FLOOR "+(type+"").charAt(1));
		if (type2==91) subType.append("ITEMSPAWN "+(type+"").charAt(1));
		if (type2==90) subType.append("PLAYERSPAWN "+(type+"").charAt(1));
		if (type2==20) subType.append("WINDOW "+(type+"").charAt(1));
		if (type2==21) subType.append("SHIELD "+(type+"").charAt(1));
		g.setColor(Color.WHITE);
		g.setFont(new Font("Helvetica", Font.BOLD, 10));
		g.drawString(subType+"", WG.stationaryX+WG.cellSize*2+3, 10+WG.cellSize);
	}

	static BufferedImage getImage(int type) {
		switch (type) {
			case 10: return floor0;
			case 11: return floor1;
			case 12: return floor2;
			case 13: return floor3;
			case 14: return floor4;
			case 15: return floor5;
			case 16: return floor6;
			case 1: return wall;
			case 0: {break;}
			case 20: return window;
			case 21: return shield;
			case 2: return crate;
			case 91: return floor0;
			case 90: return floor0;
		}
		return null;
	}

	void drawCells(Graphics2D g) {
		int iX = (int)WG.players.get(id).x/cellSize-renderDistanceX, iY = (int)WG.players.get(id).y/cellSize-renderDistanceY, fX = (int)WG.players.get(id).x/cellSize+renderDistanceX, fY = (int)WG.players.get(id).y/cellSize+renderDistanceY;
		for (int x = iX;x<fX;x++) for (int y = iY;y<fY;y++) {
			if(x>=0 && y>=0 && x<Level.sizeX && y<Level.sizeY) {
				int i = (int) (Level.cells[x][y].gX*cellSize-WG.players.get(id).x+Player.stationaryX), j = (int) (Level.cells[x][y].gY*cellSize-WG.players.get(id).y+Player.stationaryY);
				int type = Level.cells[x][y].type;
				if (String.valueOf(type).length()>2) {
					String[] tempType = new String[4];
					for (int k = 0;k<4;k++) tempType[k] = String.valueOf(type).charAt(k)+"";
					int type1 = Integer.valueOf(tempType[0]+tempType[1]), type2 = Integer.valueOf(tempType[2]+tempType[3]);
					g.drawImage(getImage(type1), i, j, null);
					if (type2!=90&&type2!=91) g.drawImage(getImage(type2), i, j, null);
				}
				else g.drawImage(getImage(type), i, j, null);
				if (WG.grid) {
					g.setColor(Color.WHITE);
					g.drawRect((int) (Level.cells[x][y].gX*cellSize-WG.players.get(id).x+Player.stationaryX), (int) (Level.cells[x][y].gY*cellSize-WG.players.get(id).y+Player.stationaryY), cellSize, cellSize);
					if (WG.gridType) {g.setFont(new Font("Helvetica", Font.PLAIN, 10));g.setColor((Level.cells[x][y].notWalkonable)?Color.GREEN:Color.RED);g.drawString(Level.cells[x][y].type+"",(int) (Level.cells[x][y].gX*cellSize-WG.players.get(id).x+Player.stationaryX)+2, (int) (Level.cells[x][y].gY*cellSize-WG.players.get(id).y+Player.stationaryY)+cellSize-2);}
				}
			}
		}
	}

	private void drawCloakBar(Graphics2D g) {
		if (WG.players.get(id).itemType==Type.CLOAK&&WG.players.get(id).cloak<UtilityType.cloakDuration) {
			g.setColor(findColor(50, 50, 50, 10));
			g.setStroke(new BasicStroke(centerBarWidth));
			g.drawArc(Player.stationaryX-centerBarSize/2, Player.stationaryY-centerBarSize/2, centerBarSize, centerBarSize, 90, (int)(WG.players.get(id).cloak/UtilityType.cloakDuration*360));
		}
	}

	private void drawMiniMap(Graphics2D g) {
		double miniMapCellSize = sizeMM;//(double)panelY/4d/(double)Level.sizeY;
		int miniMapBorder = 3, originX = 0, originY = 0;
		switch (id) {
			case 0: {originX = borderSize;break;}
			case 1: {originX = (int) (panelX-miniMapCellSize*Level.sizeX-miniMapBorder);break;}
		}
		g.setColor(hudColor);
		g.fillRect(originX-3, originY, (int)(miniMapCellSize*Level.sizeX+miniMapBorder)+3, (int)(miniMapCellSize*Level.sizeY+miniMapBorder)+3);
		originY+=miniMapBorder;
		for (int x = 0;x<Level.sizeX;x++) for (int y = 0;y<Level.sizeY;y++) {
			g.setColor(Color.LIGHT_GRAY);
			switch (Level.cells[x][y].type) {
				case 1: {g.setColor(new Color(0x000000));break;}
				case 0: {g.setColor(new Color(0x808080));break;}
				case 21: {g.setColor(new Color(0xaaaaaa));break;}
				case 20: {g.setColor(new Color(0xcccccc));break;}
				case 2: {g.setColor(new Color(0x666666));break;}
				case 91: {if (WG.editMode) {g.setColor(Color.ORANGE);}break;}
			}
			g.fillRect((int) (Level.cells[x][y].gX*miniMapCellSize+originX), (int) (Level.cells[x][y].gY*miniMapCellSize+originY), (int)miniMapCellSize, (int)miniMapCellSize);
		}
		g.setColor(getColor(id, 100));
		g.fillRect((int) ((WG.players.get(id).x/cellSize*miniMapCellSize+originX)-miniMapCellSize/2), (int) ((WG.players.get(id).y/cellSize*miniMapCellSize+originY)-miniMapCellSize/2), (int)miniMapCellSize, (int)miniMapCellSize);//player
		g.setStroke(new BasicStroke(1));
		g.drawRect((int) (WG.players.get(id).x/cellSize*miniMapCellSize+originX-(renderDistanceX-2)*miniMapCellSize), (int) (WG.players.get(id).y/cellSize*miniMapCellSize+originY-(renderDistanceY-2)*miniMapCellSize), (int)(miniMapCellSize+(renderDistanceX-2)*2*miniMapCellSize), (int)(miniMapCellSize+(renderDistanceY-2)*2*miniMapCellSize));//player
	}

	private void flashed(Graphics2D g) {
		if (flashTimer>0) {
			int opacity;
			if (flashTimer<GrenadeType.fDropOff) opacity = (int) ((double)flashTimer/(double)GrenadeType.fDropOff*100d);
			else opacity = 99;
			g.setColor(findColor(255, 255, 255, opacity));
			g.fillRect(0, 0, WG.width, WG.height);
			g.setColor(findColor(0, 0, 0, opacity));
			g.setFont(new Font("Helvetica", Font.BOLD, panelX/20));
			g.drawString(flashText,panelX/6,panelY/2);
			flashTimer--;
		}
	}

	private void drawAmmo(Graphics2D g) {
		int originX = 0, originY = 30;
		switch (id) {
			case 0: {originX = panelX-ringSize*7/16; originY = panelY-ringSize/32-ringSize/6;break;}
			case 1: {originX = borderSize+ringSize/32; originY = panelY-ringSize/32-ringSize/6;break;}
		}
		g.setColor(Color.WHITE);
		g.setFont(new Font("Helvetica", Font.BOLD, ringSize/10));
		g.drawString(WG.players.get(id).ammoMag[WG.players.get(id).currentType]+"/"+(WG.players.get(id).ammoPool[WG.players.get(id).currentType]-WG.players.get(id).ammoMag[WG.players.get(id).currentType])/*+"("+Main.players.get(id).ammoPool[Main.players.get(id).currentAmmoType]+")"*/, originX, originY);
	}

	private void drawDeaths(Graphics2D g) {
		int originX = 0, originY = 30;
		switch (id) {
			case 0: {originX = panelX-ringSize/8; originY = panelY-ringSize/16-ringSize/4;break;}
			case 1: {originX = borderSize+ringSize/24; originY = panelY-ringSize/16-ringSize/4;break;}
		}
		g.setColor(Color.WHITE);
		g.setFont(new Font("Helvetica", Font.BOLD, ringSize/10));
		g.drawString(WG.players.get(id).deaths+"", originX, originY);
	}

	private void drawReload(Graphics2D g) {
		if (WG.players.get(id).reload) {
			if (!reloadSaved) {
				lastReload = WG.players.get(id).gunCooldown;
				reloadSaved = true;
			}
			double reload = (lastReload-WG.players.get(id).gunCooldown)/lastReload*360;
			g.setColor(findColor(100, 100, 100, 20));
			g.setStroke(new BasicStroke(centerBarWidth));
			g.drawArc(Player.stationaryX-centerBarSize/2, Player.stationaryY-centerBarSize/2, centerBarSize, centerBarSize, 90, (int)reload);
		}
	}

	private void drawInventory(Graphics2D g) {
		int originX = 0, originY = 0, length = iconSize*Type.types.size();
		switch (id) {
			case 0: {originX = panelX-ringSize-length; originY = panelY-iconSize;break;}
			case 1: {originX = ringSize+borderSize; originY = panelY-iconSize;break;}
		}
		g.setColor(hudColor);
		if (id==0||id==2) g.fillRect(originX, originY, length+iconSize*2, iconSize);
		if (id==1||id==3) g.fillRect(originX-iconSize*2, originY, length+iconSize*2, iconSize);
		for (int i = 0;i<Type.types.size();i++) drawIcon(g, originX+iconSize*i, originY, iconSize, Type.types.get(i));
	}

	private void drawIcon(Graphics2D g, int x, int y, int size, Type type) {
		g.setColor(new Color(0x555555));
		if (WG.players.get(id).inventory.contains(type)) g.setColor(new Color(0xaaaaaa));
		if (WG.players.get(id).itemType==type) g.setColor(new Color(0xeeeeee));
		g.fillRect(x+2, y+2, size-5, size-5);
		g.setColor(new Color(0x303030));
		switch (type) {
			case PISTOL: {drawPistolIcon(g, x, y, size);break;}
			case SHOTGUN: {drawShotgunIcon(g, x, y, size);break;}
			case AR: {drawARIcon(g, x, y, size);break;}
			case RAIL: {drawRailIcon(g, x, y, size);break;}
			case MINIGUN: {drawMinigunIcon(g, x, y, size);break;}
			case ROCKET: {drawRocketIcon(g, x, y, size);break;}
			case FRAG: {drawFragIcon(g, x, y, size);break;}
			default: {break;}
		}
	}
	private void drawFragIcon(Graphics2D g, int x, int y, int size) {
		g.fillOval(x+size/3, y+size/3, size/3, size/2);
		g.fillRect(x+size/3, y+size/4, size/3, size/8);
	}
	private void drawPistolIcon(Graphics2D g, int x, int y, int size) {
		g.fillOval(x+size/3, y+size/3, size/3, size/3);
	}
	private void drawShotgunIcon(Graphics2D g, int x, int y, int size) {
		for (int i = 0;i<3;i++) for (int j = 0;j<3;j++) g.fillOval(x+(size*1/7)+(i*size*2/7), y+(size*1/7)+(j*size*2/7), size*1/7, size*1/7);
	}
	private void drawARIcon(Graphics2D g, int x, int y, int size) {
		for (int i = 0;i<3;i++) g.fillOval(x+(size*3/7), y+(size*1/7)+(i*size*2/7), size*1/7, size*1/7);
	}
	private void drawRailIcon(Graphics2D g, int x, int y, int size) {
		g.setStroke(new BasicStroke(size/15));
		g.drawLine(x+size/2, y+size/8, x+size/2, y+size*7/8);
	}
	private void drawMinigunIcon(Graphics2D g, int x, int y, int size) {
		g.setStroke(new BasicStroke(size/30));
		for (int i = 0;i<size/4;i++) g.drawLine(x+size*3/8+i, y+size*3/16, x+size/2, y+size*13/16);
	}
	private void drawRocketIcon(Graphics2D g, int x, int y, int size) {
		g.fillOval(x+size/3, y+size/12, size/3, size/3);
		for (int i = 0;i<2;i++) g.fillOval(x+(size*3/7), y+(size*7/14)+(i*size*3/14), size*1/7, size*1/7);
	}

	private void drawExplosions(Graphics2D g) {
		g.setColor(Color.WHITE);
		for (int i = 0;i<Explosion.explosions.size();i++) if (!Explosion.explosions.get(i).flash) g.fillOval((int) (Explosion.explosions.get(i).x-WG.players.get(id).x+Player.stationaryX-Explosion.explosions.get(i).size/2), (int) (Explosion.explosions.get(i).y-WG.players.get(id).y+Player.stationaryY-Explosion.explosions.get(i).size/2), (int) (Explosion.explosions.get(i).size), (int) (Explosion.explosions.get(i).size));
	}

	private void drawHealthText(Graphics2D g) {
		int originX = 0, originY = 30;
		switch (id) {
			case 0: {originX = panelX-ringSize*7/16; originY = panelY-ringSize/32;break;}
			case 1: {originX = borderSize+ringSize/32; originY = panelY-ringSize/32;break;}
		}
		g.setColor(Color.WHITE);
		g.setFont(new Font("Helvetica", Font.BOLD, ringSize/6));
		StringBuilder health = new StringBuilder();
		if (WG.players.get(id).health>=10) {
			for (int i = 0;i<String.valueOf(WG.healthMax).length()-1-String.valueOf((int)WG.players.get(id).health).length();i++) health.append("  ");
			health.append((int)(WG.players.get(id).health/10d));
		}
		if (WG.players.get(id).health<10) {
			g.setFont(new Font("Helvetica", Font.BOLD, ringSize/12));
			originY-=ringSize/24;
			health.append("A little bit");
		}
		g.drawString(health.toString(), originX, originY);
	}

	private void drawHealthWheel(Graphics2D g) {
		int originX = panelX-1, originY = panelY, r = 90, d = -1;
		switch (id) {
			case 0: {originX = panelX-1;originY = panelY;r = 180;d = -1;break;}
			case 1: {originX = 1;originY = panelY;r = 0;d = 1;break;}
		}
		g.setStroke(new BasicStroke(ringWidth));
		g.setColor(hudColor);
		g.fillOval(originX-(ringSize+4), originY-(ringSize+4), 2*(ringSize+4), 2*(ringSize+4));
		g.setColor(healthColor(WG.players.get(id).health, id));
		if (WG.players.get(id).health<WG.healthMax) g.drawArc(originX-(ringSize-ringWidth/2), originY-(ringSize-ringWidth/2), 2*(ringSize-ringWidth/2), 2*(ringSize-ringWidth/2), r, (int) (d*WG.players.get(id).health/WG.healthMax*90));
		else g.fillOval(originX-ringSize, originY-ringSize, 2*ringSize, 2*ringSize);
		g.setColor(hudColor);
		g.fillOval(originX-(ringSize-ringWidth), originY-(ringSize-ringWidth), 2*(ringSize-ringWidth), 2*(ringSize-ringWidth));
	}

	static Color healthColor(double health, int player) {
		int r = 255, g = 255;
		if (WG.players.get(player).spawning) return new Color(0x909090);
		r = (int) (255-(health/WG.healthMax*255));
		g = (int) (health/WG.healthMax*255);
		if (health>WG.healthMax) {r = 0; g = 255;}
		if (health<0) {r = 255; g = 0;}
		return findColor(r, g, 0, 100);
	}

	private void drawDebugText(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.setFont(new Font("Helvetica", Font.PLAIN, 20));
		g.drawString(WG.players.get(id).itemType.type+"|"+WG.players.get(id).c4Placed+"|"+WG.players.get(id).cloakOn+"|"+WG.players.get(id).shoot+"|"+(float) (WG.players.get(id).x/cellSize)+"("+(int) (WG.players.get(id).x/15/cellSize)+")|"+(float) (WG.players.get(id).y/cellSize)+"("+(int) (WG.players.get(id).y/15/cellSize)+")|"+WG.players.get(id).ammoMag[WG.players.get(id).currentType]+","+(WG.players.get(id).ammoPool[WG.players.get(id).currentType]-WG.players.get(id).ammoMag[WG.players.get(id).currentType])+"("+WG.players.get(id).ammoPool[WG.players.get(id).currentType]+")|"+WG.players.get(id).reload+"|"+WG.players.get(id).gunCooldown*WG.gameSpeed/1000f+"|"+(int)WG.players.get(id).health + "," + (float)(WG.players.get(id).momentum) + "|" + (int)WG.players.get(id).direction + "|" + WG.players.get(id).deaths + "|" + WG.players.get(id).spawnTimer + "|"/* + (float) (Main.players.get(id).ammoType.mass*Main.players.get(id).ammoType.velocity) + "|" + Bullet.bullets.size()*//*+ "|" +*//*(float) Main.players.get(id).dX + "," + (float) Main.players.get(id).dY + "|" + Main.players.get(id).canMove[0] + "|" + Main.players.get(id).canMove[1] + "|" + Main.players.get(id).canMove[2] + "|" + Main.players.get(id).canMove[3] + "|" + *//*Input.mouseX + "," + Input.mouseY + "|" + Main.stationaryX + "," + Main.stationaryY*/, 10, 50);
	}

	static Color getColor(int id, double opacityIn) {
		float opacity = (float) opacityIn;
		return findColor(WG.players.get(id).color.getRed(), WG.players.get(id).color.getGreen(), WG.players.get(id).color.getBlue(), opacity);
	}

	static Color findColor(int r, int g, int b, float opacity) {
		return new Color(r/255f, g/255f, b/255f, opacity/100);
	}

	void drawItems(Graphics2D g) {
		for (int i = 0;i<Item.items.size();i++) if (Item.items.get(i).pickupable){
			int x = (int) (Item.items.get(i).x-WG.players.get(id).x+Player.stationaryX), y = (int) (Item.items.get(i).y-WG.players.get(id).y+Player.stationaryY);
			g.drawImage(item, x, y, null);
		}
	}

	void drawC4(Graphics2D g) {
		for (int i = 0;i<C4.c4s.size();i++) {
			int x = (int) (C4.c4s.get(i).x-WG.players.get(id).x+Player.stationaryX), y = (int) (C4.c4s.get(i).y-WG.players.get(id).y+Player.stationaryY);
			g.drawImage(c4, x, y, null);
		}
	}

	private void drawBullets(Graphics2D g) {
		g.setColor(Color.BLACK);
		for (int i = 0;i<Bullet.bullets.size();i++) {
			try{
				g.setColor(getColor(Bullet.bullets.get(i).player, 100));
				g.setStroke(new BasicStroke(2));
				if (Bullet.bullets.get(i).ammoType==AmmoType.LASER||Bullet.bullets.get(i).ammoType==AmmoType.RAIL) g.drawLine(
						(int) (WG.players.get(Bullet.bullets.get(i).player).x-WG.players.get(id).x+Player.stationaryX),
						(int) (WG.players.get(Bullet.bullets.get(i).player).y-WG.players.get(id).y+Player.stationaryY),
						(int) (Bullet.bullets.get(i).posX-WG.players.get(id).x+Player.stationaryX),
						(int) (Bullet.bullets.get(i).posY-WG.players.get(id).y+Player.stationaryY));
				if (Bullet.bullets.get(i).ammoType!=AmmoType.LASER&&Bullet.bullets.get(i).ammoType!=AmmoType.RAIL&&Bullet.bullets.get(i).ammoType!=AmmoType.ROCKET) g.fillOval((int) (Bullet.bullets.get(i).posX-WG.players.get(id).x+Player.stationaryX-Bullet.bullets.get(i).size/2), (int) (Bullet.bullets.get(i).posY-WG.players.get(id).y+Player.stationaryY-Bullet.bullets.get(i).size/2), Bullet.bullets.get(i).size, Bullet.bullets.get(i).size);
				if (Bullet.bullets.get(i).ammoType==AmmoType.ROCKET) {
					//double angle = Math.atan2(WGMain.players.get(Bullet.bullets.get(i).player).x-Bullet.bullets.get(i).posX,WGMain.players.get(Bullet.bullets.get(i).player).y-Bullet.bullets.get(i).posY);//+Math.toRadians(90);
					g.drawImage(rocket, (int) (Bullet.bullets.get(i).posX-WG.players.get(id).x+Player.stationaryX), (int) (Bullet.bullets.get(i).posY-WG.players.get(id).y+Player.stationaryY), null);
				}
			} catch(Exception e) {}
		}
		g.rotate(0);
	}

	private void drawGrenades(Graphics2D g) {
		g.setColor(Color.BLACK);
		for (int i = 0;i<Grenade.grenades.size();i++) {
			g.setColor(getColor(Grenade.grenades.get(i).player, 100));
			g.setStroke(new BasicStroke(2));
			g.fillOval((int) (Grenade.grenades.get(i).x-WG.players.get(id).x+Player.stationaryX-Grenade.grenades.get(i).size/2), (int) (Grenade.grenades.get(i).y-WG.players.get(id).y+Player.stationaryY-Grenade.grenades.get(i).size/2), Grenade.grenades.get(i).size, Grenade.grenades.get(i).size);
		}
	}

	private void drawShards(Graphics2D g)  {
		try{
			g.setColor(Color.BLACK);
			for (int i = 0;i<Shard.shards.size();i++) {
				g.setColor(getColor(Shard.shards.get(i).player, 100));
				g.setStroke(new BasicStroke(2));
				g.fillOval((int) (Shard.shards.get(i).x-WG.players.get(id).x+Player.stationaryX-Shard.shards.get(i).size/2), (int) (Shard.shards.get(i).y-WG.players.get(id).y+Player.stationaryY-Shard.shards.get(i).size/2), Shard.shards.get(i).size, Shard.shards.get(i).size);
			}
		}
		catch (IndexOutOfBoundsException e) {}
	}

	void drawOwnGun(Graphics2D g) {
		if (WG.players.get(id).itemType.type==TypeTypes.GUN) {
			int length = (int) (WG.players.get(id).itemType.gun.gunLength/32d*cellSize), size = (int) (WG.players.get(id).itemType.gun.gunSize/32d*cellSize);
			g.setColor(getColor(id, WG.players.get(id).opacity));
			g.setStroke(new BasicStroke(size));
			g.drawLine(Player.stationaryX, Player.stationaryY, (int) (Player.stationaryX+(length*-Math.cos(Math.toRadians(WG.players.get(id).direction)))), (int) (Player.stationaryY+(length*-Math.sin(Math.toRadians(WG.players.get(id).direction)))));
		}
	}

	void drawOthersGun(Graphics2D g) {
		for (int i = 0;i<WG.playerCount;i++) {
			if (i!=id&&WG.players.get(i).itemType.type==TypeTypes.GUN) {
				int length = (int) (WG.players.get(i).itemType.gun.gunLength/32d*cellSize), size = (int) (WG.players.get(i).itemType.gun.gunSize/32d*cellSize);
				g.setStroke(new BasicStroke(size));
				g.setColor(getColor(i, WG.players.get(i).opacity));
				g.drawLine((int) (WG.players.get(i).x-WG.players.get(id).x+Player.stationaryX), (int) (WG.players.get(i).y-WG.players.get(id).y+Player.stationaryY), (int) (WG.players.get(i).x-WG.players.get(id).x+Player.stationaryX+(length*-Math.cos(Math.toRadians(WG.players.get(i).direction)))), (int) (WG.players.get(i).y-WG.players.get(id).y+Player.stationaryY+(length*-Math.sin(Math.toRadians(WG.players.get(i).direction)))));
			}
		}
	}

	void drawLaser(Graphics2D g, int i) {
		g.setColor(getColor(i, WG.players.get(i).opacity));
		g.setStroke(new BasicStroke(2));
		g.drawLine((int) (WG.players.get(i).x-WG.players.get(id).x+Player.stationaryX),
				(int) (WG.players.get(i).y-WG.players.get(id).y+Player.stationaryY),
				(int) (WG.players.get(i).x-WG.players.get(id).x+Player.stationaryX+(1000*-Math.cos(Math.toRadians(WG.players.get(i).direction)))),
				(int) (WG.players.get(i).y-WG.players.get(id).y+Player.stationaryY+(1000*-Math.sin(Math.toRadians(WG.players.get(i).direction)))));
	}

	void drawPlayers(Graphics2D g, boolean self) {
		if (!self) for (int i = 0;i<WG.playerCount;i++) if (i!=id) {
			g.setColor(getColor(i, WG.players.get(i).opacity));
			g.fillRect((int) (WG.players.get(i).x-WG.players.get(id).x+Player.stationaryX-Player.size/2)-1, (int) (WG.players.get(i).y-WG.players.get(id).y+Player.stationaryY-Player.size/2)-1, Player.size, Player.size);
			if (WG.debugText) {g.setColor(Color.WHITE);g.drawString(WG.players.get(i).reload+"|"+WG.players.get(i).shoot+"|"+WG.players.get(i).ammoMag[WG.players.get(i).currentType], (int) (WG.players.get(i).x-WG.players.get(id).x+Player.stationaryX-Player.size/2)-1, (int) (WG.players.get(i).y-WG.players.get(id).y+Player.stationaryY-Player.size)-1);}
		}
		if (self) {
			g.setColor(getColor(id, WG.players.get(id).opacity));
			if (WG.editMode) {g.fillRect(Player.stationaryX-2, Player.stationaryY-2, 5, 5);return;}
			g.fillRect(Player.stationaryX-(Player.size/2)-1, Player.stationaryY-(Player.size/2)-1, Player.size, Player.size);
		}
	}

	void drawBorders(Graphics2D g) {
		g.setColor(new Color(0x303030));
		g.setStroke(new BasicStroke(borderSize));
		g.drawLine(panelX, 0, panelX, panelY);
		g.drawLine(0, panelY, panelX, panelY);
		g.drawLine(0, 0, panelX, 0);
		g.drawLine(0, 0, 0, panelY);
	}

}

class Player {
	int id, deaths, regenCooldown, spawnTimer, sightCooldown, tick, currentType = 0, dC;
	static double mass = 68;
	double opacity = 90, gunCooldown, cloak, direction, health, healthRegenMin = 0.2d, healthRegenMax = 0.75d, acceleration = 0.2d/32d*WG.cellSize, x, y, dX, dY, momentum;
	boolean hasTarget, aiPlayer, up, down, left, right, shoot, shoot2, spawning, opacityUp, opacityDown = true, reload, cloakOn, cloakAvailable, c4Placed, justReloaded;
	static final int stationaryX = WG.stationaryX, stationaryY = WG.stationaryY, size = WG.playerSize, defaultType = 0, spawnCooldown = 4000/WG.gameSpeed, healthRegenTimer = 2000, maxDC = 4;
	boolean[] canMove = new boolean[4];
	int[] ammoMag = new int[Type.types.size()], ammoPool = new int[Type.types.size()], targetCell = {0,0};
	ArrayList<Type> inventory;
	Type itemType;
	ArrayList<Player> LOS;
	ArrayList<Item> LOSI;
	Color color = Color.WHITE;

	Player(int id, boolean ai) {
		aiPlayer = ai;
		this.id = id;
		inventory = new ArrayList<>();
		LOS = new ArrayList<>();
		LOSI = new ArrayList<>();
		if (WG.fullInventory) for (int i = 0;i<Type.types.size();i++) inventory.add(Type.types.get(i));
		checkDeath();
		deaths = 0;
		switch (id) {
			case 0: color = new Color(18, 10, 255, 100);break;
			case 1: color = new Color(243, 28, 21, 100);break;
			default: color = new Color(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256), 100);break;
		}
	}

	void checkDeath() {
		if (health<=0) {
			dX = 0;
			dY = 0;
			shoot = false;
			shoot2 = false;
			health = WG.healthMax;
			spawning = true;
			opacityDown = true;
			spawnTimer = spawnCooldown;
			respawnMove();
			cloak = 0;
			deaths++;
			currentType = defaultType;
			if (!WG.fullInventory) {inventory.clear();inventory.add(Type.types.get(defaultType));}
			ammoMag[currentType] = Type.types.get(currentType).gun.ammoMagMax;
			ammoPool[currentType] = Type.types.get(currentType).gun.ammoMax;
			itemType = inventory.get(currentType);
			if (WG.fullInventory) {
				cloak = UtilityType.cloakDuration;
				for (int i = 0;i<Type.types.size();i++) {
					if (Type.types.get(i).type==TypeTypes.GUN) {
						ammoMag[i] = Type.types.get(i).gun.ammoMagMax;
						ammoPool[i] = Type.types.get(i).gun.ammoMax;
					}
					if (Type.types.get(i).type==TypeTypes.GRENADE) {
						ammoMag[i] = Type.types.get(i).grenade.ammoMax;
						ammoPool[i] = Type.types.get(i).grenade.ammoMax;
					}
					if (Type.types.get(i).type==TypeTypes.UTILITY&&Type.types.get(i).utility==UtilityType.C4) {
						ammoMag[i] = UtilityType.C4ammoMax;
						ammoPool[i] = UtilityType.C4ammoMax;
					}
				}
			}
		}
	}

	public void tick() {
		for (int j = 0;j<4;j++) canMove[j] = true;
		updateDirection();
		if (aiPlayer&&id!=WG.wasdBind) AIPlayer.tick(this);
		move();
		checkDeath();
		opacity();
		momentum = mass*Math.hypot(dX, dY);
		if (health<WG.healthMax&&regenCooldown<=0) health+=regen();
		if (spawnTimer<=0) spawning = false;
		if (cloak>UtilityType.cloakCost) cloakAvailable = true;
		if (!shoot) cloakOn = false;
		if (!reload&&shoot) shoot();
		if (!reload&&shoot2) shoot2();
		if (gunCooldown<=0&&reload) {WG.panels.get(id).reloadSaved = false;reload(false);}
		if (!cloakOn&&cloak<UtilityType.cloakDuration) cloak+=UtilityType.cloakRegen*2/3;
		if (cloakOn) cloak-=UtilityType.cloakRegen;
		if (cloak<0) cloakOn = false;
		if (gunCooldown>0) gunCooldown--;
		if (sightCooldown>0) sightCooldown--;
		if (regenCooldown>0) regenCooldown--;
		if (spawnTimer>0) spawnTimer--;
		if (dC>0) dC--;
		tick++;
	}

	void reload(boolean position) {
		try {
			if (ammoMag[currentType]<itemType.gun.ammoMagMax&&ammoMag[currentType]!=ammoPool[currentType]) {
				reload = position;
				if (reload) gunCooldown = itemType.gun.reload;
				if (!reload&&ammoPool[currentType]>=ammoMag[currentType]) ammoMag[currentType] = itemType.gun.ammoMagMax;
			}
		} catch(Exception e) {}
	}

	void shoot2() {
		if (itemType.type==TypeTypes.UTILITY) {
			if (itemType==Type.C4&&!c4Placed&&ammoMag[currentType]>0&&gunCooldown<=0) {
				C4.c4s.add(new C4(x-size/2, y-size/2, Damage.C4, id));
				c4Placed = true;
				gunCooldown = UtilityType.C4RPM;
				ammoMag[currentType]-=1;
				ammoPool[currentType]-=1;
			}
		}
		if (gunCooldown<=0&&ammoMag[currentType]>0&&itemType.type==TypeTypes.GRENADE) {
			Grenade.grenades.add(new Grenade(x, y, dX, dY, false, id));
			gunCooldown = itemType.grenade.RPM;
			ammoMag[currentType]-=1;
		}
	}

	void shoot() {
		if (itemType.type==TypeTypes.UTILITY) {
			if (itemType==Type.CLOAK&&cloak>0&&cloakAvailable) {
				cloakOn = true;
				cloakAvailable = false;
			}
			if (itemType==Type.C4&&c4Placed&&gunCooldown<=0) {
				for (int i = 0;i<C4.c4s.size();i++) if (C4.c4s.get(i).player==id) C4.c4s.get(i).makeGoBoom();
				c4Placed = false;
			}
		}
		if (gunCooldown<=0) {
			if (ammoMag[currentType]>0&&ammoPool[currentType]>0) {
				if (itemType.type==TypeTypes.GUN) {
					for (int i = 0;i<itemType.gun.pelletCount;i++) Bullet.bullets.add(new Bullet(x, y, dX, dY, id));
					gunCooldown = itemType.gun.RPM;
				}
				if (itemType.type==TypeTypes.GRENADE) {
					Grenade.grenades.add(new Grenade(x, y, dX, dY, true, id));
					gunCooldown = itemType.grenade.RPM;
				}
			}
			if (itemType.type==TypeTypes.GRENADE||itemType.type==TypeTypes.GUN) {
				if (ammoMag[currentType]>0) ammoMag[currentType]-=(itemType.type==TypeTypes.GUN)?itemType.gun.drain:1;
				if (ammoPool[currentType]>0) ammoPool[currentType]-=(itemType.type==TypeTypes.GUN)?itemType.gun.drain:1;
				if (ammoMag[currentType]<=0&&ammoPool[currentType]>0) {
					reload(true);
					if (itemType.gun.reload==-1) gunCooldown = itemType.gun.RPM;
				}
			}
		}
	}

	void pickup() {
		Type type;int t = 0;
		do {
			type = Type.types.get((new Random()).nextInt(Type.types.size()));
			if (!inventory.contains(type)) break;
			t++;
		} while (t<inventory.size()/2);
		inventory.add(type);
		for (int i = 0;i<inventory.size();i++) if (inventory.get(i)==type) for (int j = 0;j<Type.types.size();j++) if (Type.types.get(j)==type) {
			if (type.type==TypeTypes.GUN) {
				ammoMag[j] = Type.types.get(j).gun.ammoMagMax;
				ammoPool[j] = Type.types.get(j).gun.ammoMax;
			}
			if (type.type==TypeTypes.GRENADE) {
				ammoMag[j] = Type.types.get(j).grenade.ammoMax;
				ammoPool[j] = Type.types.get(j).grenade.ammoMax;
			}
			if (type.type==TypeTypes.UTILITY) {
				if (type.utility==UtilityType.CLOAK) cloak = UtilityType.cloakDuration;
				if (type.utility==UtilityType.C4) {
					ammoMag[j] = UtilityType.C4ammoMax;
					ammoPool[j] = UtilityType.C4ammoMax;
				}
			}
		}
	}

	public void changeGun(int direction) {
		cloakOn = false;
		reload = false;
		for (int i = 0;i<1000;i++) {
			currentType+=direction;
			if (currentType>=Type.types.size()) currentType = 0;
			else if (currentType<0) currentType = Type.types.size()-1;
			if (inventory.contains(Type.types.get(currentType))) {gunCooldown = 10;itemType = Type.types.get(currentType);return;}
		}
	}

	public void changeGun(Type type) {
		cloakOn = false;
		reload = false;
		if (inventory.contains(type)) {
			currentType = Type.types.indexOf(type);
			itemType = type;
		}
	}

	public void hit(double damage, double momentum, double direction) {
		if (!spawning) {
			cloakOn = false;
			health-=damage;
			regenCooldown = healthRegenTimer/WG.gameSpeed;
			recoil(momentum, direction, false);
			if (WG.debugText) System.out.println("hit for " + damage);
		}
	}

	public void recoil(double momentum, double direction, boolean self) {
		double xComp = momentum*Math.cos(Math.toRadians(direction)), yComp = momentum*Math.sin(Math.toRadians(direction));
		if (WG.debugText) System.out.println(self+"|"+direction+"|"+momentum+"|"+xComp+","+yComp);
		dX+=(self)?xComp:-xComp;
		dY+=(self)?yComp:-yComp;
	}

	public double regen() {
		if (health<healthRegenMin*WG.healthMax) return (1/healthRegenMin)*WG.healthRegen*WG.dUpdate;
		if (health>healthRegenMax*WG.healthMax) return (1/healthRegenMax)*WG.healthRegen*WG.dUpdate;
		return (1/(health/WG.healthMax))*WG.healthRegen*WG.dUpdate;
	}

	void opacity() {
		if (spawning) {
			if (opacity<=5) {opacityUp = true;opacityDown = false;}
			if (opacity>=40) {opacityUp = false;opacityDown = true;}
			if (opacityUp) opacity+=2;
			if (opacityDown) opacity-=2;
		}
		else opacity = 100;
		if (cloakOn) opacity = momentum/mass+UtilityType.cloakMin;
	}

	void respawnMove() {
		x = 0;y = 0;
		int spawn = 0;
		double checkSize = 10;
		boolean safe = false;
		while (!safe) {
			spawn = (new Random()).nextInt(Level.spawns.length);
			safe = true;
			for (int j = 0;j<WG.players.size();j++) {
				if (safeZone(checkSize, (WG.cellSize*Level.spawns[spawn][0])+size/2, (WG.cellSize*Level.spawns[spawn][1])+size/2).intersects(WG.players.get(j).hitbox())) safe = false;
			}
			checkSize-=0.1;
		}
		x = (WG.cellSize*Level.spawns[spawn][0])+size/2;
		y = (WG.cellSize*Level.spawns[spawn][1])+size/2;
	}

	public static Rectangle safeZone(double size, int x, int y) {return new Rectangle((int)(x-size*WG.cellSize),(int)(y-size*WG.cellSize),(int)(((size*2)+1)*WG.cellSize),(int)(((size*2)+1)*WG.cellSize));}
	public Rectangle hitbox() {return new Rectangle((int)x-size/2, (int)y-size/2, size, size);}
	public Rectangle hitboxUp() {return new Rectangle((int)x-size/2, (int)y-size/2-1, size, 1);}
	public Rectangle hitboxDown() {return new Rectangle((int)x-size/2, (int)y+size/2, size, 1);}
	public Rectangle hitboxLeft() {return new Rectangle((int)x-size/2-1, (int)y-size/2, 1, size);}
	public Rectangle hitboxRight() {return new Rectangle((int)x+size/2, (int)y-size/2, 1, size);}

	private void collisionCheckWall() {
		if (!WG.editMode) for (int x = ((int)WG.players.get(id).x/WG.cellSize)-WG.collisionZoneSize;x<(WG.players.get(id).x/WG.cellSize)+WG.collisionZoneSize;x++) for (int y = ((int)WG.players.get(id).y/WG.cellSize)-WG.collisionZoneSize;y<(WG.players.get(id).y/WG.cellSize)+WG.collisionZoneSize;y++) {
			if (x>=0 && y>=0 && x<Level.sizeX && y<Level.sizeY) if (Level.cells[x][y].type==1||Level.cells[x][y].type==2||(!Level.cells[x][y].marker&&Level.cells[x][y].notWalkonable)) {
				if (hitboxUp().intersects(Level.cells[x][y].hitbox())) canMove[1] = false;
				if (hitboxDown().intersects(Level.cells[x][y].hitbox())) canMove[3] = false;
				if (hitboxLeft().intersects(Level.cells[x][y].hitbox())) canMove[2] = false;
				if (hitboxRight().intersects(Level.cells[x][y].hitbox())) canMove[0] = false;
			}
		}
	}

	/*private void collisionCheckPlayer() {
		if (!Main.edit) for (int i = 0;i<Main.players.size();i++) {
			if (i!=id) {
				if (hitboxUp().intersects(Main.players.get(i).hitbox())) canMove[1] = false;
				if (hitboxDown().intersects(Main.players.get(i).hitbox())) canMove[3] = false;
				if (hitboxLeft().intersects(Main.players.get(i).hitbox())) canMove[2] = false;
				if (hitboxRight().intersects(Main.players.get(i).hitbox())) canMove[0] = false;
			}
		}
	}*/

	public void move() {
		double increment = 0.1d;
		if ((dX<0&&!canMove[2])||(dX>0&&!canMove[0])||Math.abs(dX)<0.001d) dX = 0;
		if ((dY<0&&!canMove[1])||(dY>0&&!canMove[3])||Math.abs(dY)<0.001d) dY = 0;

		for (double i = 0;i<Math.abs(dY)*WG.dUpdate;i+=increment) {
			//collisionCheckPlayer();
			collisionCheckWall();
			if (canMove[1]&&dY<0d) y = y-increment;
			if (canMove[3]&&dY>0d) y = y+increment;
		}
		for (double i = 0;i<Math.abs(dX)*WG.dUpdate;i+=increment) {
			//collisionCheckPlayer();
			collisionCheckWall();
			if (canMove[2]&&dX<0d) x = x-increment;
			if (canMove[0]&&dX>0d) x = x+increment;
		}
		if (Math.abs(dX)>Math.abs(WG.moveSpeed)) dX = WG.moveSpeed*((dX>0)?1:-1);
		if (Math.abs(dY)>Math.abs(WG.moveSpeed)) dY = WG.moveSpeed*((dY>0)?1:-1);
		if (Math.abs(dY)>0&&!up&&!down) dY*=0.8d;
		if (Math.abs(dX)>0&&!left&&!right) dX*=0.8d;
		if (canMove[1]&&up) dY-=acceleration*WG.dUpdate;
		if (canMove[3]&&down) dY+=acceleration*WG.dUpdate;
		if (canMove[2]&&left) dX-=acceleration*WG.dUpdate;
		if (canMove[0]&&right) dX+=acceleration*WG.dUpdate;
	}

	private void updateDirection() {
		if (!(itemType.type==TypeTypes.GUN&&itemType.gun.autoAim)) {
			if (WG.mouse&&WG.wasdBind==id) direction = -Math.toDegrees(Math.atan2(Input.mouseX,Input.mouseY)+Math.toRadians(90));
			if (!WG.mouse) {
				if (left&&dC<=0) direction = 0;
				if (up&&dC<=0) direction = 90;
				if (right&&dC<=0) direction = 180;
				if (down&&dC<=0) direction = 270;
				if (left&&up) {direction = 45;dC = maxDC;}
				if (up&&right) {direction = 135;dC = maxDC;}
				if (right&&down) {direction = 225;dC = maxDC;}
				if (down&&left) {direction = 315;dC = maxDC;}
			}
		}
		else if (WG.playerCount>1) pointLaser();
	}

	private void pointLaser() {
		int player = 1, tolerance = 10;
		double distances[] = new double[WG.playerCount];
		distances[id] = 100001;
		for (int i = 0; i<WG.playerCount;i++) {
			if (i!=id) distances[i] = Math.hypot(Math.abs(x-WG.players.get(i).x), Math.abs(y-WG.players.get(i).y))/WG.cellSize;
		}
		double minDistance = distances[id];
		for (int i = 0;i<WG.playerCount;i++) if (distances[i]<minDistance) {
			player = i;
			minDistance = distances[i];
		}
		if (distances[player]<=tolerance) direction = -Math.toDegrees(Math.atan2(WG.players.get(player).x-WG.players.get(0).x,WG.players.get(player).y-WG.players.get(0).y)+Math.toRadians(90));
		else direction+=400/distances[player];
		if (WG.notTheAimbot) direction = -Math.toDegrees(Math.atan2(WG.players.get(player).x-WG.players.get(0).x,WG.players.get(player).y-WG.players.get(0).y)+Math.toRadians(90));
	}
}


class AIPlayer {
	static final double stupidity = 5, stupidRate = 1;
	static double currentStupidity;
	static boolean stupidityUp, stupidityDown = true;

	static void tick(Player player) {
		stupidity(player);
		int id = player.id;
		if (player.momentum>20) player.direction = wandering(player, player.dX, player.dY);
		player.shoot = false;
		if (player.ammoMag[player.currentType]<=0&&!player.reload) player.reload(true);
		Player target = WG.players.get((id==0)?1:0);
		for (Player p:WG.players) if (p.id!=id&&player.LOS.contains(target)&&!p.cloakOn&&distance(player.x, player.y, p)<distance(player.x, player.y, target)) target = p;
		if (player.LOS.contains(target)&&!target.cloakOn) {
			player.direction = aim(target, player, (player.itemType==Type.RAIL)?currentStupidity*10:currentStupidity);
			try {player.changeGun(chooseItemType(player, distance(player.x, player.y, target)));} catch (Exception e) {}
			if (!player.reload&&!target.spawning&&new Random().nextBoolean()) player.shoot = true;
		}

		updateTargetCell(player);
		move(player);
		updateLOS(player);

	}

	private static void updateTargetCell(Player player) {
		int[] random = {0,0};
		Player min = (player.id==0)?WG.players.get(1):WG.players.get(0);
		int x = player.targetCell[0], y = player.targetCell[1];
		for (Player p:WG.players) {
			if (p!=player&&!p.cloakOn&&distance(player.x, player.y, p)<distance(player.x, player.y, min)) {
				min = p;
			}
		}
		if (!player.hasTarget) for (Item target:Item.items) if (player.LOSI.contains(target)) {
			x = target.x/WG.cellSize;
			y = target.y/WG.cellSize;
			player.hasTarget = true;
		}
		if (player.LOSI.isEmpty()) player.hasTarget = false;
		if (player.LOS.contains(min)&&!min.cloakOn) {
			x = (int) (min.x/WG.cellSize);
			y = (int) (min.y/WG.cellSize);
		}
		int[] cell = {x,y};
		player.targetCell = (player.momentum>5&&player.sightCooldown>0)?cell:random;
	}

	private static void stupidity(Player player) {
		if (currentStupidity<=-player.itemType.gun.COF*stupidity) {stupidityUp = true;stupidityDown = false;}
		if (currentStupidity>=player.itemType.gun.COF*stupidity) {stupidityUp = false;stupidityDown = true;}
		if (stupidityUp) currentStupidity+=stupidRate;
		if (stupidityDown) currentStupidity-=stupidRate;
	}

	private static void move(Player player) {
		if (new Random().nextInt(20)==0) player.up = (player.up)?false:true;
		if (new Random().nextInt(20)==0) player.down = (player.down)?false:true;
		if (new Random().nextInt(20)==0) player.left = (player.left)?false:true;
		if (new Random().nextInt(20)==0) player.right = (player.right)?false:true;
		if (player.targetCell[0]!=0&&player.targetCell[1]!=0) {
			player.up = false;player.down = false;player.left = false;player.right = false;
			if (((int)(player.y/WG.cellSize))-player.targetCell[1]>0) player.up = true;
			if (((int)(player.y/WG.cellSize))-player.targetCell[1]<0) player.down = true;
			if (((int)(player.x/WG.cellSize))-player.targetCell[0]>0) player.left = true;
			if (((int)(player.x/WG.cellSize))-player.targetCell[0]<0) player.right = true;
		}

	}

	private static void updateLOS(Player player) {
		player.LOS.clear();
		player.LOSI.clear();
		for (Player target:WG.players) if (target!=player) new Sight(player, target);
		for (Item target:Item.items) if (target.pickupable) new Sight(player, target);
	}

	static Type chooseItemType(Player player, double distance) {
		Type choice = null;
		for (Type type:Type.aiTypes) {
			if (distance<type.range&&player.ammoMag[Type.types.indexOf(type)]>0) {
				choice = type;
				break;
			}
		}
		return choice;
	}

	static double wandering(Player player, double dX, double dY) {
		return -Math.toDegrees(Math.atan2(dX,dY)+Math.toRadians(90));
	}

	static double distance(double x, double y, Player target) {
		return Math.hypot(Math.abs(x-target.x), Math.abs(y-target.y))/WG.cellSize;
	}

	static double aim(Player target, Player self, double inaccuracy) {
		return -Math.toDegrees(Math.atan2(target.x-self.x,target.y-self.y)+Math.toRadians(90))+((new Random().nextFloat()>0)?1:-1)*inaccuracy;
	}
}

class Ammo {
	final static double[]
	//     mag,  pool/max,  reload,  drain
	PISTOL = {10, 130, 2000, 1},
	SHOTGUN = {6, 18, 3000, 1},
	RAIL = {20, 20, -1, 1},
	MINIGUN = {500, 1500, 4000, 2},
	ROCKET = {6, 30, 3500, 1},
	AR = {30, 180, 1900, 1},
	LASER = {500, 1500, 3000, 1};

	// ammomax
	final static int
	FRAG = 666,//15
	FLASH = 666,//2
	C4 = 666;//5
}

enum AmmoType {
//  gunsize,gunlength,mass,  velocity, cof, firerate,       pellets
	PISTOL(   8, 32,  0.0080d, 1000d,  2.00d, 15,  Damage.PISTOL,  1, Ammo.PISTOL),
	SHOTGUN( 16, 20,  0.0050d,  900d,  5.00d, 25, Damage.SHOTGUN, 10, Ammo.SHOTGUN),
	RAIL(     6, 65, -0.8000d,   -1d,  0.25d, 46,    Damage.RAIL,  1, Ammo.RAIL),
	MINIGUN( 18, 40,  0.0032d,  800d,  3.00d,  2, Damage.MINIGUN,  2, Ammo.MINIGUN),
	ROCKET(  16, 60,  0.0500d,  500d,  1.00d, 20,  Damage.ROCKET,  1, Ammo.ROCKET),
	AR(      10, 40,  0.0041d,  800d,  0.50d,  8,      Damage.AR,  1, Ammo.AR),
	LASER(    6, 25, -0.0100d,   -1d,    -1d,  1,   Damage.LASER,  1, Ammo.LASER);

	int gunSize, gunLength, bulletSize, RPM, pelletCount, explosionRadius, ammoMagMax, ammoMax;
	double reload, drain;
	boolean autoAim;
	double[] damage;
	double COF, velocity, mass;
	final static int speedChange = 10;

	AmmoType(int gunSize, int gunLength, double mass, double velocity, double COF, int RPM, double[] damage, int pelletCount, double[] ammo) {
		this.gunSize = gunSize;
		this.gunLength = gunLength;
		this.mass = mass;
		if (COF<0) this.autoAim = true;
		this.velocity = velocity/WG.cellSize*2;
		this.RPM = RPM;
		this.COF = COF;
		this.pelletCount = pelletCount;
		this.explosionRadius = 0;
		this.damage = damage; this.damage[0]/=pelletCount; this.damage[2]/=pelletCount;
		if (this.damage.length>4) this.explosionRadius = (int)this.damage[4];
		ammoMagMax = (int)ammo[0];
		ammoMax = (int)ammo[1];
		reload = ammo[2]/WG.gameSpeed;
		drain = ammo[3];
	}
}

class Bullet {
	int size = 4, player;
	double posX, posY, iX, iY, dX, dY, mass, momentum, direction, speed;
	public static ArrayList<Bullet> bullets = new ArrayList<>();
	AmmoType ammoType;
	boolean hitCheck;

	Bullet(double x, double y, double iDX, double iDY, int player) {
		this.player = player;
		this.posX = x;
		this.posY = y;
		iX = x;
		iY = y;
		ammoType = WG.players.get(player).itemType.gun;
		double COFX = 0, COFY = 0, mass = ammoType.mass;
		speed = ((new Random()).nextInt(AmmoType.speedChange/2)-AmmoType.speedChange+ammoType.velocity)/32*WG.cellSize;
		momentum = mass*speed;
		direction = WG.players.get(player).direction;
		WG.players.get(player).recoil(momentum, direction, true);
		if (ammoType.velocity<0) collisionScan();
		else {
			COFX = ((new Random()).nextFloat()-0.5f)*2*ammoType.COF;
			COFY = ((new Random()).nextFloat()-0.5f)*2*ammoType.COF;
			dX = speed*-Math.cos(Math.toRadians(direction));
			dY = speed*-Math.sin(Math.toRadians(direction));
			dX+=iDX;
			dY+=iDY;
			dX+=COFX;
			dY+=COFY;
		}
	}

	void tick() {
		double absDX = Math.abs(dX), absDY = Math.abs(dY);
		for (double i = 0;i<((absDX>absDY)?absDX:absDY)*WG.dUpdate;i+=0.2) {
			if (collisionCheck()) {bullets.remove(this);return;}
			if (i<=absDX) posX+=(dX>0)?0.2:-0.2;
			if (i<=absDY) posY+=(dY>0)?0.2:-0.2;
		}
		if (ammoType.velocity<0) bullets.remove(this);
	}

	public Rectangle hitbox() {return new Rectangle((int)posX-size/2, (int)posY-size/2, size, size);}

	boolean collisionCheck() {
		boolean hit = false;
		for (int i = 0;i<WG.players.size();i++) if (i!=player && hitbox().intersects(WG.players.get(i).hitbox())) {
			hit = true;
			if (!hitCheck && ammoType.explosionRadius==0) WG.players.get(i).hit(Type.damage(ammoType, Type.distance(iX, iY, i)), momentum, direction);
			hitCheck = true;
		}
		for (int i = (int)posX/WG.cellSize-WG.collisionZoneSize;i<posX/WG.cellSize+WG.collisionZoneSize;i++) for (int j = (int)posY/WG.cellSize-WG.collisionZoneSize;j<posY/WG.cellSize+WG.collisionZoneSize;j++) {
			if (i>=0 && j>=0 && i<Level.sizeX && j<Level.sizeY) if (Level.cells[i][j].type==1||Level.cells[i][j].type==2||(!Level.cells[i][j].marker&&!Level.cells[i][j].shootThroughable)) if (hitbox().intersects(Level.cells[i][j].hitbox())) hit = true;
		}
		if (posX<0 || posY<0 || posX>Level.sizeX*WG.cellSize || posY>Level.sizeY*WG.cellSize) hit = true;
		if (hit) {
			if (ammoType.explosionRadius>0) Explosion.explosions.add(new Explosion(posX, posY, player, ammoType.explosionRadius, Type.damage(ammoType, Type.distance(iX, iY, posX, posY)), false));
			return true;
		}
		return false;
	}

	void collisionScan() {
		while (!collisionCheck()) {
			posX-=Math.cos(Math.toRadians(direction));
			posY-=Math.sin(Math.toRadians(direction));
		}
	}
}

class C4 {
	double x, y;
	int damage, player;
	static int size = WG.cellSize*3/4;
	static ArrayList<C4> c4s = new ArrayList<>();

	C4(double x, double y, int damage, int player) {
		this.x = x;
		this.y = y;
		this.damage = damage;
		this.player = player;
	}

	void makeGoBoom() {
		c4s.remove(this);
		Explosion.explosions.add(new Explosion(x+Player.size/2, y+Player.size/2, player, UtilityType.C4BlastSize, damage, false));
	}
}

class Cell {
	int gX, gY, type;
	boolean shootThroughable = true, notWalkonable = false, marker = false;

	Cell(int gX, int gY, int type) {
		this.gX = gX;
		this.gY = gY;
		this.type = type;
		if (type>1000) {
			if (String.valueOf(type).charAt(2)=='9') marker = true;
			if (String.valueOf(type).charAt(2)=='2'&&String.valueOf(type).charAt(3)=='0') notWalkonable = true;
			if (String.valueOf(type).charAt(2)=='2'&&String.valueOf(type).charAt(3)=='1') shootThroughable = false;
		}
	}

	public Rectangle hitbox() {return new Rectangle(gX*WG.cellSize, gY*WG.cellSize, WG.cellSize, WG.cellSize);}
}

class Damage {
	final static double[]
	//closedmg, closedis, fardmg, fardis
	PISTOL = {3334, 7, 3000, 12},//15      222   200    3  4
	SHOTGUN = {7000, 2, 4990, 8},//25      280   199	2  3
	RAIL = {10000, 0, 9995, 6},//46       217.3 217.2  1  2
	MINIGUN = {430, 12, 330, 30},//2       215   165   24 31
	ROCKET = {5000, 7, 4000, 18, 11},//20  250   200    2  3
	AR = {1920, 6, 1425, 20},//8           240   178    6  7
	LASER = {200, 1, 170, 3};//1           200   170   50 59

	//total damage
	final static int
	FRAG = 100000,
	FLASH = 0,
	C4 = 10100;
}

class Edit {
	static final int typeCount = 10000, segmentDefaultRarity = 3;
	public static boolean[] change = new boolean[typeCount];
	public static String name;
	private static int[][] level = new int[15][15];

	public static void changeCell(int type) {
		try{Level.cells[(int)WG.players.get(WG.wasdBind).x/WG.cellSize][(int)WG.players.get(WG.wasdBind).y/WG.cellSize].type = type;}
		catch (ArrayIndexOutOfBoundsException e) {System.out.println("Not there. " + WG.players.get(WG.wasdBind).x/WG.cellSize + "," + WG.players.get(WG.wasdBind).y/WG.cellSize);}

	}

	public static void tick() {for (int i = 0;i<typeCount;i++) if (change[i]) changeCell(i);}

	public static void printLevel() {
		StringBuilder board = new StringBuilder();
		board.append("\nLevel:\n");
		for (int y = 0;y<Level.sizeY;y++) {
			board.append("{");
			for (int x = 0;x<Level.sizeX;x++) {
				if (Level.cells[x][y].type<10) board.append(" ");
				if (Level.cells[x][y].type<1000) board.append("  ");
				board.append(Level.cells[x][y].type);
				if (x<Level.sizeX-1) board.append(",");
			}
			board.append("}");
			if (y<Level.sizeY-1) board.append(",\n");
		}
		System.out.println(board);
	}

	public static void saveLevel(int[][] layout, int sizeX, int sizeY) throws IOException {
		StringBuilder board = new StringBuilder();
		board.append("\nLayout "+WG.SEED+"|"+System.currentTimeMillis()+":\n");
		for (int y = 0;y<sizeY;y++) {
			board.append("{");
			for (int x = 0;x<sizeX;x++) {
				if (layout[y][x]<10) board.append(" ");
				if (layout[y][x]<1000) board.append("  ");
				board.append(layout[y][x]+((x<sizeX-1)?",":""));
			}
			board.append("}"+((y<sizeX-1)?",\n":""));
		}
		System.out.println(board);
		String fileName = "src/data/Maps/Map "+WG.SEED+" "+sizeX+"x"+sizeY+" ("+sizeX/15+"x"+sizeY/15+") "+System.currentTimeMillis()+".txt";
		System.out.println(fileName);
		FileWriter fw = new FileWriter(new File(fileName));

		BufferedWriter w = new BufferedWriter(fw);
		w.write(board.toString());
		w.close();
	}

	public static void printSegment(String name) {

		for (int y = 0;y<15;y++) for (int x = 0;x<15;x++) level[x][y] = Level.cells[x][y].type;
		int[] sides = {-1, -1, -1, -1, 1, segmentDefaultRarity, -1};
		int continuous = 0;
		for (int i = 0;i<4;i++) {
			sides[i] = findSide(i);
			if (sides[i]==0) continuous++;
		}
		if (continuous==3||continuous==4) sides[6] = 0;
		else sides[6] = 1;
		StringBuilder segment = new StringBuilder();
		segment.append("\nSegment:\npublic static final int[][][] "+name);
		for (int i = 0;i<4;i++) segment.append(sides[i]);
		segment.append(" = {{\n");
		for (int y = 0;y<15;y++) {
			segment.append("    {");//4 spaces
			for (int x = 0;x<15;x++) {
				if (level[y][x]<10) segment.append(" ");
				segment.append(level[x][y]);
				if (x<15-1) segment.append(",");
			}
			segment.append("}");
			if (y<15-1) segment.append(",\n");
		}
		segment.append("\n},{{");
		for (int i = 0;i<7;i++) segment.append(sides[i]+((i<6)?",":""));
		segment.append("}}};\n");
		System.out.print(segment);
	}

	static int findSide(int side) {
		int type = -1, x = 0, y = 0;
		switch (side) {
			case 0: {x=1;y=0;type = level[x][y]/*-sideModifier*/;break;}
			case 1: {x=14;y=13;type = level[x][y]/*-sideModifier*/;break;}
			case 2: {x=13;y=14;type = level[x][y]/*-sideModifier*/;break;}
			case 3: {x=0;y=1;type = level[x][y]/*-sideModifier*/;break;}
		}
		level[x][y] = 2;
		switch (type) {
			case 3: {level[x][y] = 0;break;}
			case 4: {level[x][y] = 1;break;}
			case 5: {level[x][y] = 0;break;}
		}
		return type;
	}
}

class Explosion {
	int maxSize, player;
	double x, y, size = 0, damage;
	boolean harmless, flash;
	public static ArrayList<Explosion> explosions = new ArrayList<>();

	Explosion(double x, double y, int player, int size, double damage, boolean flash) {
		this.player = player;
		this.x = x;
		this.y = y;
		this.maxSize = size*WG.cellSize;
		this.damage = damage;
		if (flash) {
			this.flash = true;
			harmless = true;
		}
	}

	void tick() {
		checkHit();
		size+=Panel.explosionGrowthRate;
		if (size>=maxSize) explosions.remove(this);
	}

	public Rectangle hitbox() {return new Rectangle((int) (x-size/2), (int) (y-size/2), (int) (size), (int) (size));}

	void checkHit() {
		for (int i = 0;i<WG.players.size();i++) if (hitbox().intersects(WG.players.get(i).hitbox())) {
			if (flash) WG.panels.get(i).flashTimer = GrenadeType.fDuration;
			if (!harmless&&(WG.fFire?true:i!=player)) WG.players.get(i).hit(Type.damage(damage, maxSize, Type.distance(x, y, i)), 0d, 0d);
			harmless = true;
		}
	}

}

class Grenade {
	final static int timer = GrenadeType.timer;
	int size = 8, player, life, shards;
	final double deceleration = 0.9d;
	double x, y, iX, iY, dX, dY, direction, speed;
	public static ArrayList<Grenade> grenades = new ArrayList<>();
	GrenadeType grenadeType;

	Grenade(double x, double y, double iDX, double iDY, boolean highVelocity, int player) {
		this.player = player;
		this.x = x;
		this.y = y;
		iX = x;
		iY = y;
		grenadeType = WG.players.get(player).itemType.grenade;
		shards = grenadeType.shards;
		speed = ((highVelocity)?grenadeType.velocityHigh:grenadeType.velocityLow)/32*WG.cellSize;
		direction = WG.players.get(player).direction;
		dX = speed*-Math.cos(Math.toRadians(direction));
		dY = speed*-Math.sin(Math.toRadians(direction));
		dX+=iDX;
		dY+=iDY;
	}

	void tick() {
		for (double i = 0;i<Math.abs(dX)*WG.dUpdate;i+=0.2) {
			if (collisionCheck()) dX*=-1;
			x+=(dX>0)?0.2:-0.2;
		}
		for (double i = 0;i<Math.abs(dY)*WG.dUpdate;i+=0.2) {
			if(collisionCheck()) dY*=-1;
			y+=(dY>0)?0.2:-0.2;
		}
		dX*=deceleration;
		dY*=deceleration;
		if ((dX<0.01d&&dX>-0.01d&&dY<0.01d&&dY>-0.01d)||(life>timer/WG.gameSpeed)) {
			if (grenadeType==GrenadeType.FRAG) for (int i = 0;i<shards;i++) Shard.shards.add(new Shard(x, y, player));
			if (grenadeType==GrenadeType.FLASH) Explosion.explosions.add(new Explosion(x, y, player, 20/*GrenadeType.flashSize*/, 0, true));
			grenades.remove(this);
		}
		life++;
	}

	public Rectangle hitbox() {return new Rectangle((int)x-size/2, (int)y-size/2, size, size);}

	boolean collisionCheck() {
		for (int i = (int)x/WG.cellSize-WG.collisionZoneSize;i<x/WG.cellSize+WG.collisionZoneSize;i++) for (int j = (int)y/WG.cellSize-WG.collisionZoneSize;j<y/WG.cellSize+WG.collisionZoneSize;j++) {
			if (i>=0 && j>=0 && i<Level.sizeX && j<Level.sizeY) if (Level.cells[i][j].type==1||Level.cells[i][j].type==2) if (hitbox().intersects(Level.cells[i][j].hitbox())) return true;
		}
		if (x<0 || y<0 || x>Level.sizeX*WG.cellSize || y>Level.sizeY*WG.cellSize) return true;
		return false;
	}
}

enum GrenadeType {
	FRAG(40d, 6d, 25, 0.0080d, Damage.FRAG, 360, Ammo.FRAG),
	FLASH(40d, 6d, 25, 0.0080d, Damage.FLASH, 0, Ammo.FLASH);
	//    /\change these
	double velocityHigh, velocityLow, shardMass;
	int RPM, shards, damage, ammoMax;
	final static int
	flashSize = 15, flashDuration = 4000, fDuration = flashDuration/WG.cellSize, fDropOff = fDuration/3, timer = 5000, fragBlastSize = 8;

	GrenadeType(double velocityHigh, double velocityLow, int RPM, double shardMass, int damage, int shards, int ammo) {
		this.velocityHigh = velocityHigh;
		this.velocityLow = velocityLow;
		this.RPM = RPM;
		this.damage = damage;
		this.shards = shards;
		this.shardMass = shardMass;
		ammoMax = ammo;
	}
}

class Input implements KeyListener, MouseMotionListener, MouseListener, MouseWheelListener {
	public static int mouseX, mouseY;
	public static int[][] blocks = {{0,1,2},{10,11,12,13,14,15,16},{1020,1120,1220,1320,1420,1520,1620},{1021,1121,1221,1321,1421,1521,1621},{1090,1190,1290,1390,1490,1590,1690},{1091,1191,1291,1391,1491,1591,1691}};
	public static int currentBlockList = 0, currentBlock = 0;
	public static boolean list = false;

	void updateMouse(MouseEvent e) {
		if (WG.mouse) {
			mouseX = e.getX()-WG.stationaryX;
			mouseY = e.getY()-WG.stationaryY-22;
		}
	}

	void updateMouse(MouseEvent e, boolean click) {
		if (WG.editMode) {
			if (e.getButton()==MouseEvent.BUTTON1) edit(click);
			if (e.getButton()==MouseEvent.BUTTON2) ;
			if (e.getButton()==MouseEvent.BUTTON3) list = click;
		}
		if (!WG.editMode) {
			if (e.getButton()==MouseEvent.BUTTON1) WG.players.get(WG.wasdBind).shoot = click;
			if (e.getButton()==MouseEvent.BUTTON2) ;
			if (e.getButton()==MouseEvent.BUTTON3) WG.players.get(WG.wasdBind).shoot2 = click;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (WG.editMode) {
			Edit.change[blocks[currentBlockList][currentBlock]] = false;
			if (e.getWheelRotation()<0) {
				if (list) {
					currentBlockList++;
					if (currentBlockList>blocks.length-1) currentBlockList = 0;
					currentBlock = 0;
				}
				if (!list) {
					currentBlock++;
					if (currentBlock>blocks[currentBlockList].length-1) currentBlock = 0;
				}
			}
			if (e.getWheelRotation()>0) {
				if (list) {
					currentBlockList--;
					if (currentBlockList<0) currentBlockList = blocks.length-1;
					currentBlock = 0;
				}
				if (!list) {
					currentBlock--;
					if (currentBlock<0) currentBlock = blocks[currentBlockList].length-1;
				}
			}
			System.out.println(currentBlockList+"|"+currentBlock+"|"+blocks[currentBlockList][currentBlock]);
		}
		if (!WG.editMode) {
			if (e.getWheelRotation()<0) WG.players.get(WG.wasdBind).changeGun(-1);
			if (e.getWheelRotation()>0) WG.players.get(WG.wasdBind).changeGun(1);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {updateMouse(e, true);}
	@Override
	public void mouseReleased(MouseEvent e) {updateMouse(e, false);}
	@Override
	public void mouseMoved(MouseEvent e) {updateMouse(e);}
	@Override
	public void mouseDragged(MouseEvent e) {updateMouse(e);}

	public void edit(boolean state) {
		Edit.change[blocks[currentBlockList][currentBlock]] = state;
		System.out.println(currentBlockList+"|"+currentBlock+"|"+blocks[currentBlockList][currentBlock]);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode()==KeyEvent.VK_W) WG.players.get(WG.wasdBind).up = true;
		if (e.getKeyCode()==KeyEvent.VK_S) WG.players.get(WG.wasdBind).down = true;
		if (e.getKeyCode()==KeyEvent.VK_A) WG.players.get(WG.wasdBind).left = true;
		if (e.getKeyCode()==KeyEvent.VK_D) WG.players.get(WG.wasdBind).right = true;
		if (e.getKeyCode()==KeyEvent.VK_SPACE) WG.players.get(WG.wasdBind).shoot = true;
		if (e.getKeyCode()==KeyEvent.VK_V) WG.players.get(WG.wasdBind).shoot2 = true;
		if (e.getKeyCode()==KeyEvent.VK_Q) WG.players.get(WG.wasdBind).changeGun(-1);
		if (e.getKeyCode()==KeyEvent.VK_E) WG.players.get(WG.wasdBind).changeGun(1);
		if (e.getKeyCode()==KeyEvent.VK_R) if (!WG.players.get(WG.wasdBind).reload&&WG.players.get(WG.wasdBind).itemType.type==TypeTypes.GUN) WG.players.get(WG.wasdBind).reload(true);

		if (WG.mouse) {
			boolean cG = false;
			if (e.getKeyCode()==KeyEvent.VK_1) {WG.players.get(WG.wasdBind).currentType = 0;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_2) {WG.players.get(WG.wasdBind).currentType = 1;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_3) {WG.players.get(WG.wasdBind).currentType = 2;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_4) {WG.players.get(WG.wasdBind).currentType = 3;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_5) {WG.players.get(WG.wasdBind).currentType = 4;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_6) {WG.players.get(WG.wasdBind).currentType = 5;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_7) {WG.players.get(WG.wasdBind).currentType = 6;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_8) {WG.players.get(WG.wasdBind).currentType = 7;cG = true;}
			if (e.getKeyCode()==KeyEvent.VK_9) {WG.players.get(WG.wasdBind).currentType = 8;cG = true;}
			if (cG) {WG.players.get(WG.wasdBind).changeGun(0);}
		}

		if (WG.playerCount>1) {
			if (e.getKeyCode()==KeyEvent.VK_UP) WG.players.get(WG.arrowBind).up = true;
			if (e.getKeyCode()==KeyEvent.VK_DOWN) WG.players.get(WG.arrowBind).down = true;
			if (e.getKeyCode()==KeyEvent.VK_LEFT) WG.players.get(WG.arrowBind).left = true;
			if (e.getKeyCode()==KeyEvent.VK_RIGHT) WG.players.get(WG.arrowBind).right = true;
			if (e.getKeyCode()==KeyEvent.VK_SLASH) WG.players.get(WG.arrowBind).shoot = true;
			if (e.getKeyCode()==KeyEvent.VK_NUMPAD0) WG.players.get(WG.arrowBind).shoot2 = true;
			/*if (e.getKeyCode()==KeyEvent.VK_PERIOD) Main.players.get(Main.arrowBind).changeGun(1);
			if (e.getKeyCode()==KeyEvent.VK_COMMA) Main.players.get(Main.arrowBind).changeGun(-1);*/
		}

		if (e.getKeyCode()==KeyEvent.VK_F1) WG.miniMap = (WG.miniMap)?false:true;
		if (e.getKeyCode()==KeyEvent.VK_F2) WG.editMode = (WG.editMode)?false:true;
		if (e.getKeyCode()==KeyEvent.VK_F3) WG.debugText = (WG.debugText)?false:true;
		if (e.getKeyCode()==KeyEvent.VK_F4) WG.grid = (WG.grid)?false:true;
		if (e.getKeyCode()==KeyEvent.VK_F5) WG.players.get(WG.wasdBind).pickup();
		if (e.getKeyCode()==KeyEvent.VK_F8) Panel.loadTextures();
		if (e.getKeyCode()==KeyEvent.VK_B) Panel.sizeMM--;
		if (e.getKeyCode()==KeyEvent.VK_N) Panel.sizeMM++;
		if (e.getKeyCode()==KeyEvent.VK_EQUALS) WG.wasdBind++;
		if (e.getKeyCode()==KeyEvent.VK_MINUS) WG.wasdBind--;
		if (WG.wasdBind>=WG.playerCount) WG.wasdBind = 0;
		else if (WG.wasdBind<0) WG.wasdBind = WG.playerCount-1;

	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode()==KeyEvent.VK_W) WG.players.get(WG.wasdBind).up = false;
		if (e.getKeyCode()==KeyEvent.VK_S) WG.players.get(WG.wasdBind).down = false;
		if (e.getKeyCode()==KeyEvent.VK_A) WG.players.get(WG.wasdBind).left = false;
		if (e.getKeyCode()==KeyEvent.VK_D) WG.players.get(WG.wasdBind).right = false;
		if (e.getKeyCode()==KeyEvent.VK_SPACE) WG.players.get(WG.wasdBind).shoot = false;
		if (e.getKeyCode()==KeyEvent.VK_C) WG.players.get(WG.wasdBind).shoot2 = false;

		if (WG.playerCount>1) {
			if (e.getKeyCode()==KeyEvent.VK_UP) WG.players.get(WG.arrowBind).up = false;
			if (e.getKeyCode()==KeyEvent.VK_DOWN) WG.players.get(WG.arrowBind).down = false;
			if (e.getKeyCode()==KeyEvent.VK_LEFT) WG.players.get(WG.arrowBind).left = false;
			if (e.getKeyCode()==KeyEvent.VK_RIGHT) WG.players.get(WG.arrowBind).right = false;
			if (e.getKeyCode()==KeyEvent.VK_SLASH) WG.players.get(WG.arrowBind).shoot = false;
			if (e.getKeyCode()==KeyEvent.VK_NUMPAD0) WG.players.get(WG.arrowBind).shoot2 = false;
		}

		if (e.getKeyCode()==KeyEvent.VK_0) {if (Segment.editSegment) Edit.printSegment(Edit.name);else Edit.printLevel();}
	}

	@Override
	public void keyTyped(KeyEvent e) {}
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
}

class Item {
	boolean pickupable;
	int x, y, respawnTimer;
	static int size = WG.cellSize*3/4;
	static ArrayList<Item> items = new ArrayList<>();

	Item(int x, int y) {
		this.x = x*WG.cellSize;
		this.y = y*WG.cellSize;
		respawn();
	}

	void respawn() {
		pickupable = true;
	}

	void tick() {
		collisionCheck();
		if (respawnTimer<=0) respawn();
		respawnTimer--;
	}

	public Rectangle hitbox() {return new Rectangle(x+((WG.cellSize-Item.size)/2), y+((WG.cellSize-Item.size)/2), size, size);}

	void collisionCheck() {
		for (int i = 0;i<WG.players.size();i++) if (pickupable && hitbox().intersects(WG.players.get(i).hitbox())) {
			pickupable = false;
			WG.players.get(i).pickup();
			WG.players.get(i).LOSI.remove(this);
			respawnTimer = WG.itemCooldown*750/WG.gameSpeed;
			if (WG.debugText) System.out.println("pickup: " + i);
		}
	}
}

class Level {
	static int[][] tempLayout, layout, spawns, itemSpawns;
	public static int sizeX, sizeY;
	static Cell[][] cells;

	Level() {
		tempLayout = WG.selectedLevel;
		sizeX = WG.selectedLevel[0].length;
		sizeY = WG.selectedLevel.length;
		System.out.println("Making Level...\nSize: "+sizeY+","+sizeX);
		layout = new int[sizeY][sizeX];
		boolean repair = false;
		for (int x = 0;x<sizeX;x++) for (int y = 0;y<sizeY;y++) {
			try {layout[y][x] = tempLayout[y][x];}
			catch (ArrayIndexOutOfBoundsException e) {repair = true;break;}
		}
		if (repair) repair();
		ArrayList<int[]> tempSpawns = new ArrayList<>();
		ArrayList<int[]> tempItemSpawns = new ArrayList<>();
		for (int y = 0;y<sizeY;y++) for (int x = 0;x<sizeX;x++) {
			if (layout[y][x]>1000&&String.valueOf(layout[y][x]).charAt(2)=='9'&&String.valueOf(layout[y][x]).charAt(3)=='0') {
				int[] spot = {x, y};
				tempSpawns.add(spot);
			}
		}
		int[] origin = {0, 0};
		if (tempSpawns.isEmpty()) tempSpawns.add(origin);
		for (int y = 0;y<sizeY;y++) for (int x = 0;x<sizeX;x++) {
			if (layout[y][x]>1000&&String.valueOf(layout[y][x]).charAt(2)=='9'&&String.valueOf(layout[y][x]).charAt(3)=='1') {
				int[] spot = {x, y};
				tempItemSpawns.add(spot);
				Item.items.add(new Item(x, y));
			}
		}
		spawns = new int[tempSpawns.size()][2];
		itemSpawns = new int[tempItemSpawns.size()][2];
		for (int i = 0;i<tempSpawns.size();i++) {
			spawns[i][0] = tempSpawns.get(i)[0];
			spawns[i][1] = tempSpawns.get(i)[1];
		}
		for (int i = 0;i<tempItemSpawns.size();i++) {
			itemSpawns[i][0] = tempItemSpawns.get(i)[0];
			itemSpawns[i][1] = tempItemSpawns.get(i)[1];
		}
		cells = new Cell[sizeX][sizeY];
		for (int x = 0;x<sizeX;x++) for (int y = 0;y<sizeY;y++) {
			if (WG.autoEdge&&(y==0||x==0||y==sizeY-1||x==sizeX-1)) layout[y][x] = 1;
			cells[x][y] = new Cell(x, y, layout[y][x]);
		}
		if (repair) Edit.printLevel();

		System.out.print("\nLevel Made.\n\n");
	}

	private void repair() {
		System.out.println(sizeX+","+sizeY+" Repairing Level...");
		for (int x = 0;x<sizeX;x++) for (int y = 0;y<sizeY;y++) {
			layout[y][x] = 0;
			try {layout[y][x] = tempLayout[y][x];}
			catch (ArrayIndexOutOfBoundsException e) {}
		}
	}
}

class LevelGen {
	static int[][] level;
	static int totalAttempts, sizeX, sizeY,
		genSizeX = (WG.genSizeX[0]==-1)?WG.genSizeX[1]:(WG.genSizeX[0]==WG.genSizeX[1])?WG.genSizeX[0]:(new Random()).nextInt(WG.genSizeX[1]-WG.genSizeX[0])+WG.genSizeX[0],
		genSizeY = (WG.genSizeY[0]==-1)?WG.genSizeY[1]:(WG.genSizeY[0]==WG.genSizeY[1])?WG.genSizeY[0]:(new Random()).nextInt(WG.genSizeY[1]-WG.genSizeY[0])+WG.genSizeY[0];
	static final int failThreashold = 1000;
	static Segment[][] board;
	static int[][] fail = {{-1}};
	public static Random random;

	/*public static void main(String[] args) {
		for (int i = 0;i<100;i++) {
			WGMain.SEED = (new Random()).nextLong();
			generateLevel(true);
		}
	}*/

	static int[][] editSegment() {
		/*System.out.print("Backround type: ");
		Scanner scan = new Scanner(System.in);
		int type = scan.nextInt();
		System.out.print("Name: ");
		Edit.name = scan.next();*/
		int[][] layout = new int[15][15];
		for (int y = 0;y<sizeY;y++) for (int x = 0;x<sizeX;x++) layout[y][x] = 2;//type;
		int[] sides = {3,1,2,0};
		layout = SegmentGen.generateSegment(sides);
		//scan.close();
		return layout;
	}

	static int[][] generateLevel(boolean save, boolean genSegments) {
		if (WG.SEED==0) WG.SEED = (new Random()).nextLong();
		random = new Random(WG.SEED);
		sizeY = 15*genSizeY;sizeX = 15*genSizeX;
		int[][] layout = new int[sizeY][sizeX];
		int spawnMin = WG.playerCount+2, spawnMax = (genSizeX*genSizeX/3<spawnMin)?genSizeX*genSizeX/3+spawnMin:genSizeX*genSizeX/3, lootMin = (int) (genSizeX*genSizeY*0.2d), lootMax = lootMin+(int)(genSizeX*genSizeY*0.6d);
		System.out.print("Starting Map Gen...\nSeed: "+WG.SEED+"\nGenerator Size: "+genSizeX+","+genSizeY+"\nSegments: "+Segment.allSegments.size()+","+Segment.segments.size()+"\nSpawn Target: "+spawnMin+","+spawnMax+"\nLoot Target: "+lootMin+","+lootMax+"\n");
		int spawns = 0, loot = 0;
		long initTime = System.currentTimeMillis();
		float currentTime = (System.currentTimeMillis()-initTime)/1000f;
		if (!genSegments) do {
			layout = genLevel();
			spawns = 0; loot = 0;
			if (layout!=fail) for (int y = 0;y<sizeY;y++) for (int x = 0;x<sizeX;x++) {
				if (layout[y][x]>1000) {
					if (String.valueOf(layout[y][x]).charAt(2)=='9'&&String.valueOf(layout[y][x]).charAt(3)=='0') spawns++;
					if (String.valueOf(layout[y][x]).charAt(2)=='9'&&String.valueOf(layout[y][x]).charAt(3)=='1') loot++;
				}
			}
			if (loot>lootMax||loot<lootMin) layout = fail;
			if (spawns>spawnMax||spawns<spawnMin) layout = fail;
			currentTime = (System.currentTimeMillis()-initTime)/1000f;
			if (totalAttempts%150==0) System.out.print("Time: "+currentTime+"|Give it a second\n");
		} while (layout==fail);
		if (genSegments) layout = genRandomSegmentLevel();
		System.out.print("Time: "+currentTime+"\nGenerated.\nTries: "+totalAttempts+"\nSpawns: "+spawns+"\nLoot: "+loot+"\n\n");
		if (save) try{Edit.saveLevel(layout, sizeX, sizeY);} catch(Exception e) {e.printStackTrace();}
		return layout;
	}

	static int[][] genLevel() {
		level = new int[sizeY][sizeX];
		board = new Segment[genSizeY][genSizeX];
		for (int y = 0;y<genSizeY;y++) for (int x = 0;x<genSizeX;x++) {
			boolean valid = false;
			int segmentId = 0, rotate = 0, mirror = 0, attempts = 0;
			Segment segment = null;
			while (!valid) {
				segmentId = random.nextInt(Segment.segments.size());
				rotate = random.nextInt(4);
				mirror = random.nextInt(3);
				segment = new Segment(segmentId, rotate, mirror);

				attempts++;
				if (attempts>failThreashold) {
					if (x==0||y==0||(board[y-1][x].down==6||board[y][x-1].right==6)) {totalAttempts++;return fail;}
					segment = new Segment(findValid(x, y, board));
				}
				if (attempts>failThreashold*30) {totalAttempts++;return fail;}
				if (checkValid(x, y, segment)) valid = true;
			}
			Segment.generatedSegments.add(segment);
			board[y][x] = segment;
			addSegment(x, y, board[y][x]);
		}
		Segment.generatedSegments.clear();
		return level;
	}

	static int[][] genRandomSegmentLevel() {
		level = new int[sizeY][sizeX];
		board = new Segment[genSizeY][genSizeX];
		for (int y = 0;y<genSizeY;y++) for (int x = 0;x<genSizeX;x++) {
			Segment segment = null;
			segment = new Segment(findValid(x, y, board));
			Segment.generatedSegments.add(segment);
			board[y][x] = segment;
			addSegment(x, y, board[y][x]);
		}
		Segment.generatedSegments.clear();
		return level;
	}

	static boolean checkValid(int x, int y, Segment segment) {
		boolean valid = true;
		if ((x==0&&segment.left!=0)||(y==0&&segment.up!=0)||(x==genSizeX-1&&segment.right!=0)||(y==genSizeY-1&&segment.down!=0)) valid = false;
		if (y!=0&&((board[y-1][x].down!=segment.up)||(!board[y-1][x].continuous&&!segment.continuous))) valid = false;
		if (x!=0&&((board[y][x-1].right!=segment.left)||(!board[y][x-1].continuous&&!segment.continuous))) valid = false;
		return valid;
	}

	static int[] findValid(int x, int y, Segment[][] board) {
		int[] sides = new int[4];
		if (y==0) sides[0] = 0;else sides[0] = board[y-1][x].down;
		if (x==0) sides[3] = 0;else sides[3] = board[y][x-1].right;
		sides[1] = new Random().nextInt(2)+1;
		sides[2] = new Random().nextInt(2)+1;
		if (x==genSizeX-1) sides[1] = 0;
		if (y==genSizeY-1) sides[2] = 0;
		return sides;
	}

	static void addSegment(int locationX, int locationY, Segment segment) {
		board[locationY][locationX] = segment;
		locationX*=15;locationY*=15;
		for (int y = 0;y<segment.sizeY;y++) for (int x = 0;x<segment.sizeX;x++) level[locationY+y][locationX+x] = segment.getSpot(y, x);
	}
}

class Shard {
	int size = 2, player, life, blastSize = GrenadeType.fragBlastSize;
	double x, y, iX, iY, dX, dY, damage, direction, mass = GrenadeType.FRAG.shardMass, momentum, speed, velocity = 20d;
	public static ArrayList<Shard> shards = new ArrayList<>();
	boolean hitCheck;

	Shard(double x, double y, int player) {
		this.player = player;
		this.x = x;
		this.y = y;
		iX = x;
		iY = y;
		speed = ((new Random()).nextFloat()*3-5+velocity)/32*WG.cellSize;
		momentum = mass*speed;
		damage = GrenadeType.FRAG.damage/GrenadeType.FRAG.shards;
		direction = (new Random()).nextInt(361);
		dX = speed*-Math.cos(Math.toRadians(direction));
		dY = speed*-Math.sin(Math.toRadians(direction));
	}

	void tick() {
		for (double i = 0;i<Math.abs(dX)*WG.dUpdate;i+=0.2) {
			if (collisionCheck()) {
				shards.remove(this);
				return;
			}
			x+=(dX>0)?0.2:-0.2;
		}
		for (double i = 0;i<Math.abs(dY)*WG.dUpdate;i+=0.2) {
			if (collisionCheck()) {
				shards.remove(this);
				return;
			}
			y+=(dY>0)?0.2:-0.2;
		}
		if (life>blastSize) shards.remove(this);
		life++;
	}

	public Rectangle hitbox() {return new Rectangle((int)x-size/2, (int)y-size/2, size, size);}

	boolean collisionCheck() {
		boolean hit = false;
		for (int i = 0;i<WG.players.size();i++) if ((WG.fFire?true:i!=player)&&hitbox().intersects(WG.players.get(i).hitbox())) {
			hit = true;
			if (!hitCheck) WG.players.get(i).hit(Type.damage(damage, blastSize*WG.cellSize, Type.distance(iX, iY, i)), momentum, direction);
			hitCheck = true;
		}
		for (int i = (int)x/WG.cellSize-WG.collisionZoneSize;i<x/WG.cellSize+WG.collisionZoneSize;i++) for (int j = (int)y/WG.cellSize-WG.collisionZoneSize;j<y/WG.cellSize+WG.collisionZoneSize;j++) {
			if (i>=0 && j>=0 && i<Level.sizeX && j<Level.sizeY) if (Level.cells[i][j].type==1||Level.cells[i][j].type==2||(!Level.cells[i][j].marker&&!Level.cells[i][j].shootThroughable)) if (hitbox().intersects(Level.cells[i][j].hitbox())) hit = true;
		}
		if (x<0 || y<0 || x>Level.sizeX*WG.cellSize || y>Level.sizeY*WG.cellSize) hit = true;
		if (hit) return true;
		return false;
	}

	void collisionScan() {
		while (!collisionCheck()) {
			x-=Math.cos(Math.toRadians(direction));
			y-=Math.sin(Math.toRadians(direction));
		}
	}
}

class Sight {
	static final int cooldown = 1500, cooldownRandom = 500;
	double posX, posY;
	boolean hitTarget;

	Sight(Player player, Item target) {
		this.posX = player.x;
		this.posY = player.y;
		if (collisionScan(player, target)) {
			player.LOSI.add(target);
			player.sightCooldown = 2;
		}
	}

	Sight(Player player, Player target) {
		this.posX = player.x;
		this.posY = player.y;
		if (collisionScan(player, target)) {
			player.LOS.add(target);
			player.sightCooldown = (cooldown+((new Random().nextInt(cooldownRandom*2))-cooldownRandom))/WG.gameSpeed;
		}
	}

	public Rectangle hitbox() {return new Rectangle((int)posX, (int)posY, 1, 1);}

	boolean collisionCheck(Player target) {
		if (hitbox().intersects(target.hitbox())) {
			hitTarget = true;
			return true;
		}
		for (int i = (int)posX/WG.cellSize-WG.collisionZoneSize;i<posX/WG.cellSize+WG.collisionZoneSize;i++) for (int j = (int)posY/WG.cellSize-WG.collisionZoneSize;j<posY/WG.cellSize+WG.collisionZoneSize;j++) if (i>=0 && j>=0 && i<Level.sizeX && j<Level.sizeY) if (Level.cells[i][j].type==1||Level.cells[i][j].type==5) if (hitbox().intersects(Level.cells[i][j].hitbox())) return true;
		if (posX<0||posY<0||posX>Level.sizeX*WG.cellSize||posY>Level.sizeY*WG.cellSize) return true;
		return false;
	}

	boolean collisionScan(Player player, Player target) {
		while (!collisionCheck(target)) {
			posX-=Math.cos(Math.toRadians(-Math.toDegrees(Math.atan2(target.x-player.x,target.y-player.y)+Math.toRadians(90))));
			posY-=Math.sin(Math.toRadians(-Math.toDegrees(Math.atan2(target.x-player.x,target.y-player.y)+Math.toRadians(90))));
		}
		if (hitTarget) return true;
		return false;
	}

	boolean collisionCheck(Item target) {
		if (hitbox().intersects(target.hitbox())&&target.pickupable) {
			hitTarget = true;
			return true;
		}
		for (int i = (int)posX/WG.cellSize-WG.collisionZoneSize;i<posX/WG.cellSize+WG.collisionZoneSize;i++) for (int j = (int)posY/WG.cellSize-WG.collisionZoneSize;j<posY/WG.cellSize+WG.collisionZoneSize;j++) if (i>=0 && j>=0 && i<Level.sizeX && j<Level.sizeY) if (Level.cells[i][j].type==1||Level.cells[i][j].type==5||Level.cells[i][j].type==3) if (hitbox().intersects(Level.cells[i][j].hitbox())) return true;
		if (posX<0||posY<0||posX>Level.sizeX*WG.cellSize||posY>Level.sizeY*WG.cellSize) return true;
		return false;
	}

	boolean collisionScan(Player player, Item target) {
		while (!collisionCheck(target)) {
			posX-=Math.cos(Math.toRadians(-Math.toDegrees(Math.atan2(target.x-player.x,target.y-player.y)+Math.toRadians(90))));
			posY-=Math.sin(Math.toRadians(-Math.toDegrees(Math.atan2(target.x-player.x,target.y-player.y)+Math.toRadians(90))));
		}
		if (hitTarget) return true;
		return false;
	}
}

enum Type {
	PISTOL(AmmoType.PISTOL, 20),
	SHOTGUN(AmmoType.SHOTGUN,8),
	AR(AmmoType.AR, 30),
	MINIGUN(AmmoType.MINIGUN, 30),
	RAIL(AmmoType.RAIL, 40),
	ROCKET(AmmoType.ROCKET),
	LASER(AmmoType.LASER),
	FRAG(GrenadeType.FRAG),
	FLASH(GrenadeType.FLASH),
	CLOAK(UtilityType.CLOAK),
	C4(UtilityType.C4);

	public static ArrayList<Type> types = new ArrayList<>();
	public static ArrayList<Type> aiTypes = new ArrayList<>();
	TypeTypes type;
	AmmoType gun;
	GrenadeType grenade;
	UtilityType utility;
	double range;

	static {
		types.addAll(Arrays.asList(PISTOL,SHOTGUN,AR,RAIL,MINIGUN,/*LASER,*/ROCKET,FRAG,FLASH,CLOAK,C4));
		aiTypes.addAll(Arrays.asList(SHOTGUN,RAIL,AR,MINIGUN,PISTOL));
	}

	Type(AmmoType type) {
		this.type = TypeTypes.GUN;
		gun = type;
		this.range = 0;
	}

	Type(AmmoType type, double range) {
		this.type = TypeTypes.GUN;
		gun = type;
		this.range = range;
	}

	Type(GrenadeType type) {
		this.type = TypeTypes.GRENADE;
		grenade = type;
	}

	Type(UtilityType type) {
		this.type = TypeTypes.UTILITY;
		utility = type;
	}

	static double damage(AmmoType ammoType, double distance) {
		if (distance<=ammoType.damage[1]) return ammoType.damage[0];
		if (distance>=ammoType.damage[3]) return ammoType.damage[2];
		return (int) (ammoType.damage[0]-((distance-ammoType.damage[3])/(ammoType.damage[3]-ammoType.damage[1])*ammoType.damage[2]));
	}

	static int damage(double damage, int range, double distance) {
		if (distance>range) return 0;
		return (int)(damage-(distance/range)*damage);
	}

	static double distance(double x1, double y1, int target) {
		try {double x2 = WG.players.get(target).x, y2 = WG.players.get(target).y;
		return Math.hypot(Math.abs(x1-x2), Math.abs(y1-y2))/WG.cellSize;} catch (IndexOutOfBoundsException e) {}
		return 0;
	}

	static double distance(double iX, double iY, double x, double y) {
		return Math.hypot(Math.abs(iX-x), Math.abs(iY-y))/WG.cellSize;
	}
}

enum TypeTypes {
	GUN,GRENADE,UTILITY;
}

enum UtilityType {
	CLOAK,
	C4;

	static final double cloakDuration = 2500, cloakCost = cloakDuration/4, cloakRegen = 4d, cloakMin = 0.25d;
	static final int C4BlastSize = 8, C4RPM = 25, C4ammoMax = Ammo.C4;
}

class SegmentGen {
	static int[][] generateSegment(int[] sides) {
		int[][] layout = new int[15][15];
		boolean large = false;
		for (int i = 0;i<4;i++) if (sides[i]==3) large = true;

		for (int y = 0;y<15;y++) for (int x = 0;x<15;x++) layout[y][x] = 0;
		int[][] inner = rotate(inSide, new Random().nextInt(4), new Random().nextInt(3)), innerL = rotate(inSideL, new Random().nextInt(4), new Random().nextInt(3));
		if (!(sides[0]==0&&sides[1]==0&&sides[2]==0&&sides[3]==0)) for (int y = 0;y<13;y++) for (int x = 0;x<13;x++) layout[y+1][x+1] = inner[y][x];
		if (large) layout = innerL;
		if (!large) for (int i = 1;i<13;i++) {
			if (sides[0]==1) layout[1][i+1] = inSide1[i];
			if (sides[0]==2) layout[1][i+1] = inSide2[i];
			if (sides[2]==1) layout[13][i+1] = inSide1[i];
			if (sides[2]==2) layout[13][i+1] = inSide2[i];
			if (sides[3]==1) layout[i+1][1] = inSide1[i];
			if (sides[3]==2) layout[i+1][1] = inSide2[i];
			if (sides[1]==1) layout[i+1][13] = inSide1[i];
			if (sides[1]==2) layout[i+1][13] = inSide2[i];
		}
		for (int i = 0;i<15;i++) {
			if (sides[0]==1) layout[0][i] = side1[i];
			if (sides[0]==2) layout[0][i] = side2[i];
			if (sides[0]==3) layout[0][i] = side3[i];
			if (sides[2]==1) layout[14][i] = side1[i];
			if (sides[2]==2) layout[14][i] = side2[i];
			if (sides[2]==3) layout[14][i] = side3[i];
			if (sides[3]==1) layout[i][0] = side1[i];
			if (sides[3]==2) layout[i][0] = side2[i];
			if (sides[3]==3) layout[i][0] = side3[i];
			if (sides[1]==1) layout[i][14] = side1[i];
			if (sides[1]==2) layout[i][14] = side2[i];
			if (sides[1]==3) layout[i][14] = side3[i];
		}
		if (large) for (int i = 0;i<15;i++) {
			if (sides[0]==0) layout[0][i] = side0L[i];
			if (sides[0]==1) layout[0][i] = side1L[i];
			if (sides[0]==2) layout[0][i] = side2L[i];
			if (sides[2]==0) layout[14][i] = side0L[i];
			if (sides[2]==1) layout[14][i] = side1L[i];
			if (sides[2]==2) layout[14][i] = side2L[i];
			if (sides[3]==0) layout[i][0] = side0L[i];
			if (sides[3]==1) layout[i][0] = side1L[i];
			if (sides[3]==2) layout[i][0] = side2L[i];
			if (sides[1]==0) layout[i][14] = side0L[i];
			if (sides[1]==1) layout[i][14] = side1L[i];
			if (sides[1]==2) layout[i][14] = side2L[i];
		}

		//int sideCount = 0;
		//for (int i = 0;i<4;i++) if (sides[i]!=0) sideCount++;
		//System.out.println(sideCount);

		return layout;
	}

	private static int[][] rotate(int[][] layout, int rotate, int mirror) {
		if (mirror==1) {//x
			int[][] tempLayout = layout;
			layout = new int[layout[0].length][layout.length];
			for (int y = 0;y<layout.length;y++) for (int x = 0;x<layout[0].length;x++) layout[y][layout[0].length-x-1] = tempLayout[y][x];
		}
		if (mirror==2) {//y
			int[][] tempLayout = layout;
			layout = new int[layout[0].length][layout.length];
			for (int y = 0;y<layout.length;y++) for (int x = 0;x<layout[0].length;x++) layout[layout.length-y-1][x] = tempLayout[y][x];
		}
		for (int i = 0;i<rotate;i++) {//90 degrees per
			int[][] tempLayout = layout;
			layout = new int[layout[0].length][layout.length];
			for (int y = 0;y<layout.length;y++) for (int x = 0;x<layout[0].length;x++) layout[x][layout[0].length-y-1] = tempLayout[y][x];
		}
		return layout;
	}

	static int[] side1 = { 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0};
	static int[] side2 = { 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0};
	static int[] side3 = { 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1};
	static int[] side0L = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
	static int[] side1L = { 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1};
	static int[] side2L = { 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1};
	static int[] inSide1 = { 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1};
	static int[] inSide2 = { 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1};
	static int[] inSide3 = {10,10,10,10,10,10,10,10,10,10,10,10,10};

	static int[][] inSide = {
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,91,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,90,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
	};

	static int[][] inSideL = {
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,91,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,90,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
	};
}

class LevelLayout {
	static final int[][] textureTest = {
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2,   2},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10},
			{1020,1021,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,1090,1091},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11},
			{1120,1121,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,  11,1190,1191},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12},
			{1220,1221,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,  12,1290,1291},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13},
			{1320,1321,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,  13,1390,1391},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14},
			{1420,1421,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,  14,1490,1491},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15},
			{1520,1521,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,  15,1590,1591},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16},
			{1620,1621,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,  16,1690,1691}
	};

	static final int[][] buttonsLarge = {
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,7,0,0,1,1,1,1,1,0,0,7,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
	};

	static final int[][] buttons = {
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
	};

	static final int[][] buttonsSmall = {
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,3,3,3,3,3,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,3,3,3,3,3,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,3,3,3,3,3,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,3,3,3,3,3,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,3,3,3,3,3,0,0,0,0,0,1,1,1,1,1,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,0,0,9,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,1,1,1,1,1,1},
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
	};

	static final int[][] test = {
		{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1},
		{   1,  10,  10,  10,1090,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,1090,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1},
		{   1,  10,  10,  10,1090,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,1090,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1},
		{   1,  10,  10,  10,1090,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,1090,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,  10,1020,  10,  10,  10,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,  10,  10,  10,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,1091,1091,1091,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,1091,1091,1091,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,  10,  10,  10,1091,1091,1091,  10,  10,  10,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0},
		{   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   1,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0,   0}
	};

	static final int[][] aiTest = {
		{1,1,1,1,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
		{1,7,7,7,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,7,7,7,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,1},
		{1,7,7,7,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0,0,5,5,0,0,0,0,0,0,5,5,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,5,5,0,0,9,0,0,0,5,5,0,0,0,0,0,1,1,1,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,5,0,0,0,0,0,0,5,5,0,0,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,0,0,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,2,2,2,2,1,0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0,0,0,1,2,2,1,0,0,0,1},
		{1,4,4,4,5,5,5,5,5,5,1,2,2,2,2,1,0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,2,2,2,2,1,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,2,2,2,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,2,2,2,2,1,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,1,2,2,1,0,0,0,1},
		{1,5,5,5,5,5,5,4,4,4,1,2,2,2,2,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,0,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,0,0,0,0,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,0,1},
		{1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
		{1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
	};
}

class Segment {
	public static ArrayList<int[][][]> segments = new ArrayList<>();
	public static ArrayList<int[][][]> allSegments = new ArrayList<>();
	public static ArrayList<Segment> generatedSegments = new ArrayList<>();
	static final boolean editSegment = false;

	int sizeY = 15, sizeX = 15, up, right, down, left;
	int[][] layout;
	boolean continuous;

	Segment(int[] sides) {
		int[][] segment = SegmentGen.generateSegment(sides);
		up = sides[0];
		right = sides[1];
		down = sides[2];
		left = sides[3];
		layout = segment;
	}

	Segment(int segmentId, int rotate, int mirror) {
		sizeY = segments.get(segmentId)[0].length;
		sizeX = segments.get(segmentId)[0][0].length;
		up = segments.get(segmentId)[1][0][0];
		right = segments.get(segmentId)[1][0][1];
		down = segments.get(segmentId)[1][0][2];
		left = segments.get(segmentId)[1][0][3];
		continuous = (segments.get(segmentId)[1][0][5]==1)?true:false;
		layout = rotate(segments.get(segmentId)[0],rotate,mirror);
	}

	private int[][] rotate(int[][] layout, int rotate, int mirror) {
		if (mirror==1) {//x
			int[][] tempLayout = layout;
			layout = new int[layout[0].length][layout.length];
			for (int y = 0;y<layout.length;y++) for (int x = 0;x<layout[0].length;x++) layout[y][layout[0].length-x-1] = tempLayout[y][x];
			int temp = left;left = right;right = temp;//left
		}
		if (mirror==2) {//y
			int[][] tempLayout = layout;
			layout = new int[layout[0].length][layout.length];
			for (int y = 0;y<layout.length;y++) for (int x = 0;x<layout[0].length;x++) layout[layout.length-y-1][x] = tempLayout[y][x];
			int temp = up;up = down;down = temp;//up
		}
		for (int i = 0;i<rotate;i++) {//90 degrees per
			int[][] tempLayout = layout;
			layout = new int[layout[0].length][layout.length];
			for (int y = 0;y<layout.length;y++) for (int x = 0;x<layout[0].length;x++) layout[x][layout[0].length-y-1] = tempLayout[y][x];
			int temp = up;up = left;left = down;down = right;right = temp;
		}
		return layout;
	}

	int getSpot(int y, int x) {
		return layout[y][x];
	}

	public static void initSegments() {
		segments.clear();
		allSegments.clear();
		allSegments.addAll(Arrays.asList(
			//blank,
			segSmall0,segSmall1,segSmall2,segSmall3,
			loot0,loot1,loot2,loot3,loot4,
			spawn0,spawn1,spawn2,spawn3,spawn4,
			segMedium0,segMedium1,segMedium2,segMedium3,
			spawnMedium1,spawnMedium2,spawnMedium3,spawnMedium4,
			lootMedium0,lootMedium1,lootMedium2,lootMedium3,lootMedium4,
			segAdapt1102,segAdapt1202,segAdapt1222,segAdapt2101,segAdapt2102,segAdapt2111,segAdapt2112,segAdapt2121,segAdapt1002,segAdapt2010,
			lootAdapt1102,lootAdapt1202,lootAdapt1222,lootAdapt2101,lootAdapt2102,lootAdapt2111,lootAdapt2112,lootAdapt2121,
			seg0,seg1,seg2,seg3,seg4,seg5,
			lootBig1030,lootBig3300,spawnBig1230,lootBig3200,segBig1320,spawnBig3030,
			roomSpawn11661,roomLoot12661,roomLoot12662,roomSpawn10660,roomSpawn12660,roomLoot11660
		));
		for (int[][][] i:allSegments) for (int rarity = 0;rarity<i[1][0][4];rarity++) segments.add(i);
	}

	//SEGMENTS (under this)

	public final static int[][][] seg0 = {{
		{ 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,12,1291,12,1291,12,1291,12,1291,12,1291,12, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,12,12,1220,1220,1220,1220,1220,1220,1220,1220,1220, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,1220,1220,1220,1220,1220,1220,1220,1220,1220,12,12, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,12,12,1220,1220,1220,1220,1220, 2, 2, 2, 2, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1,12,12,12,12,12,12,12,12,12,12,12, 1, 0},
		{ 0, 1, 1, 1, 1, 1,12,12,12, 1, 1, 1, 1, 1, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,0,1,0,2,0}}};

	public static final int[][][] seg1 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10, 2, 2,1021,1021,1021, 2, 2,10,10, 1, 0},
		{ 1, 1,10,10, 2,10,10,10,10,10, 2,10,10, 1, 1},
		{10,10,10,10,1020,10,10,10,10,10,1020,10,10,10,10},
		{10,10,10,10,1020,10,1091,10,1091,10,1020,10,10,10,10},
		{10,10,10,10,1020,10,10,10,10,10,1020,10,10,10,10},
		{ 1, 1,10,10, 2,10,10,10,10,10, 2,10,10, 1, 1},
		{ 0, 1,10,10, 2, 2,1020,1020,1020, 2, 2,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{0,1,1,1,5,1}}};

	public static final int[][][] seg2 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10, 2, 2,1021,1021,1021, 2, 2,10,10, 1, 0},
		{ 1, 1,10,10, 2,10,10,10,10,10, 2,10,10, 1, 1},
		{10,10,10,10,1020,10,1091,10,1091,10,1020,10,10,10,10},
		{10,10,10,10,1020,10,10,10,10,10,1020,10,10,10,10},
		{10,10,10,10,1020,10,1091,10,1091,10,1020,10,10,10,10},
		{ 1, 1,10,10, 2,10,10,10,10,10, 2,10,10, 1, 1},
		{ 0, 1,10,10, 2, 2,1020,1020,1020, 2, 2,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,1,2,1,5,1}}};

	public static final int[][][] seg3 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10, 2, 2,1021,1021,1021, 2, 2,10,10, 1, 0},
		{ 1, 1,10,10, 2,10,10,10,10,10, 2,10,10, 1, 1},
		{10,10,10,10,1021,10,1091,10,1091,10,1021,10,10,10,10},
		{10,10,10,10,1021,10,10,1090,10,10,1021,10,10,10,10},
		{10,10,10,10,1021,10,1091,10,1091,10,1021,10,10,10,10},
		{ 1, 1,10,10, 2,10,10,10,10,10, 2,10,10, 1, 1},
		{ 0, 1,10,10, 2, 2,1021,1021,1021, 2, 2,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{1,1,2,1,7,1}}};

	public static final int[][][] seg4 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,1091,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 1, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 1},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,1090,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1,10,10,10,10,10,10,10,10,10,10,10, 1, 0},
		{ 0, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{ 1, 1, 1, 1, 2, 1}}};

	public static final int[][][] seg5 = {{
	    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	    { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
	    { 0, 1,11,11,11,11,11,11,11,11,11,11,11, 1, 0},
	    { 0, 1,11,11,11,11,11,11,11,11,11,11,11, 1, 0},
	    { 0, 1,11,11,11,11,11,11,11, 2, 2,11,11, 1, 0},
	    { 1, 1,11,11,11,11,11,11,11, 2, 2,11,11, 1, 1},
	    {10,10,11,11,11,11,11,11,11,11,11,11,11,10,10},
	    {10,10,11,11,11,11,11,11,11,11,11,11,11,10,10},
	    {10,10,11,11,11,11,11,11,11,11,11,11,11,10,10},
	    { 1, 1,11,11, 2, 2,11,11,11,11,11,11,11, 1, 1},
	    { 0, 1,11,11, 2, 2,11,11,11,11,11,11,11, 1, 0},
	    { 0, 1,11,11,11,11,11,11,11,11,11,1190,11, 1, 0},
	    { 0, 1,11,11,11,11,11,11,11,11,11,11,11, 1, 0},
	    { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0},
	    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{0,1,0,1,3,1}}};

	public static final int[][][] blank = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{0,0,0,0,1,0}}};

	public static final int[][][] empty = {{{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{0,0,0,0,0,1}}};

	public static final int[][][] segSmall0 = {{{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{1,1,1,1,1,1}}};

	public static final int[][][] segSmall1 = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,1,1,0,1,1}}};

	public static final int[][][] segSmall2 = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,1,1,1,1,1}}};

	public static final int[][][] segSmall3 = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	},{{0,1,0,1,1,1}}};

	public static final int[][][] spawn0 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 1, 1, 1, 1, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10,11,11,11,11, 1, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10,11,11,1190,11, 1, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10,11,11,11,11, 1, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10,11,11,11,11, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{1,1,1,1,2,1}}};

	public static final int[][][] spawn1 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,1190,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,1,1,0,2,1}}};

	public static final int[][][] spawn2 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,1190,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,11,11,11, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,1,1,1,2,1}}};

	public static final int[][][] spawn3 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,1190,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,11,11,11, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	},{{0,1,0,1,1,1}}};

	public static final int[][][] spawn4 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,1190,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,0,1,0,1,0}}};

	public static final int[][][] loot0 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{1,1,1,1,5,1}}};

	public static final int[][][] loot1 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 0, 0, 1,10,1091,10,10,10,10,10,10,10},
		{ 0, 0, 0, 0, 0, 1,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,1,1,0,3,1}}};

	public static final int[][][] loot2 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,1091,10,10,10,10,10,1091,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,1,1,1,4,1}}};

	public static final int[][][] loot3 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,1091,10,10,10,10,10,1091,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	},{{0,1,0,1,2,1}}};

	public static final int[][][] loot4 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,1091,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,1091,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	},{{0,0,1,0,1,0}}};

	public static final int[][][] segMedium0 = {{{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{2,2,2,2,1,1}}};

	public static final int[][][] segMedium1 = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,2,2,0,1,1}}};

	public static final int[][][] segMedium2 = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,2,2,2,1,1}}};

	public static final int[][][] segMedium3 = {{{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{0,2,0,2,1,1}}};

	public static final int[][][] segAdapt1222 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10, 2, 2,10,10,10,10,10,10,10, 2, 2,10,10},
		{10,10, 2, 2,10,10,10,10,10,10,10, 2, 2,10,10},
		{10,10, 2, 2,10,10,10,10,10,10,10, 2, 2,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{1,2,2,2,2,1}}};

	public static final int[][][] segAdapt2111 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{2,1,1,1,2,1}}};

	public static final int[][][] segAdapt2121 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10, 2,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10, 2,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10, 2,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{2,1,2,1,2,1}}};

	public static final int[][][] segAdapt2101 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{2,1,0,1,2,1}}};

	public static final int[][][] segAdapt1202 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10, 2, 2, 2, 2, 2, 2, 2, 2, 2,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{1,2,0,2,3,1}}};

	public static final int[][][] segAdapt2102 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10, 2, 2, 2,10,10,10,10,10,10,10,10,10},
		{10,10,10, 2, 2, 2,10,10,10,10,10,10,10,10,10},
		{10,10,10, 2, 2, 2,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{2,1,0,2,3,1}}};

	public static final int[][][] segAdapt2112 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{10,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{10,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{2,1,1,2,3,1}}};

	public static final int[][][] segAdapt1102 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10, 2, 2, 2,10,10,10,10,10,10,10,10},
		{10,10,10,10, 2, 2, 2,10,10,10,10,10,10,10,10},
		{10,10,10,10, 2, 2, 2,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{1,1,0,2,1,1}}};

	public static final int[][][] segAdapt1002 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{1,0,0,2,5,1}}};

	public static final int[][][] segAdapt2010 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10, 2, 2, 2,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1, 1, 1,10,10,10, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{2,0,1,0,2,1}}};

	public static final int[][][] lootMedium0 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10, 2, 2, 2,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 2,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10, 2,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 2,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{2,2,2,2,6,1}}};

	public static final int[][][] lootMedium1 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,1091,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,2,2,0,2,1}}};

	public static final int[][][] lootMedium2 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10, 2,10,10,10, 2,10,10,10,10,10},
		{10,10,10,10,10, 2,10,1091,10, 2,10,10,10,10,10},
		{10,10,10,10,10, 2,10,10,10, 2,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,2,2,2,3,1}}};

	public static final int[][][] lootMedium3 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10, 2,10,10,10, 2,10,10,10,10,10},
		{10,10,10,10,10, 2,10,1091,10, 2,10,10,10,10,10},
		{10,10,10,10,10, 2,10,10,10, 2,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{0,2,0,2,2,1}}};

	public static final int[][][] lootMedium4 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,1091,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,0,2,0,1,0}}};

	public static final int[][][] lootAdapt1222 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10, 2,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{1,2,2,2,2,1}}};

	public static final int[][][] lootAdapt2111 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,1091,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10, 2, 2, 2,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{2,1,1,1,2,1}}};

	public static final int[][][] lootAdapt2121 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,1091,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,1091,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{2,1,2,1,3,1}}};

	public static final int[][][] lootAdapt2101 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{2,1,0,1,2,1}}};

	public static final int[][][] lootAdapt1202 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{1,2,0,2,2,1}}};

	public static final int[][][] lootAdapt2102 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{2,1,0,2,2,1}}};

	public static final int[][][] lootAdapt2112 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0}
	},{{2,1,1,2,3,1}}};

	public static final int[][][] lootAdapt1102 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{1,1,0,2,2,1}}};

	public static final int[][][] spawnMedium1 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,1090,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10,10,10,10,10},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,2,2,0,2,1}}};

	public static final int[][][] spawnMedium2 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{10,10,10,10, 2, 2,10,10,10, 2, 2,10,10,10,10},
		{10,10,10,10, 2, 2,10,1090,10, 2, 2,10,10,10,10},
		{10,10,10,10, 2, 2,10,10,10, 2, 2,10,10,10,10},
		{10,10,10,10, 2, 2,10,10,10, 2, 2,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0}
	},{{0,2,2,2,4,1}}};

	public static final int[][][] spawnMedium3 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,1090,10,10,10,10,10,10,10},
		{10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	},{{0,2,0,2,4,1}}};

	public static final int[][][] spawnMedium4 = {{
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,1190,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 0, 0, 1,11,11,11, 1, 0, 0, 0, 0, 0},
		{ 0, 0, 0, 1, 1, 1,11,11,11, 1, 1, 1, 0, 0, 0}
	},{{0,0,2,0,1,0}}};

	public static final int[][][] lootBig1030 = {{
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10, 2, 2, 2, 2, 2,10,10,10,10, 1},
		{ 1,10,10,10,10, 2, 2, 2, 2, 2,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,1091,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10, 2, 2, 2, 2, 2,10,10,10,10, 1},
		{ 1,10,10,10,10, 2, 2, 2, 2, 2,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1}
	},{{1,0,3,0,3,1}}};

	public static final int[][][] spawnBig3030 = {{
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10, 2, 2, 2, 2, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,1091,10,10,10,10,10,10,1090,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10, 2, 2, 2, 2, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1}
	},{{3,0,3,0,4,1}}};

	public static final int[][][] lootBig3300 = {{
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10, 2, 2,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10, 2, 2,10,10,1090,10,10,10,10,10,10},
		{ 1,10,10,10, 2, 2,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10, 2, 2,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10, 2, 2, 2, 2, 2, 2,10,10,10,10,10},
		{ 1,10,10,10, 2, 2, 2, 2, 2, 2,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,1090,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
	},{{3,3,0,0,4,1}}};

	public static final int[][][] lootBig3200 = {{
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10, 2, 2, 2, 2,1021,1021,1021, 1},
		{ 1,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,1020,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,1020,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,1020,10,10,1090,10,10,10,10},
		{ 1,10,10,10,10,10,10,1020,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,1020,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10, 2,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10, 2, 2, 2, 2,1021,1021,1021, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
	},{{3,2,0,0,2,1}}};

	public static final int[][][] spawnBig1230 = {{
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,1090,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10, 1}
	},{{1,2,3,0,1,1}}};

	public static final int[][][] segBig1320 = {{
		{ 1, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,1091,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{ 1,10,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
		{ 1,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
		{ 1, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1}
	},{{1,3,2,0,2,1}}};

	public static final int[][][] roomLoot12661 = {{
	    { 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
	    { 0, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 1, 1,10,10,10, 1, 1, 1, 1, 1, 1, 1, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 1, 1, 1, 1, 1,10,10},
	    { 1, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,1091,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{2,6,6,1,10,1}}};

	public static final int[][][] roomSpawn11661 = {{
	    { 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	    { 0, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 1, 1,10,10,10, 1, 1, 1, 1, 1, 1, 1, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 1, 1, 1, 1, 1,10,10},
	    { 1, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,1090,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{1,6,6,1,10,1}}};

	public static final int[][][] roomLoot12662 = {{
	    { 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
	    { 0, 1, 1, 1,10,10, 2, 2, 2,10,10, 1, 1, 1, 1},
	    { 0, 1,10,10,10,10, 2, 2, 2,10,10,10,10,10,10},
	    { 1, 1,10,1091,10,10,10,10,10,10,10,10,10,10,10},
	    {10,10,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    {10,10,10,10,10, 1, 1, 1, 1, 1, 1, 1, 1,10,10},
	    {10, 2, 2,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    {10, 2, 2,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    {10, 2, 2,10,10, 1, 0, 0, 1, 1, 1, 1, 1,10,10},
	    {10,10,10,10,10, 1, 0, 0, 1,10,10,10, 2,10,10},
	    {10,10,10,10,10, 1, 0, 0, 1,10,10,10, 2,10,10},
	    { 1, 1,10,10,10, 1, 0, 0, 1,10,10,10, 2,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1,10,10,10, 2,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{2,6,6,2,10,1}}};

	public static final int[][][] roomSpawn10660 = {{
	    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
	    { 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,1090,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1, 1, 1, 1, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1, 1, 1, 1, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{0,6,6,0,6,0}}};

	public static final int[][][] roomSpawn12660 = {{
		{ 0, 0, 0, 1,10,10,10,10,10,10,10, 1, 0, 0, 0},
	    { 0, 1, 1, 1,10,10,10,10,10,10,10, 1, 1, 1, 1},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,1091,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1, 1, 1, 1, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1, 1, 1, 1, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,1090,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{2,6,6,0,2,1}}};

	public static final int[][][] roomLoot11660 = {{
		{ 0, 0, 0, 0, 0, 1,10,10,10, 1, 0, 0, 0, 0, 0},
	    { 0, 1, 1, 1, 1, 1,10,10,10, 1, 1, 1, 1, 1, 1},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,1091,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1, 1, 1, 1, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 0, 0, 0, 0, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1, 1, 1, 1, 1,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,1091,10,10,10,10},
	    { 0, 1,10,10,10, 1, 0, 0, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10, 1, 1, 1, 1,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10},
	    { 0, 1,10,10,10,10,10,10,10,10,10,10,10,10,10}
	},{{1,6,6,0,2,1}}};

	static {initSegments();}
}













