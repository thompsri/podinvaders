package com.citi.biab.pi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.EventQueue;
import java.io.IOException;

public class PodInvaders extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(PodInvaders.class);

    private static final double SCALE = Double.parseDouble(System.getProperty("graphics.scale", "1"));

    public PodInvaders() {

        final String nameSpace = System.getProperty("k8s.namespace", "default");

        Kubectl kubectl;

        if (Boolean.getBoolean("k8s.useKubectl")) {
            kubectl = new KubectlCmdLine(nameSpace);
        } else {
            try {
                kubectl = new KubectlApi(nameSpace);
            } catch (IOException e) {
                log.error("Error starting K8s api, falling back to 'kubectl' command line", e);
                kubectl = new KubectlCmdLine(nameSpace);
            }
        }

        kubectl.start();

        initUI(kubectl);
    }

    private void initUI(Kubectl kubectl) {
        add(new Board(kubectl, SCALE, Integer.getInteger("downStep", 5)));

        setTitle("Ambrosia - Pod Invaders");

        setSize((int) (Constants.BOARD_WIDTH * SCALE), (int) (Constants.BOARD_HEIGHT * SCALE));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public static void main(String... args) {
        EventQueue.invokeLater(() -> {
            new PodInvaders().setVisible(true);
        });
    }
}
