package com.github.museadmin.api_client_throttle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * The core throttle for the API clients
 * Sets the rate based on original config and then
 * calculated on number of throttle reports it receives
 * from the clients.
 *
 * Effectively this object adds latency to the outgoing API
 * request but at the application level, rather than network level
 * like the Network Link Conditioner for MAC does.
 *
 * Throttle should sleep or loop for period of latency
 */
public class APIClientThrottle {

  // ================= variables =================

  // The latency
  public Integer getLatency() {
    return latency;
  }
  public void setLatency(Integer latency) {
    this.latency = latency;
  }
  private Integer latency;

  // Latency increment
  public Integer getLatencyOffset() {
    return latencyOffset;
  }
  public void setLatencyOffset(Integer latencyOffset) {
    this.latencyOffset = latencyOffset;
  }
  private Integer latencyOffset = 0;

  // Requestor ArrayList and Queue for indexes
  private ArrayList<APIClientRequestor> requestors = new ArrayList<>();
  private Queue<Integer> requestQueue = new LinkedList<>();

  // Scheduler
  private final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(1);
  private ScheduledFuture<?> throttleHandle = null;

  // ================= methods =================

  /**
   * Decrement the latency used by latencyOffset and
   * return the value. This will be critical for anyone
   * implementing a self adjusting API rate that reacts to
   * throttling.
   * @return Integer the incremented latency
   */
  public Integer decrementLatency() {

    Integer i = latency - latencyOffset;

    if (this.throttleHandle != null) {
      stop();
      latency = i;
      start();
    } else {
      latency = i;
    }
    return i;
  }

  /**
   * Increment the latency used by latencyOffset and
   * return the value. This will be critical for anyone
   * implementing a self adjusting API rate that reacts to
   * throttling.
   * @return Integer the incremented latency
   */
  public Integer incrementLatency() {

    Integer i = latency + latencyOffset;

    if (this.throttleHandle != null) {
      stop();
      latency = i;
      start();
    } else {
      latency = i;
    }
    return i;
  }

  /**
   * Register an API Requestor. e.g. Add it to the internal array
   * of requestors so that it can be enqueued and dequeued
   * @param requestor APIClientRequestor
   */
  public void registerRequestor(APIClientRequestor requestor) {
    this.requestors.add(requestor);
  }

  /**
   * The latency can be reset to another value
   * or 0 to switch off. Uses the throttleHandle to cancel
   * the current scheduler and then reset the schedule
   *
   * @param latency Integer representing time in milliseconds
   */
  public void resetLatency(Integer latency) {
    this.stop();
    this.latency = latency;
    this.start();
  }

  /**
   * Start the throttle
   */
  public void start(){
    if (this.latency > 0) {
      throttleHandle = scheduler.scheduleAtFixedRate(throttle, 0, this.latency, MILLISECONDS);
    }
  }

  /**
   * Stop the throttle
   */
  public void stop() {
    if (this.throttleHandle != null) {
      throttleHandle.cancel(true);
      throttleHandle = null;
    }
  }

  /**
   * The main timed loop that expresses the latency. Iterates
   * over the Requestors array list. First looking for any that
   * are raising a new request to join the queue. Then dis-inhibiting
   * the requestor at the head of the queue.
   */
  final Runnable throttle = () -> {
    // If we have no requestors then just skip and wait for
    // developer to figure it out. Throwing an exception from
    // inside a Lambda is too messy.
    if (this.requestors.size() > 0) {
      // Look for any new requests to join the queue
      APIClientRequestor r;
      for (Integer i = 0; i < requestors.size(); i++) {
        r = this.requestors.get(i);
        if (r.joinQueue) {
          this.requestQueue.add(i);
          r.acknowledgeRequest();
        }
      }

      // If anything in queue then pop off the head of
      // the Q and dis-inhibit it.
      if (requestQueue.size() > 0) {
        Integer index = requestQueue.remove();
        if (index != null) {
          requestors.get(index).deQueue();
        }
      }
    }
  };

  // ================= test methods =================
  public Integer numberOfRequestors() {
    return this.requestors.size();
  }
  public Integer numberOfQueueItems() {
    return this.requestQueue.size();
  }
}
