package com.github.museadmin.api_client_throttle_test;

import com.github.museadmin.api_client_throttle.APIClientRequestor;
import com.github.museadmin.api_client_throttle.APIClientThrottle;

import org.junit.Test;

import java.util.stream.IntStream;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.assertTrue;

public class TestAPIClientThrottle {

  private void registerNumberOfRequestors(Integer number, APIClientThrottle throttle) {
    IntStream.range(0, number).forEach(
        nbr -> {
          APIClientRequestor r = new APIClientRequestor();
          r.enQueue();
          throttle.registerRequestor(r);
        }
    );
  }

  @Test
  public void testSetLatency() {
    APIClientThrottle throttle = new APIClientThrottle();
    throttle.setLatency(1000);
    assertTrue(throttle.getLatency() == 1000);
  }

  @Test
  public void testResetLatency() {
    APIClientThrottle throttle = new APIClientThrottle();
    throttle.setLatency(1000);
    throttle.resetLatency(200);
    assertTrue(throttle.getLatency() == 200);
  }

  @Test
  public void testRequestorsAreRegistered() {
    APIClientThrottle throttle = new APIClientThrottle();
    registerNumberOfRequestors(10, throttle);
    assertTrue(throttle.numberOfRequestors() == 10);
  }

  @Test
  public void testQueueIsEmptied() throws InterruptedException {
    APIClientThrottle throttle = new APIClientThrottle();
    registerNumberOfRequestors(10, throttle);
    throttle.setLatency(100);
    throttle.start();
    sleep(1000);
    throttle.stop();
    assertTrue(throttle.numberOfQueueItems() == 0);
  }

  @Test
  public void testWaitWhileInhibited() throws InterruptedException {
    // This test expresses the intended usage for applications.
    // It registers 11 requestors and waits in a loop for the 11th
    // requestor to reach the head of the Q and be dis-inhibited.
    // All eleven requestors are dis-inhibited sequentially, one every 200 ms
    APIClientThrottle throttle = new APIClientThrottle();
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
}
