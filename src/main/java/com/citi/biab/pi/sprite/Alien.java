package com.citi.biab.pi.sprite;

import com.citi.biab.pi.K8sPod;

import javax.annotation.Nullable;
import java.awt.Image;
import java.util.concurrent.TimeUnit;

public class Alien extends Sprite {

    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_TERMINATING = "Terminating";

    private final Image downImage = Images.getImage("alien-down.png");
    private final Image upImage = Images.getImage("alien-up.png");
    private final Image explosionImage = Images.getImage("explosion.png");
    private final Image pendingImage = Images.getImage("alien-pending.png");

    private final Bomb bomb;

    @Nullable
    private K8sPod pod;

    private long deathTime;

    private boolean alienUpMode;

    private int value;

    public Alien(int x, int y) {
        rectangle.setLocation(x, y);
        rectangle.setSize(downImage.getWidth(null), downImage.getHeight(null));

        bomb = new Bomb(x, y + rectangle.height);
    }

    @Override
    public Image getImage() {
        if (recentlyDied()) {
            return explosionImage;
        }
        if (isDisabled()) {
            return pendingImage;
        }
        return alienUpMode ? upImage : downImage;
    }

    public boolean isDisabled() {
        return pod == null || !STATUS_RUNNING.equals(pod.getStatus());
    }

    @Override
    public void setDying(boolean dying) {
        super.setDying(dying);

        deathTime = System.currentTimeMillis();
    }

    public boolean recentlyDied() {
        return (System.currentTimeMillis() - deathTime < TimeUnit.SECONDS.toMillis(5));
    }

    public void act(int direction) {
        incX(direction);
    }

    public Bomb getBomb() {
        return bomb;
    }

    @Override
    public boolean isVisible() {
        if (pod == null) {
            return false;
        }
        return super.isVisible();
    }

    @Nullable
    public K8sPod getPod() {
        return pod;
    }

    public void setPod(@Nullable K8sPod pod) {
        this.pod = pod;

        if (pod != null) {
            final String name = pod.getName();

            if (name.startsWith("client")) {
                value = 100;
            }
            else if (name.startsWith("trading")) {
                value = 200;
            }
            else if (name.startsWith("risk")) {
                value = 500;
            }
            else if (name.startsWith("exchange")) {
                value = 1000;
            }
        }
    }

    public String getName() {
        return pod == null
               ? ""
               : pod.getName();
    }

    public void setUpMode(boolean alienUpMode) {
        this.alienUpMode = alienUpMode;
    }

    public int getValue() {
        return value;
    }

    public static class Bomb extends Sprite {
        private final Image bomb = Images.getImage("bomb.png");

        private boolean destroyed;

        public Bomb(int x, int y) {
            setDestroyed(true);

            rectangle.setLocation(x, y);
        }

        public void setDestroyed(boolean destroyed) {
            this.destroyed = destroyed;
            setImage(bomb);
        }

        public boolean isDestroyed() {
            return destroyed;
        }
    }
}
