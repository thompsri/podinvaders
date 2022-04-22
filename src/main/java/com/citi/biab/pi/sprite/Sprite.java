package com.citi.biab.pi.sprite;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

public class Sprite {

    private Image image;

    private boolean visible;
    private boolean dying;

    protected Rectangle rectangle = new Rectangle();

    public Sprite() {
        visible = true;
    }

    public void die() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setImage(Image image) {
        this.image = image;

        rectangle.setSize(image.getWidth(null), image.getHeight(null));
    }

    public Image getImage() {
        return image;
    }

    public void setX(int x) {
        this.rectangle.x = x;
    }

    public void setY(int y) {
        this.rectangle.y = y;
    }

    public int getY() {
        return rectangle.y;
    }

    public int getX() {
        return rectangle.x;
    }

    public void setDying(boolean dying) {
        this.dying = dying;
    }

    public boolean isDying() {
        return this.dying;
    }

    public Rectangle getRectangle() {
        return rectangle;
    }

    public Point getLocation() {
        return rectangle.getLocation();
    }

    public int getWidth() {
        return rectangle.width;
    }

    public int getHalfWidth() {
        return rectangle.width / 2;
    }

    public int getHeight() {
        return rectangle.height;
    }

    public void incX(int dx) {
        rectangle.x += dx;
    }

    public void incY(int dy) {
        rectangle.y += dy;
    }
}