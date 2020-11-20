package br.com.rubim.runtime;

import br.com.rubim.runtime.core.Metrics;
import br.com.rubim.runtime.dependency.DependencyEvent;
import br.com.rubim.runtime.dependency.DependencyState;
import br.com.rubim.runtime.request.RequestEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorMetrics {

  private static final Logger LOG = LoggerFactory.getLogger(MonitorMetrics.class);
  public static MonitorMetrics INSTANCE = new MonitorMetrics();
  private static final BigDecimal MULTIPLIER_NANO_TO_SECONDS = new BigDecimal(1.0E9D);
  private Map<String, ScheduledExecutorService> schedulesCheckers;

  private MonitorMetrics() {
    schedulesCheckers = new HashMap<>();
  }

  /**
   * Add dependency to be checked successive between the period
   *
   * @param name name of dependency checker
   * @param task task for dependency checker
   * @param time time in unit between successive task executions
   * @param unit unit of time for task executions
   */
  public void addDependencyChecker(String name, Supplier<DependencyState> task, long time,
      TimeUnit unit) {
    if (schedulesCheckers.containsKey(name)) {
      cancelDependencyChecker(name);
    }

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    executor.scheduleWithFixedDelay(() -> {
      if (DependencyState.UP.equals(task.get())) {
        LOG.debug("Checker: {} is UP", name);
        Metrics.dependencyUp(name);
      } else {
        LOG.debug("Checker: {} is DOWN", name);
        Metrics.dependencyDown(name);
      }
    }, time, time, unit);

    schedulesCheckers.put(name, executor);
  }

  /**
   * Cancel all scheduled dependency checkers and terminates the executor timer.
   */
  public void cancelAllDependencyCheckers() {
    var listOfKeys = new HashSet<>(schedulesCheckers.keySet());
    listOfKeys.forEach(this::cancelDependencyChecker);
  }

  /**
   * Cancel the scheduled dependency checker and terminates the executor timer.
   *
   * @param name dependency checker
   */
  public void cancelDependencyChecker(String name) {
    ScheduledExecutorService executor = schedulesCheckers.get(name);
    try {
      LOG.debug("attempt to shutdown executor {}", name);
      executor.shutdown();
      executor.awaitTermination(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOG.debug("tasks interrupted in executor {}", name);
    } finally {
      if (!executor.isTerminated()) {
        LOG.debug("cancel non-finished tasks in executor {}", name);
      }
      executor.shutdownNow();
      LOG.debug("shutdown finished in executor {}", name);
      schedulesCheckers.remove(name);
    }
  }

  /**
   * Add a dependency event to be monitored with elapsed time
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   */
  public void addDependencyEvent(DependencyEvent event, double elapsedSeconds) {
    var labels = new String[]{
        event.getName(),
        event.getType(),
        event.getStatus(),
        event.getMethod(),
        event.getAddress(),
        event.getIsError(),
        event.getErrorMessage()};

    Metrics.dependencyRequestSeconds(labels, elapsedSeconds);
  }

  /**
   * Get all checkers in execution
   *
   * @return Collection of checkers in execution
   */
  public Collection<String> listOfCheckersScheduled() {
    return Collections.unmodifiableSet(schedulesCheckers.keySet());
  }

  /**
   * Add a request event to be monitored with elapsed time
   *
   * @param event properties of event to be monitored
   * @param elapsedSeconds time in seconds to be register in metric
   */
  public void addRequestEvent(RequestEvent event, double elapsedSeconds) {
    var labels = new String[]{
        event.getType(),
        event.getStatus(),
        event.getMethod(),
        event.getAddress(),
        event.getIsError(),
        event.getErrorMessage()};

    Metrics.requestSeconds(labels, elapsedSeconds);
  }

  /**
   * Calculate the elapsed time in seconds
   *
   * @param init initial time
   * @return time in seconds
   */
  public static double calcTimeElapsedInSeconds(Instant init) {
    var finish = Instant.now();
    BigDecimal diff = new BigDecimal(Duration.between(init, finish).toNanos());
    return diff.divide(MULTIPLIER_NANO_TO_SECONDS, 9, RoundingMode.HALF_UP).doubleValue();
  }
}
