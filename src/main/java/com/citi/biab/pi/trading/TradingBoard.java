package com.citi.biab.pi.trading;

import com.citi.biab.pi.Constants;
import com.citi.biab.pi.K8sPod;
import com.citi.biab.pi.Kubectl;
import com.citi.biab.pi.NodeColors;
import com.citi.biab.pi.Sounds;
import com.citi.biab.pi.sprite.Images;
import com.citi.biab.pi.sprite.Venue;
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
import java.util.Map;
import java.util.Random;

public class TradingBoard extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(TradingBoard.class);

    public final Image citiLogo = Images.getImage("citi.png");

    private final Font scaleFont = new Font("Impact", Font.PLAIN, 18);
    private final Font smallFont = new Font("Helvetica", Font.PLAIN, 12);

    private final NodeColors nodeColors = new NodeColors();

    private Sounds sounds;

    private final Dimension canvasSize = new Dimension(Constants.BOARD_WIDTH, Constants.BOARD_HEIGHT);

    private final Random generator = new Random();

    private final Kubectl kubectl;

    private final double graphicsScale;

    private final List<Venue> venues = new ArrayList<>();

    private final int[] layout = new int[]{4, 6, 8, 6};

    private final List<Venue>[] venueGrid = new List[layout.length];

    public TradingBoard(Kubectl kubectl, double graphicsScale) {
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

        final Timer timer = new Timer(15, new GameCycle());
        timer.start();

        gameInit();
    }

    private void gameInit() {
        final Venue sizer = new Venue(0, 0, 0, 0);

        final int xSpace = 20;
        final int ySpace = 40;

        final int cellWidth = sizer.getWidth() + xSpace * 2;
        final int cellHeight = sizer.getHeight() + ySpace * 2;

        final int xMargin = 20;
        final int yMargin = 50;

        final int xCells = (canvasSize.width - 2 * xMargin) / cellWidth;

        for (int i = 0; i < layout.length; i++) {
            venueGrid[i] = new ArrayList<>();
        }
        for (int i = 0; i < layout.length; i++) {
            final int startCell = (xCells - layout[i]) / 2;

            for (int j = 0; j < layout[i]; j++) {

                final Venue venue = new Venue(xMargin + (startCell + j) * cellWidth,
                                              yMargin + cellHeight * i, i, j);
                venues.add(venue);

                venueGrid[i].add(venue);
            }
        }

        loadAliensAsPods();

        sounds.playBackGround();
    }

    private synchronized void loadAliensAsPods() {
        final Map<String, K8sPod> pods = kubectl.getPodsByName();

        for (Venue venue : venues) {
            venue.setPod(pods.get(venue.getName()));
        }
    }

    private void drawVenues(Graphics g) {

        for (Venue venue : venues) {
            final int cx = venue.getX() + venue.getWidth();
            final int cy = venue.getY() + venue.getImage().getHeight(null) / 2 + 20;

            g.setColor(Color.black);

            g.setFont(scaleFont);
            g.fillRect(cx, cy - g.getFontMetrics().getHeight(), 40, g.getFontMetrics().getHeight());
            g.setColor(Color.orange);

            g.drawString(String.valueOf(venue.getOrderCount()), cx, cy);
            g.drawImage(venue.getImage(), venue.getX(), venue.getY(), this);
            g.setFont(smallFont);

            final int tx = venue.getX() + (venue.getWidth() - g.getFontMetrics().stringWidth(venue.getName())) / 2;

            g.drawString(venue.getName(), tx, venue.getY() + venue.getHeight() + 10);

            final K8sPod pod = venue.getPod();

            if (pod != null) {
                g.setColor(nodeColors.getColourForNode(pod.getNode()));
                g.fillRect(venue.getX() + venue.getWidth() + 2, venue.getY() + venue.getHeight() - 20, 5, 5);
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

        g.drawImage(citiLogo, Constants.BOARD_WIDTH - citiLogo.getWidth(null) - 30, Constants.BOARD_HEIGHT - 2 * citiLogo.getHeight(null), null);

        drawVenues(g);
        drawBombing(g);

        Toolkit.getDefaultToolkit().sync();
    }

    private void drawBombing(Graphics g) {

        g.setColor(Color.green);

        for (Venue a : venues) {
            final Venue.Order b = a.getOrder();

            if (b.isActive()) {
                g.drawLine(b.getX() + b.getHalfWidth(),
                           b.getY() + b.getHalfWidth(),
                           a.getX() + a.getHalfWidth(),
                           a.getY() + a.getHeight() + 15);
            }
        }
    }

    private void update() {
        loadAliensAsPods();
        processOrders();
    }

    private void processOrders() {
        for (int row = 0; row < layout.length - 1; row++) {
            for (int col = 0; col < layout[row]; col++) {

                final int shot = generator.nextInt(15);
                final Venue venue = venueGrid[row].get(col);

                final Venue.Order order = venue.getOrder();

                if (shot == Constants.CHANCE && venue.isVisible() && !venue.isDisabled() && !order.isActive()) {
                    order.setActive(true);
                    order.getRectangle().setLocation(venue.getLocation());
                    order.incX(venue.getWidth() / 2 - order.getWidth() / 2);
                    order.incY(venue.getHeight() + 25);

                    final Venue dest = getDestination(row);

                    if (dest != null) {
                        order.setDestination(dest);
                    } else {
                        order.setActive(false);
                    }

                    if (row == 0) {
                        venue.incOrderCount();
                    }
                }

                final Venue dest = order.getDestination();

                if (order.isActive() && dest != null) {
                    if (dest.getRectangle().contains(order.getLocation())) {
                        dest.incOrderCount();
                        order.setActive(false);
                    } else {
                        order.moveToDest();
                    }
                }
            }
        }
    }

    private Venue getDestination(int row) {
        final int nextRow = row + 1;

        if (nextRow < venueGrid.length) {
            final List<Venue> nextVenues = venueGrid[nextRow];

            final List<Venue> enabled = new ArrayList<>();

            for (Venue nextVenue : nextVenues) {
                if (!nextVenue.isDisabled()) {
                    enabled.add(nextVenue);
                }
            }

            if (enabled.isEmpty()) {
                return null;
            }

            final int rand = generator.nextInt(enabled.size());

            return enabled.get(rand);
        }
        return null;
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
        }

        @Override
        public void keyPressed(KeyEvent e) {
        }
    }
}
