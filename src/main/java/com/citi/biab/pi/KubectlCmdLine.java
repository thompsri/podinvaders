package com.citi.biab.pi;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import io.fabric8.kubernetes.api.model.BaseKubernetesList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class KubectlCmdLine extends AbstractKubectl {
    private static final Logger log = LoggerFactory.getLogger(KubectlCmdLine.class);

    private static final String KUBECTL = "kubectl";

    public KubectlCmdLine(String nameSpace, String scalable) {
        super(nameSpace, scalable);

        log.info("Using '{}' command line tool", KUBECTL);
    }

    private String runKubectl(String... args) {
        final List<String> kubeCtl = new ArrayList<>();

        kubeCtl.add(KUBECTL);

        Collections.addAll(kubeCtl, args);

        kubeCtl.add("--namespace");
        kubeCtl.add(nameSpace);

        try {
            final Process process = new ProcessBuilder(kubeCtl).redirectErrorStream(true).start();

            final String output = new String(ByteStreams.toByteArray(process.getInputStream()));

            process.waitFor();

            return output;

        } catch (Exception e) {
            log.error("Error running {}", kubeCtl, e);
            return e.getMessage();
        }
    }

    protected List<Pod> listPods() {
        final String yaml = runKubectl("get", "all", "-o", "yaml");

        if (Strings.isNullOrEmpty(yaml)) {
            return Collections.emptyList();
        }

        final List<Pod> pods = new ArrayList<>();

        final BaseKubernetesList list = Serialization.unmarshal(yaml);

        for (HasMetadata item : list.getItems()) {
            if (item instanceof Pod) {
                pods.add((Pod) item);
            }

            if (item instanceof StatefulSet) {
                final StatefulSet statefulSet = (StatefulSet) item;

                replicas = statefulSet.getStatus().getReplicas();
                pendingScale.set(statefulSet.getSpec().getReplicas());
            }
        }
        return pods;
    }

    @Override
    public Iterable<String> getNodes() {
        final String yaml = runKubectl("get", "nodes", "-o", "yaml");

        if (Strings.isNullOrEmpty(yaml)) {
            return Collections.emptyList();
        }
        final BaseKubernetesList list = Serialization.unmarshal(yaml);

        return list.getItems()
                   .stream()
                   .map(hasMetadata -> hasMetadata.getMetadata().getName())
                   .sorted()
                   .collect(Collectors.toList());
    }

    protected void scale(int scale) {
        runKubectl("scale", "statefulset/" + scalableName, "--replicas=" + scale);
    }

    @Override
    protected void getScale() {
        // no-op
    }

    protected void deletePod(String podName) {
        log.info("Deleting pod {}", podName);

        runKubectl("delete", "pod/" + podName);
    }
}
