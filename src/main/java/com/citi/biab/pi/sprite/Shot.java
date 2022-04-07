package com.citi.biab.pi.sprite;

public class Shot extends Sprite {

    public Shot() {

    }

    public Shot(int x, int y) {
        setImage(Images.getImage("shot.png"));

        final int hSPace = 6;
        setX(x + hSPace);

        final int vSPace = 1;
        setY(y + vSPace);
    }
}
