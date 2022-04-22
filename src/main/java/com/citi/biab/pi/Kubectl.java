package com.citi.biab.pi;

import java.util.Map;

public interface Kubectl {
    Iterable<? extends K8sPod> getPods();

    Iterable<String> getNodes();

    Map<String, K8sPod> getPodsByName();

    int getReplicas();

    int getPendingScale();

    void deletePod(K8sPod pod);

    void start();

    void scaleUp();

    void scaleDown();
}
