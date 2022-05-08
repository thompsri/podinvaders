package com.citi.biab.pi;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import java.util.Map;

public interface Kubectl {
    Iterable<? extends K8sPod> getPods();

    Iterable<String> getNodes();

    Map<String, K8sPod> getPodsByName();

    void deletePod(K8sPod pod);

    void forceDeletePod(K8sPod pod);

    void start();

    void scaleUp();

    void scaleDown();

    void nextStatefulSet();

    StatefulSet getCurrentStatefulSet();
}
