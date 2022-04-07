package com.citi.biab.pi;

import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
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
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractKubectl implements Kubectl {
    private static final Logger log = LoggerFactory.getLogger(AbstractKubectl.class);

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private final Map<String, K8sPod> pods = new ConcurrentHashMap<>();

    protected final String nameSpace;
    protected final String scalableName;

    protected final AtomicInteger pendingScale = new AtomicInteger();

    protected Integer replicas = 0;

    protected AbstractKubectl(String nameSpace, String scalable) {
        this.nameSpace = nameSpace;
        this.scalableName = scalable;
    }

    @Override
    public Iterable<? extends K8sPod> getPods() {
        final Set<K8sPod> sortedPods = new TreeSet<>(Comparator.comparing(K8sPod::getName));

        sortedPods.addAll(pods.values());

        return sortedPods;
    }

    @Override
    public int getReplicas() {
        return replicas;
    }

    @Override
    public int getPendingScale() {
        return pendingScale.get();
    }

    @Override
    public void deletePod(K8sPod pod) {
        tasks.add(() -> deletePod(pod.getName()));
    }

    protected abstract void deletePod(String name);

    @Override
    public void scaleDown() {
        pendingScale.getAndDecrement();

        if (pendingScale.get() < 0) {
            pendingScale.set(0);
        }

        scaleAsync();
    }

    @Override
    public void scaleUp() {
        pendingScale.getAndIncrement();
        scaleAsync();
    }

    private void scaleAsync() {
        tasks.add(() -> scale(pendingScale.get()));
    }

    protected abstract void scale(int scale);

    @Override
    public void start() {
        final Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    while (!tasks.isEmpty()) {
                        Runnable task = tasks.remove();
                        task.run();
                    }

                    final List<Pod> podList = listPods();

                    pods.clear();

                    for (Pod v1Pod : podList) {
                        K8sPod pod = new K8sPod(v1Pod);
                        pods.put(pod.getName(), pod);
                    }

                    getScale();
                } catch (Exception e) {
                    log.error("Error listing pods", e);
                }
            }
        }, 0, TimeUnit.SECONDS.toMillis(5));
    }

    protected abstract void getScale();

    protected abstract List<Pod> listPods() throws Exception;
}
