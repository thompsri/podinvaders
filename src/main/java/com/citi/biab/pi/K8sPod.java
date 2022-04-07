package com.citi.biab.pi;

import io.fabric8.kubernetes.api.model.Pod;

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.TimeUnit;

public class K8sPod {
    private final Pod pod;

    public K8sPod(Pod pod) {
        this.pod = pod;
    }

    public String getStatus() {
        if (pod.getMetadata().getDeletionTimestamp() != null) {
            return "Terminating";
        }
        return pod.getStatus().getPhase();
    }

    public String getName() {
        return pod.getMetadata().getName();
    }

    public long getAgeSeconds() {
        final TemporalAccessor time = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(pod.getMetadata().getCreationTimestamp());
        return TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - time.getLong(ChronoField.INSTANT_SECONDS);
    }

    public String getNode() {
        return pod.getSpec().getNodeName();
    }

    @Override
    public String toString() {
        return pod.getMetadata().getName() + "@" + pod.getSpec().getNodeName();
    }
}
