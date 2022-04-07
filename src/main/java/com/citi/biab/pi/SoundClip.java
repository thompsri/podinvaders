package com.citi.biab.pi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import java.net.URL;

public class SoundClip {

    private static final Logger log = LoggerFactory.getLogger(SoundClip.class);

    private final Clip clip;
    private final URL resource;

    public SoundClip(URL resource) throws LineUnavailableException {
        this.resource = resource;

        clip = AudioSystem.getClip();

        clip.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.STOP)) {
                final Line soundClip = event.getLine();
                soundClip.close();
            }
        });
    }

    public void play() {
        try {
            if (!clip.isRunning()) {
                clip.open(AudioSystem.getAudioInputStream(resource));
                clip.start();
            }
        } catch (Throwable e) {
            log.error("Error playing sound from '{}'", resource);
        }
    }

    public void stop() {
        clip.stop();
    }
}
