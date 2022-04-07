package com.citi.biab.pi;

import javax.sound.sampled.LineUnavailableException;

public class Sounds {

    private final SoundClip background = new SoundClip((Sounds.class.getResource("/background.wav")));
    private final SoundClip phaser = new SoundClip((Sounds.class.getResource("/phaser.wav")));
    private final SoundClip explosion = new SoundClip((Sounds.class.getResource("/explosion.wav")));

    private boolean mute = true;

    public Sounds() throws LineUnavailableException {
    }

    public void playBackGround() {
        if (!mute) {
            background.play();
        }
    }

    public void playPhaser() {
        phaser.play();
    }

    public void playExplosion() {
        explosion.play();
    }

    public void stopBackground() {
        background.stop();
    }

    public void toggleMute() {
        mute = !mute;

        if (mute) {
            stopBackground();
        } else {
            playBackGround();
        }
    }
}
