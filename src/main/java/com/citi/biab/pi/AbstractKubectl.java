package com.citi.biab.pi;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public abstract class AbstractKubectl implements Kubectl {
    private static final Logger log = LoggerFactory.getLogger(AbstractKubectl.class);

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private final Map<String, K8sPod> pods = new ConcurrentHashMap<>();

    protected final String nameSpace;

    protected final List<StatefulSet> statefulSets = new ArrayList<>();

    protected final Map<String, StatefulSet> pendingStatefulSets = new HashMap<>();

    protected int currentStatefulSet = 0;

    protected AbstractKubectl(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    @Override
    public Iterable<? extends K8sPod> getPods() {
        final Set<K8sPod> sortedPods = new TreeSet<>(Comparator.comparing(K8sPod::getName));

        sortedPods.addAll(pods.values());

        return sortedPods;
    }

    @Override
    public Map<String, K8sPod> getPodsByName() {
        return pods;
    }

    @Override
    public void deletePod(K8sPod pod) {
        tasks.add(() -> deletePod(pod.getName()));
    }

    protected abstract void deletePod(String name);

    @Override
    public void scaleDown() {
        final StatefulSet statefulSet = getCurrentStatefulSet();

        if (statefulSet != null) {
            pendingStatefulSets.put(statefulSet.getMetadata().getName(), statefulSet);

            final int scale = statefulSet.getSpec().getReplicas();

            if (scale > 0) {
                statefulSet.getSpec().setReplicas(scale - 1);
            }
        }

        scaleAsync();
    }

    @Override
    public void scaleUp() {
        final StatefulSet statefulSet = getCurrentStatefulSet();

        if (statefulSet != null) {
            pendingStatefulSets.put(statefulSet.getMetadata().getName(), statefulSet);

            final int scale = statefulSet.getSpec().getReplicas();

            statefulSet.getSpec().setReplicas(scale + 1);
        }

        scaleAsync();
    }

    private void scaleAsync() {
        tasks.add(this::scale);
    }

    protected abstract void scale();

    @Override
    public void start() {
        final Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    while (!tasks.isEmpty()) {
                        final Runnable task = tasks.remove();
                        task.run();
                    }

                    final List<Pod> podList = listPods();

                    pods.clear();

                    for (Pod v1Pod : podList) {
                        final K8sPod pod = new K8sPod(v1Pod);
                        pods.put(pod.getName(), pod);
                    }

                    getScale();
                } catch (Exception e) {
                    log.error("Error listing pods", e);
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(3));
    }

    protected abstract void getScale();

    protected abstract List<Pod> listPods() throws Exception;

    @Nullable
    public synchronized StatefulSet getCurrentStatefulSet() {
        if (currentStatefulSet < statefulSets.size()) {
            final StatefulSet statefulSet = statefulSets.get(currentStatefulSet);

            return pendingStatefulSets.getOrDefault(statefulSet.getMetadata().getName(), statefulSet);
        }
        currentStatefulSet = 0;
        return null;
    }

    @Override
    public synchronized void nextStatefulSet() {
        currentStatefulSet++;
        if (currentStatefulSet >= statefulSets.size()) {
            currentStatefulSet = 0;
        }
    }
}
