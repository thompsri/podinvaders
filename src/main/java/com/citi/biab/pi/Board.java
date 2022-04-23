package com.citi.biab.pi;

import com.citi.biab.pi.sprite.Alien;
import com.citi.biab.pi.sprite.Images;
import com.citi.biab.pi.sprite.Player;
import com.citi.biab.pi.sprite.Shot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(Board.class);

    private static final int ROWS = 4;
    private static final int COLUMNS = 10;

    private String message = "Game Over";

    public final Image explosionImage = Images.getImage("explosion.png");
    public final Image logo = Images.getImage("logo.png");
    public final Image citiLogo = Images.getImage("citi.png");

    private final Font alienFont = new Font("Helvetica", Font.PLAIN, 10);
    private final Font listFont = new Font("Monospaced", Font.PLAIN, 14);
    private final Font scaleFont = new Font("Impact", Font.PLAIN, 24);
    private final Font smallFont = new Font("Helvetica", Font.PLAIN, 14);

    private final NodeColors nodeColors = new NodeColors();

    private Sounds sounds;

    private final Dimension canvasSize = new Dimension(Constants.BOARD_WIDTH, Constants.BOARD_HEIGHT);

    private final Random generator = new Random();

    private final Kubectl kubectl;

    private final double graphicsScale;

    private final List<Alien> aliens = new ArrayList<>();

    private Player player;
    private Shot shot;

    private int direction = -1;
    private int deaths = 0;

    private boolean inGame = true;

    private Timer timer;

    private boolean alienUpMode;
    private long lastUpdate;

    public Board(Kubectl kubectl, double graphicsScale) {
        this.kubectl = kubectl;
        this.graphicsScale = graphicsScale;

        try {
            sounds = new Sounds();
        } catch (LineUnavailableException e) {
            log.error("Error loading sound", e);
        }

        for (String node : kubectl.getNodes()) {
            nodeColors.addNode(node);
        }

        initBoard();
        gameInit();
    }

    private void initBoard() {

        addKeyListener(new TAdapter());
        setFocusable(true);

        setBackground(Color.black);

        timer = new Timer(Constants.DELAY, new GameCycle());
        timer.start();

        gameInit();
    }

    private void gameInit() {

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLUMNS; j++) {

                final Alien alien = new Alien(Constants.ALIEN_INIT_X + 80 * j,
                                              Constants.ALIEN_INIT_Y + 60 * i);
                aliens.add(alien);
            }
        }

        player = new Player();
        shot = new Shot();

        loadAliensAsPods();

        sounds.playBackGround();
    }

    private synchronized void loadAliensAsPods() {
        for (Alien alien : aliens) {
            alien.setPod(null);
        }

        int count = 0;

        for (K8sPod pod : kubectl.getPods()) {
            if (count < aliens.size()) {
                final Alien alien = aliens.get(count);
                alien.setPod(pod);
                count++;
            }
        }
    }

    private void drawAliens(Graphics g) {

        boolean above = true;

        for (Alien alien : aliens) {
            alien.setUpMode(alienUpMode);

            if (alien.isVisible()) {
                g.drawImage(alien.getImage(), alien.getX(), alien.getY(), this);
                g.setColor(Color.magenta);
                g.setFont(alienFont);

                if (above) {
                    above = false;
                    g.drawString(alien.getName(), alien.getX() - 20, alien.getY() - 5);
                } else {
                    above = true;
                    g.drawString(alien.getName(), alien.getX() - 20, alien.getY() + alien.getImage().getHeight(null) + 10);
                }
            }
        }
    }

    private void drawPlayer(Graphics g) {

        if (player.isVisible()) {
            g.drawImage(player.getImage(), player.getX(), player.getY(), this);
        }

        if (player.isDying()) {
            player.die();
            inGame = false;
        }
    }

    private void drawShot(Graphics g) {
        if (shot.isVisible()) {
            g.drawImage(shot.getImage(), shot.getX(), shot.getY(), this);
        }
    }

    private void drawBombing(Graphics g) {

        for (Alien a : aliens) {
            final Alien.Bomb b = a.getBomb();

            if (!b.isDestroyed()) {
                g.drawImage(b.getImage(), b.getX(), b.getY(), this);
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        final Graphics2D graphics2D = (Graphics2D) g;

        final AffineTransform transform = graphics2D.getTransform();

        transform.setToScale(graphicsScale, graphicsScale);
        graphics2D.setTransform(transform);

        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        g.setColor(Color.black);
        g.fillRect(0, 0, canvasSize.width, canvasSize.height);

        if (inGame) {
            g.setColor(Color.green);

            g.drawLine(0, Constants.GROUND, Constants.BOARD_WIDTH, Constants.GROUND);

            g.setFont(listFont);
            g.setColor(Color.white);

            g.drawString("[SPACE] to fire, [LEFT] and [RIGHT] arrows to move, [UP] scale pods up, [DOWN] scale pods down, [S] sound on/off, [=] cycle scalable",
                         5,
                         Constants.BOARD_HEIGHT - 70);

            g.drawImage(logo, Constants.BOARD_WIDTH - logo.getWidth(null) - 30, Constants.GROUND - 30, null);
            g.drawImage(citiLogo, Constants.BOARD_WIDTH - citiLogo.getWidth(null) - 30, Constants.BOARD_HEIGHT - 2 * citiLogo.getHeight(null), null);

            drawAliens(g);
            drawShot(g);
            drawBombing(g);
            listPods(g);
            drawScale(g);
            drawPlayer(g);

        } else {

            if (timer.isRunning()) {
                timer.stop();
            }

            gameOver(g);
        }

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawScale(Graphics g) {
        final int xPos = Constants.BOARD_WIDTH - 360;

        g.setFont(scaleFont);
        g.setColor(Color.white);
        g.drawString("Scale", xPos, g.getFontMetrics().getHeight());

        final int labelWidth = g.getFontMetrics().stringWidth("scale  ");

        final int x = drawLevelBars(g, xPos + g.getFontMetrics().stringWidth("scale  "), kubectl.getReplicas());

        g.setColor(Color.green);
        g.drawString(String.format("%d/%d", kubectl.getReplicas(), kubectl.getPendingScale()), x, g.getFontMetrics().getHeight());

        if (kubectl.nextStatefulSet() != null) {
            final int yPos = g.getFontMetrics().getHeight() + 15;

            g.setFont(alienFont);
            g.drawString(kubectl.getCurrentStatefulSet(), xPos + labelWidth, yPos);
        }

        drawScore(g);
    }

    private int drawLevelBars(Graphics g, int xPos, int current) {
        final int blockY = 12;

        for (int i = 0; i < 10; i++) {
            g.setColor(Color.gray);

            if (i >= 9 && current > 10) {
                g.setColor(Color.red);
            } else if (i < current) {
                g.setColor(Color.green);
            }

            if (i >= current && i < kubectl.getPendingScale()) {
                g.setColor(Color.yellow);
            }

            final int pending = kubectl.getPendingScale();

            if (i == pending - 1 && current != pending) {
                g.setColor(Color.orange);
            }

            g.fillRect(xPos, blockY, 15, 20);
            xPos += 20;
        }

        return xPos;
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.white);
        g.drawString("Score", 5, g.getFontMetrics().getHeight());
        g.setColor(Color.green);
        g.drawString(String.valueOf(deaths * 10), 80, g.getFontMetrics().getHeight());
    }

    private void listPods(Graphics g) {
        g.setFont(listFont);
        g.setColor(Color.white);

        int y = Constants.GROUND + 2 * player.getHeight();

        for (K8sPod pod : kubectl.getPods()) {
            Color color = Color.white;

            switch (pod.getStatus()) {
                case Alien.STATUS_PENDING:
                    color = Color.orange;
                    break;
                case Alien.STATUS_TERMINATING:
                    color = Color.red;
                    break;
            }
            g.setColor(color);

            final String podInfo = String.format("%-24s %-16s  [%-48s] %10ds",
                                                 pod.getName(), pod.getStatus(), pod.getNode(), pod.getAgeSeconds());

            g.drawString(podInfo, 5, y);

            g.setColor(nodeColors.getColourForNode(pod.getNode()));
            g.fillRect(g.getFontMetrics().stringWidth(podInfo) + 20, y - 10, 10, 10);

            y += g.getFontMetrics().getHeight();

            if (y > Constants.BOARD_HEIGHT - 100) {
                break;
            }
        }
    }

    private void gameOver(Graphics g) {

        g.setColor(Color.black);
        g.fillRect(0, 0, Constants.BOARD_WIDTH, Constants.BOARD_HEIGHT);

        g.setColor(new Color(0, 32, 48));
        g.fillRect(50, Constants.BOARD_WIDTH / 2 - 30, Constants.BOARD_WIDTH - 100, 50);
        g.setColor(Color.white);
        g.drawRect(50, Constants.BOARD_WIDTH / 2 - 30, Constants.BOARD_WIDTH - 100, 50);

        g.setColor(Color.white);
        g.setFont(smallFont);

        g.drawString(message, (Constants.BOARD_WIDTH - g.getFontMetrics().stringWidth(message)) / 2,
                     Constants.BOARD_WIDTH / 2);
    }

    private void update() {
        if (deaths == Constants.NUMBER_OF_ALIENS_TO_DESTROY) {

            inGame = false;
            timer.stop();
            message = "Game won!";
        }

        if (System.currentTimeMillis() - lastUpdate > 500) {
            lastUpdate = System.currentTimeMillis();
            alienUpMode = !alienUpMode;
            for (Alien alien : aliens) {
                alien.act(15 * direction);
            }
        }

        player.act();

        processShot();
        loadAliensAsPods();
        shiftAliens();
        detectInvasion();
        processAlienBombs();
    }

    private void processAlienBombs() {
        for (Alien alien : aliens) {

            final int shot = generator.nextInt(15);

            final Alien.Bomb bomb = alien.getBomb();

            if (shot == Constants.CHANCE && alien.isVisible() && bomb.isDestroyed()) {
                bomb.setDestroyed(false);
                bomb.getRectangle().setLocation(alien.getLocation());
                bomb.incY(alien.getHeight());
            }

            if (player.isVisible() && !bomb.isDestroyed()) {

                if (player.getRectangle().contains(bomb.getLocation())) {
                    player.setImage(explosionImage);
                    player.setDying(true);
                    bomb.setDestroyed(true);
                }
            }

            if (!bomb.isDestroyed()) {
                bomb.incY(1);

                if (bomb.getY() >= Constants.GROUND - 20) {
                    bomb.setImage(Images.getImage("bomb-impact.png"));
                }
                if (bomb.getY() >= Constants.GROUND - bomb.getHeight()) {
                    bomb.setDestroyed(true);
                }
            }
        }
    }

    private void detectInvasion() {
        for (Alien alien : aliens) {
            if (alien.isVisible()) {
                if (alien.getY() > Constants.GROUND - alien.getHeight()) {
                    sounds.stopBackground();
                    inGame = false;
                    message = "Invasion!";
                }
            }
        }
    }

    private void shiftAliens() {
        for (Alien alien : aliens) {
            if (alien.isVisible()) {
                final int x = alien.getX();

                if (x >= Constants.BOARD_WIDTH - Constants.BORDER_RIGHT && direction != -1) {
                    direction = -1;
                    moveDown();
                    break;
                }

                if (x <= Constants.BORDER_LEFT && direction != 1) {
                    direction = 1;
                    moveDown();
                    break;
                }
            }
        }
    }

    private void moveDown() {
        for (Alien alien : aliens) {
            alien.incY(Constants.GO_DOWN);
        }
    }

    private void processShot() {
        if (shot.isVisible()) {

            for (Alien alien : aliens) {

                if (alien.isVisible() && shot.isVisible()) {
                    if (alien.getRectangle().contains(shot.getLocation())) {

                        alien.setImage(explosionImage);
                        alien.setDying(true);

                        if (alien.getPod() != null) {
                            sounds.playExplosion();
                            kubectl.deletePod(alien.getPod());
                        }

                        deaths++;
                        shot.die();
                    }
                }
            }

            int y = shot.getY();
            y -= 4;

            if (y < 0) {
                shot.die();
            } else {
                shot.setY(y);
            }
        }
    }

    private void doGameCycle() {
        update();
        repaint();
    }

    private class GameCycle implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            doGameCycle();
        }
    }

    private class TAdapter extends KeyAdapter {

        @Override
        public void keyReleased(KeyEvent e) {
            player.keyReleased(e);
        }

        @Override
        public void keyPressed(KeyEvent e) {

            player.keyPressed(e);

            final int x = player.getX();
            final int y = player.getY();

            final int key = e.getKeyCode();

            switch (key) {
                case KeyEvent.VK_SPACE:

                    if (inGame) {
                        if (!shot.isVisible()) {
                            shot = new Shot(x, y);

                            sounds.playPhaser();
                        }
                    }
                    break;

                case KeyEvent.VK_UP:
                    kubectl.scaleUp();
                    break;

                case KeyEvent.VK_DOWN:
                    kubectl.scaleDown();
                    break;

                case KeyEvent.VK_S:
                    sounds.toggleMute();
                    break;

                case KeyEvent.VK_EQUALS:
                    kubectl.nextStatefulSet();
                    break;
            }
        }
    }
}
