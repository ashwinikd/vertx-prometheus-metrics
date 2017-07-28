package io.vertx.ext.prometheus.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.vertx.core.spi.metrics.PoolMetrics;
import io.vertx.ext.prometheus.metrics.counters.Stopwatch;
import org.jetbrains.annotations.NotNull;

public final class PoolPrometheusMetrics extends PrometheusMetrics implements PoolMetrics<Stopwatch> {
  private static final @NotNull Gauge states = Gauge.build("vertx_pool_tasks", "Pool queue metrics")
      .labelNames("type", "name", "state").create();
  private static final @NotNull Counter time = Counter.build("vertx_pool_time", "Pool time metrics (us)")
      .labelNames("type", "name", "state").create();

  private final @NotNull String name;
  private final @NotNull String type;

  public PoolPrometheusMetrics(@NotNull CollectorRegistry registry, @NotNull String type, @NotNull String name, int maxSize) {
    super(registry);
    this.type = type;
    this.name = name;
    register(states);
    register(time);
    states("max_size").set(maxSize);
  }

  @Override
  public @NotNull Stopwatch submitted() {
    states("queued").inc();
    return new Stopwatch();
  }

  @Override
  public void rejected(@NotNull Stopwatch submittedStopwatch) {
    states("queued").dec();
  }

  @Override
  public @NotNull Stopwatch begin(@NotNull Stopwatch submittedStopwatch) {
    states("queued").dec();
    states("used").inc();
    time("delay").inc(submittedStopwatch.stop());
    return submittedStopwatch;
  }

  @Override
  public void end(@NotNull Stopwatch beginStopwatch, boolean succeeded) {
    time("process").inc(beginStopwatch.stop());
    states("used").dec();
  }

  private @NotNull Counter.Child time(@NotNull String state) {
    return time.labels(type, name, state);
  }

  private @NotNull Gauge.Child states(@NotNull String state) {
    return states.labels(type, name, state);
  }
}
