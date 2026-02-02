import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * PROCEDURAL DUNGEON SURVIVAL GAME - OPTIMIZED 60+ FPS VERSION
 * ============================================================
 * A high-performance roguelike survival game with smooth 60+ FPS!
 * 
 * PERFORMANCE OPTIMIZATIONS:
 * - Separate render loop (60 FPS) and game logic loop (10 TPS)
 * - Double buffering for flicker-free rendering
 * - Optimized rendering (only visible tiles)
 * - Cached sprite rendering
 * - Smooth interpolation between game states
 * - Hardware acceleration enabled
 * 
 * FEATURES:
 * - Procedurally generated dungeons using cellular automata
 * - Smooth player movement with WASD or Arrow keys
 * - Enemies that chase and attack the player
 * - Health and hunger systems
 * - Collectible items (health potions, food, weapons)
 * - Combat system with visual feedback
 * - Multiple dungeon levels
 * - Custom images support for player, walls, enemies, and items
 * - FPS counter display
 * 
 * HOW TO ADD IMAGES:
 * ------------------
 * Same as before - create assets/ folder with subfolders.
 * See previous documentation for details.
 * 
 * @author Junior Developer (Performance Edition)
 * @version 2.0 - 60+ FPS Optimized
 */
public class DungeonSurvival extends JPanel implements KeyListener, Runnable {
    
    // ==================== CONSTANTS ====================
    
    /** Size of each tile in pixels */
    private static final int TILE_SIZE = 32;
    
    /** Width of the dungeon in tiles */
    private static final int DUNGEON_WIDTH = 40;
    
    /** Height of the dungeon in tiles */
    private static final int DUNGEON_HEIGHT = 30;
    
    /** Target frames per second for rendering */
    private static final int TARGET_FPS = 60;
    
    /** Target game logic updates per second (ticks) */
    private static final int TARGET_TPS = 10;
    
    /** Nanoseconds per frame for 60 FPS */
    private static final long OPTIMAL_TIME = 1000000000 / TARGET_FPS;
    
    /** Game logic update interval in nanoseconds */
    private static final long UPDATE_INTERVAL = 1000000000 / TARGET_TPS;
    
    /** Player starting health */
    private static final int STARTING_HEALTH = 100;
    
    /** Player starting hunger */
    private static final int STARTING_HUNGER = 100;

    private Canvas gameCanvas;
    // ==================== GAME STATE ====================
    
    /** The main game thread */
    private Thread gameThread;
    
    /** Flag to control game thread */
    private volatile boolean running;
    
    /** The player character */
    private Player player;
    
    /** The current dungeon level */
    private Dungeon dungeon;
    
    /** List of all enemies in the current level */
    private List<Enemy> enemies;
    
    /** List of all items in the current level */
    private List<Item> items;
    
    /** Current dungeon level number */
    private int currentLevel;
    
    /** Game over flag */
    private boolean gameOver;
    
    /** Victory flag */
    private boolean victory;
    
    /** Camera offset X for viewport (with smooth interpolation) */
    private double cameraX;
    
    /** Camera offset Y for viewport (with smooth interpolation) */
    private double cameraY;
    
    /** Target camera X (for smooth following) */
    private double targetCameraX;
    
    /** Target camera Y (for smooth following) */
    private double targetCameraY;
    
    /** Image cache for sprites */
    private Map<String, Image> imageCache;
    
    /** Double buffer for rendering */
    private BufferedImage backBuffer;
    
    /** Graphics context for back buffer */
    private Graphics2D backBufferGraphics;
    
    /** FPS counter */
    private int fps;
    
    /** TPS counter */
    private int tps;
    
    /** Last FPS update time */
    private long lastFpsTime;
    
    /** Frame counter */
    private int frameCount;
    
    /** Update counter */
    private int updateCount;

    private SoundManager soundManager; // Add this line here!
    
    // ==================== CONSTRUCTOR ====================
    
    /**
     * Creates a new optimized Dungeon Survival game instance.
     * Initializes all game components with performance optimizations.
     */
    class SoundManager {
        private Map<String, Clip> clips = new HashMap<>();

        public void loadSound(String name, String path) {
            try {
                File file = new File(path);
                if (!file.exists()) {
                    System.out.println("File not found: " + path);
                    return;
                }
                AudioInputStream ai = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(ai);
                clips.put(name, clip);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // New method to set volume (0.0 to 1.0)
        public void setVolume(String name, float volume) {
            Clip clip = clips.get(name);
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert linear 0.0-1.0 to decibels (which Java uses)
                float dB = (float) (Math.log(volume != 0 ? volume : 0.0001) / Math.log(10.0) * 20.0);
                gainControl.setValue(dB);
            }
        }

        public void play(String name) {
            Clip clip = clips.get(name);
            if (clip != null) {
                if (clip.isRunning()) clip.stop(); // Stop if already playing
                clip.setFramePosition(0);         // Rewind to the beginning
                clip.start();                     // Start!
            } else {
                // Helpful debug to see if a specific name is misspelled
                System.out.println("Sound not found in manager: " + name);
            }
        }

        public void loop(String name) {
            Clip clip = clips.get(name);
            if (clip != null) {
                clip.setFramePosition(0);
                clip.loop(Clip.LOOP_CONTINUOUSLY); // This keeps the music playing
            }
        }
    }
    public DungeonSurvival() {
        // Set up the panel with performance hints
        setPreferredSize(new Dimension(1280, 720));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        gameCanvas = new Canvas();
        gameCanvas.setPreferredSize(new Dimension(1280, 720));
        gameCanvas.setFocusable(false); // Let the Panel handle keys
        this.setLayout(new BorderLayout());
        this.add(gameCanvas, BorderLayout.CENTER);
        
        // Enable double buffering and hardware acceleration
        setDoubleBuffered(true);
        
        // Initialize game state
        imageCache = new HashMap<>();
        currentLevel = 1;
        gameOver = false;
        victory = false;
        fps = 0;
        tps = 0;
        lastFpsTime = System.nanoTime();
        frameCount = 0;
        updateCount = 0;
        
        // Load images
        loadImages();

        soundManager = new SoundManager();

        // Load Music
        soundManager.loadSound("bgm", "assets/sounds/music/background_theme.wav");
        soundManager.setVolume("bgm", 0.4f); // 40% Volume
        soundManager.loop("bgm");           // Start playing and looping

        // Load SFX (100% volume by default)
        // If you structured it like the previous step, make sure "sfx/" is in the string!
        soundManager.loadSound("potion", "assets/sounds/sfx/drink.wav");
        soundManager.loadSound("food", "assets/sounds/sfx/eat.wav");
        soundManager.loadSound("death", "assets/sounds/sfx/death.wav");
        soundManager.loadSound("weapon", "assets/sounds/sfx/weapon_pickup.wav");

        // Initialize the first level
        initializeLevel();
    }
    
    /**
     * Starts the game thread for smooth 60+ FPS rendering.
     */
    public void startGame() {
        if (gameThread == null || !running) {
            running = true;
            gameThread = new Thread(this, "GameThread");
            gameThread.start();
        }
    }
    
    /**
     * Stops the game thread.
     */
    public void stopGame() {
        running = false;
        try {
            if (gameThread != null) {
                gameThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // ==================== GAME LOOP (60+ FPS) ====================
    
    /**
     * Main game loop running in separate thread.
     * Achieves 60+ FPS by separating rendering from game logic.
     * 
     * TECHNIQUE: Fixed timestep game loop
     * - Render at 60 FPS (smooth visuals)
     * - Update logic at 10 TPS (gameplay speed)
     * - Interpolation for smooth movement between updates
     */
    @Override
    public void run() {
        // Create Triple Buffering to stop horizontal bars
        gameCanvas.createBufferStrategy(3);
        BufferStrategy bs = gameCanvas.getBufferStrategy();

        long lastUpdateTime = System.nanoTime();
        long lastFpsTime = System.nanoTime();
        int frames = 0;
        int updates = 0;

        while (running) {
            long now = System.nanoTime();

            // LOGIC UPDATE (10 TPS)
            if (now - lastUpdateTime >= UPDATE_INTERVAL) {
                update();
                lastUpdateTime = now;
                updates++;
            }

            // RENDER (60 FPS)
            Graphics2D g = null;
            try {
                g = (Graphics2D) bs.getDrawGraphics();
                // Apply High Quality Rendering Hints
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                renderToGraphics(g); // Call our new draw method
            } finally {
                if (g != null) g.dispose();
            }

            // Show the frame instantly
            if (!bs.contentsLost()) {
                bs.show();
            }

            // Sync for smooth movement
            Toolkit.getDefaultToolkit().sync();

            frames++;
            if (now - lastFpsTime >= 1000000000) {
                fps = frames;
                tps = updates;
                frames = 0;
                updates = 0;
                lastFpsTime = now;
            }

            // Regulate FPS
            try {
                long sleep = (OPTIMAL_TIME - (System.nanoTime() - now)) / 1000000;
                if (sleep > 0) Thread.sleep(sleep);
            } catch (Exception e) {}
        }
    }

    private void renderToGraphics(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        if (gameOver) { drawGameOver(g); return; }
        if (victory) { drawVictory(g); return; }

        // Camera Lerp
        cameraX += (targetCameraX - cameraX) * 0.2;
        cameraY += (targetCameraY - cameraY) * 0.2;

        drawDungeon(g);
        drawItems(g);
        drawEnemies(g);
        drawPlayer(g);
        drawUI(g);
        drawFPS(g);
    }

    // ==================== INITIALIZATION ====================
    
    /**
     * Loads all game images from the assets folder.
     * Images are cached for better performance.
     */
    private void loadImages() {
        String[] imagePaths = {
            "assets/player/player.png",
            "assets/walls/wall.png",
            "assets/walls/floor.png",
            "assets/enemies/enemy.png",
            "assets/items/health_potion.png",
            "assets/items/food.png",
            "assets/items/weapon.png",
            "assets/items/stairs.png"
        };
        
        for (String path : imagePaths) {
            try {
                File imageFile = new File(path);
                if (imageFile.exists()) {
                    BufferedImage img = ImageIO.read(imageFile);
                    Image scaledImg = img.getScaledInstance(TILE_SIZE, TILE_SIZE, Image.SCALE_SMOOTH);
                    imageCache.put(path, scaledImg);
                    System.out.println("Loaded image: " + path);
                }
            } catch (Exception e) {
                System.out.println("Could not load image: " + path + " (using fallback graphics)");
            }
        }
    }
    
    /**
     * Initializes a new dungeon level with player, enemies, and items.
     */
    private void initializeLevel() {
        dungeon = new Dungeon(DUNGEON_WIDTH, DUNGEON_HEIGHT);
        dungeon.generate();
        
        if (player == null) {
            Point startPos = dungeon.getRandomFloorTile();
            player = new Player(startPos.x, startPos.y);
        } else {
            Point startPos = dungeon.getRandomFloorTile();
            player.x = startPos.x;
            player.y = startPos.y;
        }
        
        // Create enemies
        enemies = new ArrayList<>();
        int enemyCount = 5 + (currentLevel * 2);
        for (int i = 0; i < enemyCount; i++) {
            Point pos = dungeon.getRandomFloorTile();
            enemies.add(new Enemy(pos.x, pos.y, currentLevel));
        }
        
        // Create items
        items = new ArrayList<>();
        int itemCount = 8 + currentLevel;
        for (int i = 0; i < itemCount; i++) {
            Point pos = dungeon.getRandomFloorTile();
            ItemType type = ItemType.values()[(int)(Math.random() * (ItemType.values().length - 1))];
            items.add(new Item(pos.x, pos.y, type));
        }
        
        // Add stairs
        Point stairsPos = dungeon.getRandomFloorTile();
        items.add(new Item(stairsPos.x, stairsPos.y, ItemType.STAIRS));
        
        // Reset camera
        updateCameraTarget();
        cameraX = targetCameraX;
        cameraY = targetCameraY;
    }
    
    // ==================== GAME UPDATE (10 TPS) ====================
    
    /**
     * Updates game logic at 10 TPS.
     * Separated from rendering for consistent gameplay regardless of FPS.
     */
    private void update() {
        if (gameOver || victory) {
            return;
        }
        
        // Update player hunger
        player.updateHunger();
        
        // Check if player died
        if (player.health <= 0) {
            if (!gameOver) soundManager.play("death");
            gameOver = true;
            return;
        }
        
        // Update all enemies
        for (Enemy enemy : enemies) {
            enemy.update(player, dungeon);
        }
        
        // Check enemy-player collisions
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            if (enemy.x == player.x && enemy.y == player.y) {
                int damage = Math.max(1, enemy.damage - player.defense);
                player.health -= damage;
                
                enemy.health -= player.attack;
                
                if (enemy.health <= 0) {
                    enemyIterator.remove();
                }
            }
        }
        
        // Update camera target (smooth following)
        updateCameraTarget();
    }
    
    /**
     * Updates the camera target position.
     * Actual camera smoothly interpolates to this target during rendering.
     */
    private void updateCameraTarget() {
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        
        targetCameraX = player.x * TILE_SIZE - screenWidth / 2.0;
        targetCameraY = player.y * TILE_SIZE - screenHeight / 2.0;
        
        // Clamp to dungeon bounds
        targetCameraX = Math.max(0, Math.min(targetCameraX, DUNGEON_WIDTH * TILE_SIZE - screenWidth));
        targetCameraY = Math.max(0, Math.min(targetCameraY, DUNGEON_HEIGHT * TILE_SIZE - screenHeight));
    }
    
    // ==================== RENDERING (60 FPS) ====================
    
    /**
     * Renders the game to the back buffer.
     * Called 60 times per second for smooth visuals.
     */
    private void render() {
        // Create back buffer if needed
        if (backBuffer == null || backBuffer.getWidth() != getWidth() || backBuffer.getHeight() != getHeight()) {
            backBuffer = new BufferedImage(
                Math.max(1, getWidth()), 
                Math.max(1, getHeight()), 
                BufferedImage.TYPE_INT_ARGB
            );
            backBufferGraphics = backBuffer.createGraphics();
            
            // Enable rendering hints for better quality and performance
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            backBufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        
        Graphics2D g = backBufferGraphics;
        
        // Clear screen
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (gameOver) {
            drawGameOver(g);
            return;
        }
        
        if (victory) {
            drawVictory(g);
            return;
        }
        
        // Smooth camera interpolation (lerp)
        double lerpSpeed = 0.2; // Adjust for smoother/faster camera
        cameraX += (targetCameraX - cameraX) * lerpSpeed;
        cameraY += (targetCameraY - cameraY) * lerpSpeed;
        
        // Draw everything
        drawDungeon(g);
        drawItems(g);
        drawEnemies(g);
        drawPlayer(g);
        drawUI(g);
        drawFPS(g);
    }
    
    /**
     * Paints the back buffer to the screen.
     * This is where double buffering prevents flickering.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backBuffer != null) {
            g.drawImage(backBuffer, 0, 0, null);
        }
        
        // Sync for smoother rendering
        Toolkit.getDefaultToolkit().sync();
    }
    
    /**
     * Draws the dungeon tiles (optimized - only visible tiles).
     */
    private void drawDungeon(Graphics2D g) {
        int startX = Math.max(0, (int)cameraX / TILE_SIZE);
        int startY = Math.max(0, (int)cameraY / TILE_SIZE);
        int endX = Math.min(DUNGEON_WIDTH, (int)(cameraX + getWidth()) / TILE_SIZE + 2);
        int endY = Math.min(DUNGEON_HEIGHT, (int)(cameraY + getHeight()) / TILE_SIZE + 2);
        
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int screenX = (int)(x * TILE_SIZE - cameraX);
                int screenY = (int)(y * TILE_SIZE - cameraY);
                
                if (dungeon.isWall(x, y)) {
                    Image wallImg = imageCache.get("assets/walls/wall.png");
                    if (wallImg != null) {
                        g.drawImage(wallImg, screenX, screenY, null);
                    } else {
                        g.setColor(new Color(70, 70, 70));
                        g.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                        g.setColor(new Color(50, 50, 50));
                        g.drawRect(screenX, screenY, TILE_SIZE - 1, TILE_SIZE - 1);
                    }
                } else {
                    Image floorImg = imageCache.get("assets/walls/floor.png");
                    if (floorImg != null) {
                        g.drawImage(floorImg, screenX, screenY, null);
                    } else {
                        g.setColor(new Color(40, 40, 40));
                        g.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }
    
    /**
     * Draws all items.
     */
    private void drawItems(Graphics2D g) {
        for (Item item : items) {
            int screenX = (int)(item.x * TILE_SIZE - cameraX);
            int screenY = (int)(item.y * TILE_SIZE - cameraY);
            
            // Only draw if on screen
            if (screenX > -TILE_SIZE && screenX < getWidth() && screenY > -TILE_SIZE && screenY < getHeight()) {
                Image itemImg = imageCache.get(item.getImagePath());
                if (itemImg != null) {
                    g.drawImage(itemImg, screenX, screenY, null);
                } else {
                    g.setColor(item.type.color);
                    g.fillOval(screenX + 8, screenY + 8, TILE_SIZE - 16, TILE_SIZE - 16);
                }
            }
        }
    }
    
    /**
     * Draws all enemies with health bars.
     */
    private void drawEnemies(Graphics2D g) {
        for (Enemy enemy : enemies) {
            int screenX = (int)(enemy.x * TILE_SIZE - cameraX);
            int screenY = (int)(enemy.y * TILE_SIZE - cameraY);
            
            // Only draw if on screen
            if (screenX > -TILE_SIZE && screenX < getWidth() && screenY > -TILE_SIZE && screenY < getHeight()) {
                Image enemyImg = imageCache.get("assets/enemies/enemy.png");
                if (enemyImg != null) {
                    g.drawImage(enemyImg, screenX, screenY, null);
                } else {
                    g.setColor(Color.RED);
                    g.fillRect(screenX + 4, screenY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                    g.setColor(Color.DARK_GRAY);
                    g.fillOval(screenX + 10, screenY + 8, 4, 4);
                    g.fillOval(screenX + 18, screenY + 8, 4, 4);
                }
                
                // Health bar
                int barWidth = TILE_SIZE;
                int barHeight = 4;
                g.setColor(Color.BLACK);
                g.fillRect(screenX, screenY - 6, barWidth, barHeight);
                g.setColor(Color.RED);
                int healthWidth = (int)((enemy.health / (float)enemy.maxHealth) * barWidth);
                g.fillRect(screenX, screenY - 6, healthWidth, barHeight);
            }
        }
    }
    
    /**
     * Draws the player character.
     */
    private void drawPlayer(Graphics2D g) {
        int screenX = (int)(player.x * TILE_SIZE - cameraX);
        int screenY = (int)(player.y * TILE_SIZE - cameraY);
        
        Image playerImg = imageCache.get("assets/player/player.png");
        if (playerImg != null) {
            g.drawImage(playerImg, screenX, screenY, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillOval(screenX + 4, screenY + 4, TILE_SIZE - 8, TILE_SIZE - 8);
            g.setColor(Color.WHITE);
            g.fillOval(screenX + 10, screenY + 10, 4, 4);
            g.fillOval(screenX + 18, screenY + 10, 4, 4);
        }
    }
    
    /**
     * Draws the UI with health, hunger, and stats.
     */
    private void drawUI(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(10, 10, 300, 120);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        
        g.drawString("LEVEL: " + currentLevel, 20, 30);
        g.drawString("ENEMIES: " + enemies.size(), 20, 50);
        
        g.drawString("HEALTH:", 20, 75);
        drawBar(g, 100, 65, 200, 15, player.health, STARTING_HEALTH, Color.RED, Color.DARK_GRAY);
        
        g.drawString("HUNGER:", 20, 100);
        drawBar(g, 100, 90, 200, 15, player.hunger, STARTING_HUNGER, Color.ORANGE, Color.DARK_GRAY);
        
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("ATK: " + player.attack + " | DEF: " + player.defense, 20, 120);
        
        g.setColor(new Color(255, 255, 255, 150));
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("WASD/Arrows: Move | Find stairs to next level!", 10, getHeight() - 10);
    }
    
    /**
     * Draws FPS and TPS counter (performance monitoring).
     */
    private void drawFPS(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(getWidth() - 120, 10, 110, 50);
        
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("FPS: " + fps, getWidth() - 110, 30);
        
        g.setColor(Color.CYAN);
        g.drawString("TPS: " + tps, getWidth() - 110, 50);
    }
    
    /**
     * Helper to draw status bars.
     */
    private void drawBar(Graphics2D g, int x, int y, int width, int height, int current, int max, Color fillColor, Color bgColor) {
        g.setColor(bgColor);
        g.fillRect(x, y, width, height);
        
        g.setColor(fillColor);
        int fillWidth = (int)((current / (float)max) * width);
        g.fillRect(x, y, Math.max(0, fillWidth), height);
        
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, height);
    }
    
    /**
     * Draws game over screen.
     */
    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        String text = "GAME OVER";
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, getWidth() / 2 - textWidth / 2, getHeight() / 2 - 50);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        String levelText = "You reached level " + currentLevel;
        textWidth = g.getFontMetrics().stringWidth(levelText);
        g.drawString(levelText, getWidth() / 2 - textWidth / 2, getHeight() / 2);
        
        String restartText = "Press R to restart";
        textWidth = g.getFontMetrics().stringWidth(restartText);
        g.drawString(restartText, getWidth() / 2 - textWidth / 2, getHeight() / 2 + 50);
    }
    
    /**
     * Draws victory screen.
     */
    private void drawVictory(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        String text = "VICTORY!";
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, getWidth() / 2 - textWidth / 2, getHeight() / 2 - 50);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        String levelText = "Completed level " + currentLevel;
        textWidth = g.getFontMetrics().stringWidth(levelText);
        g.drawString(levelText, getWidth() / 2 - textWidth / 2, getHeight() / 2);
    }
    
    // ==================== INPUT HANDLING ====================
    

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                currentLevel = 1;
                player = null;
                gameOver = false;
                victory = false;
                initializeLevel();
            }
            return;
        }

        int newX = player.x;
        int newY = player.y;

        switch (e.getKeyCode()) {
            case KeyEvent.VK_W: case KeyEvent.VK_UP:    newY--; break;
            case KeyEvent.VK_S: case KeyEvent.VK_DOWN:  newY++; break;
            case KeyEvent.VK_A: case KeyEvent.VK_LEFT:  newX--; break;
            case KeyEvent.VK_D: case KeyEvent.VK_RIGHT: newX++; break;
        }

        if (!dungeon.isWall(newX, newY)) {
            player.x = newX;
            player.y = newY;

            // --- ADD THIS LOGIC TO TRIGGER SOUNDS ---
            synchronized(items) {
                Iterator<Item> itemIterator = items.iterator();
                while (itemIterator.hasNext()) {
                    Item item = itemIterator.next();
                    if (item.x == player.x && item.y == player.y) {

                        // Play the correct sound based on item type
                        if (item.type == ItemType.HEALTH_POTION) soundManager.play("potion");
                        else if (item.type == ItemType.FOOD) soundManager.play("food");
                        else if (item.type == ItemType.WEAPON) soundManager.play("weapon");
                        else if (item.type == ItemType.STAIRS) {
                            soundManager.play("level"); // Play level up sound
                            currentLevel++;
                            initializeLevel();
                            return; // Level changed, stop checking items
                        }

                        item.applyEffect(player);
                        itemIterator.remove();
                    }
                }
            }
        }
    }
    private void checkItemPickup() {
        synchronized(items) {
            Iterator<Item> it = items.iterator();
            while (it.hasNext()) {
                Item item = it.next();
                if (item.x == player.x && item.y == player.y) {
                    // Trigger Meme Sounds
                    if (item.type == ItemType.HEALTH_POTION) soundManager.play("potion");
                    else if (item.type == ItemType.FOOD) soundManager.play("food");
                    else if (item.type == ItemType.WEAPON) soundManager.play("weapon");
                    else if (item.type == ItemType.STAIRS) {
                        soundManager.play("level");
                        currentLevel++;
                        initializeLevel();
                        return;
                    }
                    item.applyEffect(player);
                    it.remove();
                }
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {}
    
    @Override
    public void keyTyped(KeyEvent e) {}
    
    // ==================== INNER CLASSES ====================
    
    class Player {
        int x, y;
        int health;
        int hunger;
        int attack;
        int defense;
        int hungerTicks;
        
        Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.health = STARTING_HEALTH;
            this.hunger = STARTING_HUNGER;
            this.attack = 10;
            this.defense = 5;
            this.hungerTicks = 0;
        }
        
        void updateHunger() {
            hungerTicks++;
            if (hungerTicks >= 10) {
                hunger = Math.max(0, hunger - 1);
                hungerTicks = 0;
                
                if (hunger == 0) {
                    health -= 2;
                }
            }
        }
    }
    
    class Enemy {
        int x, y;
        int health;
        int maxHealth;
        int damage;
        int speed;
        int speedCounter;
        
        Enemy(int x, int y, int level) {
            this.x = x;
            this.y = y;
            this.maxHealth = 20 + (level * 10);
            this.health = maxHealth;
            this.damage = 5 + (level * 2);
            this.speed = Math.max(2, 5 - level);
            this.speedCounter = 0;
        }
        
        void update(Player player, Dungeon dungeon) {
            speedCounter++;
            if (speedCounter < speed) {
                return;
            }
            speedCounter = 0;
            
            int dx = Integer.compare(player.x - x, 0);
            int dy = Integer.compare(player.y - y, 0);
            
            if (dx != 0 && !dungeon.isWall(x + dx, y)) {
                x += dx;
            } else if (dy != 0 && !dungeon.isWall(x, y + dy)) {
                y += dy;
            }
        }
    }
    
    enum ItemType {
        HEALTH_POTION(Color.PINK, "assets/items/health_potion.png"),
        FOOD(Color.YELLOW, "assets/items/food.png"),
        WEAPON(Color.CYAN, "assets/items/weapon.png"),
        STAIRS(Color.WHITE, "assets/items/stairs.png");
        
        Color color;
        String imagePath;
        
        ItemType(Color color, String imagePath) {
            this.color = color;
            this.imagePath = imagePath;
        }
    }
    
    class Item {
        int x, y;
        ItemType type;
        
        Item(int x, int y, ItemType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
        
        String getImagePath() {
            return type.imagePath;
        }
        
        void applyEffect(Player player) {
            switch (type) {
                case HEALTH_POTION:
                    player.health = Math.min(STARTING_HEALTH, player.health + 30);
                    break;
                case FOOD:
                    player.hunger = Math.min(STARTING_HUNGER, player.hunger + 40);
                    break;
                case WEAPON:
                    player.attack += 5;
                    player.defense += 2;
                    break;
                case STAIRS:
                    break;
            }
        }
    }

    class Dungeon {
        int width, height;
        boolean[][] walls;

        Dungeon(int width, int height) {
            this.width = width;
            this.height = height;
            this.walls = new boolean[width][height];
        }

        void generate() {
            Random rand = new Random();

            // 1. Initial random noise
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
                        walls[x][y] = true;
                    } else {
                        walls[x][y] = rand.nextDouble() < 0.45;
                    }
                }
            }

            // 2. Run Cellular Automata iterations
            for (int iteration = 0; iteration < 4; iteration++) {
                boolean[][] newWalls = new boolean[width][height];
                for (int y = 1; y < height - 1; y++) {
                    for (int x = 1; x < width - 1; x++) {
                        int wallCount = countAdjacentWalls(x, y);
                        if (wallCount >= 5) newWalls[x][y] = true;
                        else if (wallCount <= 3) newWalls[x][y] = false;
                        else newWalls[x][y] = walls[x][y];
                    }
                }
                // Keep the border intact
                for (int i = 0; i < width; i++) { newWalls[i][0] = true; newWalls[i][height-1] = true; }
                for (int i = 0; i < height; i++) { newWalls[0][i] = true; newWalls[width-1][i] = true; }
                walls = newWalls;
            }

            // 3. FIX: Ensure connectivity (Flood Fill)
            ensureConnectivity();
        }

        private void ensureConnectivity() {
            // Find the largest open area
            Point start = getRandomFloorTile();
            boolean[][] reachable = new boolean[width][height];
            Queue<Point> queue = new LinkedList<>();

            queue.add(start);
            reachable[start.x][start.y] = true;

            // Flood fill to find all tiles connected to the starting point
            while (!queue.isEmpty()) {
                Point p = queue.poll();
                int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};
                for (int[] d : dirs) {
                    int nx = p.x + d[0], ny = p.y + d[1];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height && !walls[nx][ny] && !reachable[nx][ny]) {
                        reachable[nx][ny] = true;
                        queue.add(new Point(nx, ny));
                    }
                }
            }

            // 4. Fill in all unreachable floor tiles with walls
            // This effectively removes "pockets" the player can't reach
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (!walls[x][y] && !reachable[x][y]) {
                        walls[x][y] = true;
                    }
                }
            }
        }
        
        int countAdjacentWalls(int x, int y) {
            int count = 0;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    if (walls[x + dx][y + dy]) count++;
                }
            }
            return count;
        }
        
        boolean isWall(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return true;
            }
            return walls[x][y];
        }
        
        Point getRandomFloorTile() {
            Random rand = new Random();
            int x, y;
            do {
                x = rand.nextInt(width);
                y = rand.nextInt(height);
            } while (walls[x][y]);
            return new Point(x, y);
        }
    }
    
    // ==================== MAIN METHOD ====================
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Procedural Dungeon Survival - 60+ FPS Optimized");
            DungeonSurvival game = new DungeonSurvival();
            
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            
            // Add window listener to stop game thread when closing
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    game.stopGame();
                }
            });
            
            // Start the optimized game loop
            game.startGame();
            
            System.out.println("=================================");
            System.out.println("DUNGEON SURVIVAL - 60+ FPS MODE!");
            System.out.println("=================================");
            System.out.println("Performance optimizations:");
            System.out.println("  ✓ Separate render (60 FPS) and logic (10 TPS) loops");
            System.out.println("  ✓ Double buffering for smooth rendering");
            System.out.println("  ✓ Hardware acceleration enabled");
            System.out.println("  ✓ Smooth camera interpolation");
            System.out.println("  ✓ Optimized tile rendering");
            System.out.println("  ✓ FPS/TPS counter in top-right corner");
            System.out.println("=================================");
        });
    }
}
