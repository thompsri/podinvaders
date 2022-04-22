package com.citi.biab.pi;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class KubectlApi extends AbstractKubectl {
    private static final Logger log = LoggerFactory.getLogger(KubectlApi.class);

    private final DefaultKubernetesClient client;

    public KubectlApi(String nameSpace, String scalable) throws IOException {
        super(nameSpace, scalable);

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

    @Override
    protected void scale(int scale) {
        client.apps()
              .statefulSets()
              .inNamespace(nameSpace)
              .withName(scalableName)
              .scale(scale);
    }

    @Override
    protected void getScale() {
        if (scalableName != null) {
            final StatefulSet statefulSet = client.apps()
                    .statefulSets()
                    .inNamespace(nameSpace)
                    .withName(scalableName)
                    .get();

            if (statefulSet != null) {
                replicas = statefulSet.getStatus().getReplicas();
                pendingScale.set(statefulSet.getSpec().getReplicas());
            }
        }
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
}
