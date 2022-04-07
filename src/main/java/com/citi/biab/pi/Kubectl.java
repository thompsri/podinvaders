package com.citi.biab.pi;

public interface Kubectl {
    Iterable<? extends K8sPod> getPods();

    Iterable<String> getNodes();

    int getReplicas();

    int getPendingScale();

    void deletePod(K8sPod pod);

    void start();

    void scaleUp();

    void scaleDown();
}
