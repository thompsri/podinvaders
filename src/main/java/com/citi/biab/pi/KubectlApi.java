package com.citi.biab.pi;

import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.NodeCondition;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class KubectlApi extends AbstractKubectl {
    private static final Logger log = LoggerFactory.getLogger(KubectlApi.class);

    private final DefaultKubernetesClient client;

    public KubectlApi(String nameSpace) throws IOException {
        super(nameSpace);

        log.info("Using K8s (fabric8) API");

        client = new DefaultKubernetesClient();
    }

    protected void deletePod(String podName) {
        log.info("Deleting pod {}", podName);

        client.pods()
                .inNamespace(nameSpace)
                .withName(podName)
                .delete();
    }

    protected void forceDeletePod(String podName) {
        log.info("Force deleting pod {}", podName);

        client.pods()
                .inNamespace(nameSpace)
                .withName(podName)
                .withGracePeriod(0)
                .delete();
    }

    @Override
    protected synchronized void scale() {
        for (StatefulSet statefulSet : pendingStatefulSets.values()) {
            client.apps()
                    .statefulSets()
                    .inNamespace(nameSpace)
                    .withName(statefulSet.getMetadata().getName())
                    .scale(statefulSet.getSpec().getReplicas());
        }
        pendingStatefulSets.clear();
    }

    @Override
    protected synchronized void getScale() {
        statefulSets.clear();
        statefulSets.addAll(client.apps()
                .statefulSets()
                .inNamespace(nameSpace)
                .list()
                .getItems());
        Collections.sort(statefulSets, Comparator.comparing(o -> o.getMetadata().getName()));
    }

    @Override
    protected List<Pod> listPods() {
        return client.pods()
                .inNamespace(nameSpace)
                .list()
                .getItems();
    }

    @Override
    public Iterable<String> getNodes() {
        return client.nodes()
                .list()
                .getItems()
                .stream()
                .map(node -> node.getMetadata().getName())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getReadyNodes() {
        final Set<String> readyNodes = new HashSet<>();

        for (Node node : client.nodes()
                .list()
                .getItems()) {
            for (NodeCondition condition : node.getStatus().getConditions()) {
                if ("Ready".equals(condition.getType())) {
                    if (condition.getStatus().equals("True")) {
                        readyNodes.add(node.getMetadata().getName());
                    }
                }
            }
        }

        return readyNodes;
    }
}
