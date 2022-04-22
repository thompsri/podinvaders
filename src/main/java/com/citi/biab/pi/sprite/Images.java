package com.citi.biab.pi.sprite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Images {
    private static final Logger log = LoggerFactory.getLogger(Images.class);

    private static final Map<String, Image> imageCache = new ConcurrentHashMap<>();

    public static Image getImage(String resource) {

        Image image = imageCache.get(resource);

        if (image == null) {
            final URL url = Images.class.getResource("/" + resource);

            if (url == null) {
                log.error("Image not found: " + resource);
            } else {
                image = new ImageIcon(url).getImage();
                imageCache.put(resource, image);
            }
        }

        return image;
    }
}
