package com.github.museadmin.api_client_throttle_test;

import com.github.museadmin.api_client_throttle.APIClientRequestor;
import com.github.museadmin.api_client_throttle.APIClientThrottle;

import org.junit.Test;

import java.util.stream.IntStream;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static sun.security.krb5.Confounder.longValue;

public class TestAPIClientThrottle {

  private void registerNumberOfRequestors(Integer number, APIClientThrottle throttle) {
    IntStream.range(0, 10).forEach(
        nbr -> {
          APIClientRequestor r = new APIClientRequestor();
          r.enQueue();
          throttle.addRequestor(r);
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

  /**
   * 1 - test throws runtime exception with empty q on start
   * 2 - reset changes latency
   * 3 - q is emptied after registering n requestors
   * 4 - Add single requestor and assert we can wait for inhibited == false
   *     sleep(2000);
   *
   *     throttle.stop();
   *
   *     String str = "Junit is working fine";
   *     assertEquals("Junit is working fine",str);
   */
}
