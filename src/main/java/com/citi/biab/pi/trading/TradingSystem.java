package com.citi.biab.pi.trading;

import com.citi.biab.pi.Constants;
import com.citi.biab.pi.Kubectl;
import com.citi.biab.pi.KubectlApi;
import com.citi.biab.pi.KubectlCmdLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.EventQueue;
import java.io.IOException;

public class TradingSystem extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(TradingSystem.class);

    private static final double SCALE = Double.parseDouble(System.getProperty("graphics.scale", "1"));

    public TradingSystem() {

        final String nameSpace = System.getProperty("k8s.namespace", "default");
        final String scalableStatefulSet = System.getProperty("k8s.scalable");

        Kubectl kubectl;

        if (Boolean.getBoolean("k8s.useKubectl")) {
            kubectl = new KubectlCmdLine(nameSpace, scalableStatefulSet);
        } else {
            try {
                kubectl = new KubectlApi(nameSpace, scalableStatefulSet);
            } catch (IOException e) {
                log.error("Error starting K8s api, falling back to 'kubectl' command line", e);
                kubectl = new KubectlCmdLine(nameSpace, scalableStatefulSet);
            }
        }

        kubectl.start();

        initUI(kubectl);
    }

    private void initUI(Kubectl kubectl) {
        add(new TradingBoard(kubectl, SCALE));

        setTitle("Bank-in-a-Box Trading System");

        setSize((int) (Constants.BOARD_WIDTH * SCALE), (int) (Constants.BOARD_HEIGHT * SCALE));

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    public static void main(String... args) {
        EventQueue.invokeLater(() -> {
            new TradingSystem().setVisible(true);
        });
    }
}
