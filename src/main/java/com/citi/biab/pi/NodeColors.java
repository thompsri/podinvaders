package com.citi.biab.pi;

import com.google.common.collect.ImmutableMap;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

public class NodeColors {
    private final int DEFAULT_COLOUR = 3;

    private final Color[] rainbow = {Color.red, Color.orange, Color.yellow, Color.green, Color.blue, Color.cyan, Color.magenta};

    private final Map<String, Color> nodeColours = new HashMap<>(
            ImmutableMap.of(
                    "worker01", Color.red,
                    "worker02", Color.orange,
                    "worker03", Color.yellow,
                    "worker04", Color.green,
                    "worker05", Color.blue,
                    "worker06", Color.magenta,
                    "control01", Color.gray
            ));

    public synchronized void addNode(String nodeName) {
        if (!nodeColours.containsKey(nodeName)) {
            int ix = nodeColours.size();

            if (ix >= rainbow.length) {
                ix = DEFAULT_COLOUR;
            }

            nodeColours.put(nodeName, rainbow[ix]);
        }
    }

    public synchronized Color getColourForNode(String nodeName) {
        return nodeColours.getOrDefault(nodeName, rainbow[DEFAULT_COLOUR]);
    }
}
