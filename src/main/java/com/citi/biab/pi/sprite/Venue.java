package com.citi.biab.pi.sprite;

import com.citi.biab.pi.K8sPod;

import javax.annotation.Nullable;
import java.awt.Image;
import java.awt.Point;

import static com.citi.biab.pi.sprite.Alien.STATUS_RUNNING;

public class Venue extends Sprite {

    enum Type {
        CLIENT_GATEWAY("broker-gateway", "client"),
        TRADING_ENGINE("trading-engine", "trading-engine"),
        RISK_ENGINE("risk-controller", "risk"),
        EXCHANGE("exchange-gateway", "exchange");

        private final String name;
        private final Image image;
        private final Image disabledImage;

        Type(String name, String imageName) {
            this.name = name;
            this.image = Images.getImage(imageName + ".png");
            this.disabledImage = Images.getImage(imageName + "-dis.png");
        }

        public Image getImage() {
            return image;
        }

        public String getName() {
            return name;
        }

        public Image getDisabledImage() {
            return disabledImage;
        }
    }

    private final Type type;

    private final Order order;

    private final int ordinal;

    @Nullable
    private K8sPod pod;

    private int orderCount;

    private int pendingCount = 0;

    public Venue(int x, int y, int type, int ordinal) {
        this.type = Type.values()[type];
        this.ordinal = ordinal;

        rectangle.setLocation(x, y);
        rectangle.setSize(this.type.getImage().getWidth(null), this.type.getImage().getHeight(null));

        order = new Order(x + 300, y + rectangle.height + 15);
    }

    @Override
    public Image getImage() {
        return isDisabled() ?
               type.getDisabledImage() : type.getImage();
    }

    public Order getOrder() {
        return order;
    }

    @Nullable
    public K8sPod getPod() {
        return pod;
    }

    public void setPod(@Nullable K8sPod pod) {
        this.pod = pod;

        if (pod == null) {
            orderCount = 0;
            pendingCount = 0;
        }
    }

    public String getName() {
        return type.getName() + "-" + ordinal;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public boolean hasPending() {
        return pendingCount > 0;
    }

    public void decPending() {
        pendingCount--;

        if (pendingCount < 0) {
            pendingCount = 0;
        }
    }

    public void incOrderCount() {
        this.orderCount++;

        pendingCount++;
    }

    public boolean isDisabled() {
        return pod == null || !STATUS_RUNNING.equals(pod.getStatus());
    }

    public static class Order extends Sprite {
        private boolean active;

        public Venue getDestination() {
            return destination;
        }

        private Venue destination;
        private Point destPoint;

        public Order(int x, int y) {
            setActive(false);

            rectangle.setLocation(x, y);
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }

        public void setDestination(Venue destination) {
            this.destination = destination;

            destPoint = new Point((int) destination.rectangle.getCenterX(),
                                  (int) destination.rectangle.getCenterY());
        }

        public void moveToDest() {
            if (destPoint != null) {
                final int dx = destPoint.x - rectangle.x;
                final int dy = destPoint.y - rectangle.y;

                incY(2);
                incX(2 * dx / dy);
            }
        }
    }
}
