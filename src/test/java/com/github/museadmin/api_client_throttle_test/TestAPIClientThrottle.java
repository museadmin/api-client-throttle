package com.github.museadmin.api_client_throttle_test;

import com.github.museadmin.api_client_throttle.APIClientRequestor;
import com.github.museadmin.api_client_throttle.APIClientThrottle;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.*;

public class TestAPIClientThrottle {

  private APIClientThrottle throttle;
  private void registerNumberOfRequestors(Integer number, APIClientThrottle throttle) {
    IntStream.range(0, number).forEach(
        nbr -> {
          APIClientRequestor r = new APIClientRequestor();
          r.enQueue();
          throttle.registerRequestor(r);
        }
    );
  }

  @Before
  public void setUp() {
    throttle = new APIClientThrottle();
  }

  @Test
  public void testIncrementLatencyRestingThrottle() {
    throttle.setLatency(1000);
    throttle.setLatencyOffset(100);
    throttle.incrementLatency();
    assertEquals(1100, (int) throttle.getLatency());
    assertEquals(100, (int) throttle.getLatencyOffset());
  }

  @Test
  public void testDecrementLatencyRunningThrottle() throws InterruptedException {
    throttle.setLatency(500);
    throttle.setLatencyOffset(300);

    registerNumberOfRequestors(5, throttle);
    APIClientRequestor r = new APIClientRequestor();
    // Register the 6th requestor with the throttle
    throttle.registerRequestor(r);

    // Can now request to be added to the throttle's Q asynchronously
    r.enQueue();

    // Capture start time
    Long startS = Instant.now().getEpochSecond();

    // Now loop every <latency> ms maintaining the queue
    throttle.start();

    // Test would complete in 3 second but adding decrement should reduce that
    throttle.decrementLatency();

    // Wait until 6th requestor reaches head of Q and is dis-inhibited
    while (r.inhibited) {
      sleep(10);
    }

    throttle.stop();

    // Capture finish time
    Long endS = Instant.now().getEpochSecond();

    long testDuration = endS - startS;

    assertTrue(testDuration < 3);
    assertEquals(200, (int) throttle.getLatency());
  }

  @Test
  public void testIncrementLatencyRunningThrottle() throws InterruptedException {
    throttle.setLatency(100);
    throttle.setLatencyOffset(300);

    registerNumberOfRequestors(5, throttle);
    APIClientRequestor r = new APIClientRequestor();
    // Register the 6th requestor with the throttle
    throttle.registerRequestor(r);

    // Can now request to be added to the throttle's Q asynchronously
    r.enQueue();

    // Capture start time
    Long startS = Instant.now().getEpochSecond();

    // Now loop every <latency> ms maintaining the queue
    throttle.start();

    // Test would complete in 1 second but adding increment should extend that
    throttle.incrementLatency();

    // Wait until 6th requestor reaches head of Q and is dis-inhibited
    while (r.inhibited) {
      sleep(10);
    }

    throttle.stop();

    // Capture finish time
    Long endS = Instant.now().getEpochSecond();

    long testDuration = endS - startS;

    assertTrue(testDuration > 1);
    assertEquals(400, (int) throttle.getLatency());
  }

  @Test
  public void testSetLatency() {
    throttle.setLatency(1000);
    assertEquals(1000, (int) throttle.getLatency());
  }

  @Test
  public void testResetLatency() {
    throttle.setLatency(1000);
    throttle.resetLatency(200);
    assertEquals(200, (int) throttle.getLatency());
  }

  @Test
  public void testRequestorsAreRegistered() {
    registerNumberOfRequestors(10, throttle);
    assertEquals(10, (int) throttle.numberOfRequestors());
  }

  @Test
  public void testQueueIsEmptied() throws InterruptedException {
    registerNumberOfRequestors(10, throttle);
    throttle.setLatency(100);
    throttle.start();
    sleep(1000);
    throttle.stop();
    assertEquals(0, (int) throttle.numberOfQueueItems());
  }

  @Test
  public void testWaitWhileInhibited() throws InterruptedException {
    // This test expresses the intended usage for applications.
    // It registers 11 requestors and waits in a loop for the 11th
    // requestor to reach the head of the Q and be dis-inhibited.
    // All eleven requestors are dis-inhibited sequentially, one every 200 ms
    throttle.setLatency(200);

    registerNumberOfRequestors(10, throttle);
    APIClientRequestor r = new APIClientRequestor();
    // Register the 11th requestor with the throttle
    throttle.registerRequestor(r);

    // Can now request to be added to the throttle's Q asynchronously
    r.enQueue();

    // Now loop every <latency> ms maintaining the queue
    throttle.start();
    int loops = 0;
    // Wait until 11th requestor reaches head of Q and is dis-inhibited
    while (r.inhibited) {
      loops++;
      sleep(10);
    }
    throttle.stop();

    // Confirm we looped while waiting for Q to be processed
    assertTrue(loops > 150);
  }

  @Test
  public void testThreadsWaitWhileInhibited() throws InterruptedException {

    throttle.setLatency(200);

    // Add some bulk to the throttle load
    registerNumberOfRequestors(5, throttle);

    // One requestor for each thread
    APIClientRequestor r1 = new APIClientRequestor();
    APIClientRequestor r2 = new APIClientRequestor();
    throttle.registerRequestor(r1);
    throttle.registerRequestor(r2);
    r1.enQueue();
    r2.enQueue();

    // Two runnable services to spawn the threads
    TestSvc ts1 = new TestSvc(r1, "Thread1");
    TestSvc ts2 = new TestSvc(r2, "Thread2");
    ts1.start();
    ts2.start();

    // Start processing the request queue
    throttle.start();
    sleep(2000);
    throttle.stop();

    // Confirm all references were processed and dis-inhibited
    assertFalse(ts1.requestor.inhibited);
    assertFalse(ts2.requestor.inhibited);
    assertFalse(r1.inhibited);
    assertFalse(r2.inhibited);
  }

  /**
   * Test class mimics a service that is waiting for
   * its turn to hit the head of the queue. This can be
   * used as an example of how to implement a throttled
   * service.
   */
  class TestSvc implements Runnable {

    private Thread t;
    private String threadName;

    // Points to the same instance of the requestor in the throttle
    private APIClientRequestor requestor;

    TestSvc(APIClientRequestor requestor, String name) {
      this.requestor = requestor;
      this.threadName = name;
    }

    public void run() {

      // Requestor is dis-inhibited after reaching the head of the queue.
      while (requestor.inhibited) {
        try {
          sleep(10);
        } catch (InterruptedException e) {
          System.out.println("Thread " +  threadName + " interrupted.");
          break;
        }
      }
      // This is where a real client would send out the API request
      // after waiting for its place at the head of the queue.
    }

    void start() {
      if (t == null) {
        t = new Thread (this, threadName);
        t.start ();
      }
    }
  }

}
