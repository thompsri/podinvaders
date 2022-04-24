package com.citi.biab.pi.sprite;

import com.citi.biab.pi.Constants;

import java.awt.event.KeyEvent;

public class Player extends Sprite {
    private static final int DX = 2;

    int dx;

    public Player() {

        setImage(Images.getImage("player.png"));

        final int START_X = (Constants.BOARD_WIDTH - getWidth()) / 2;
        setX(START_X);

        final int START_Y = Constants.GROUND + 10;
        setY(START_Y);
    }

    public void act() {
        incX(dx);
        rectangle.x = Math.max(rectangle.x, DX);
        rectangle.x = Math.min(rectangle.x, Constants.BOARD_WIDTH - 2 * getWidth());
    }

    public void keyPressed(KeyEvent e) {
        final int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_LEFT:
                dx = -4;
                break;

            case KeyEvent.VK_RIGHT:
                dx = 4;
                break;
        }
    }

    public void keyReleased(KeyEvent e) {
        final int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            dx = 0;
        }
    }
}
