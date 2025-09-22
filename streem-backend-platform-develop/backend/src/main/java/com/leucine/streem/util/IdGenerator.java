package com.leucine.streem.util;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

/**
 * Java edition of Twitter <b>IdGenerator</b>, a service for generating
 * unique ID numbers at high scale with some simple guarantees.
 * https://github.com/twitter/snowflake
 */
public class IdGenerator {

  /**
   * private static final int UNUSED_BITS = 1; // Sign bit, Unused (always set to 0)
   * private static final int EPOCH_BITS = 41;
   * <p>
   * The following code can be used in future so that Users can decide the other parameters as per the environment.
   * // Create IdGenerator with a nodeId and custom epoch
   * private IdGenerator(long nodeId, long customEpoch) {
   * if (nodeId < 0 || nodeId > MAX_NODE_ID) throw new IllegalArgumentException(String.format("NodeId must be between %d and %d", 0, MAX_NODE_ID));
   * this.nodeId = nodeId;
   * this.customEpoch = customEpoch;
   * }
   * <p>
   * // Create IdGenerator with a nodeId
   * private IdGenerator(long nodeId) {
   * this(nodeId, DEFAULT_CUSTOM_EPOCH);
   * }
   */

  private static final IdGenerator INSTANCE = new IdGenerator();
  private static final int NODE_ID_BITS = 10;
  private static final int SEQUENCE_BITS = 12;

  private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1; // 1023
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1; // 4096

  // Custom Epoch (September 1, 2020 Midnight UTC = 2020-09-01T00:00:00Z)
  private static final long DEFAULT_CUSTOM_EPOCH = 1598918400000L;

  private final long customEpoch;
  private final long nodeId;
  private volatile long sequence = 0L;

  private volatile long lastTimestamp = -1L;

  // Let IdGenerator generate a nodeId and use the default custom epoch
  private IdGenerator() {
    nodeId = createNodeId();
    customEpoch = DEFAULT_CUSTOM_EPOCH;
  }

  public static IdGenerator getInstance() {
    return INSTANCE;
  }

  public synchronized long nextId() {
    long currentTimestamp = timestamp();
    if (currentTimestamp < lastTimestamp)
      throw new IllegalStateException(String.format("Clock moved backwards. Refusing to generate id for %d milliseconds", lastTimestamp - currentTimestamp));

    // reset sequence to start with zero for the next millisecond
    if (currentTimestamp == lastTimestamp) {
      sequence = (sequence + 1) & MAX_SEQUENCE;
      // Sequence Exhausted, wait till next millisecond.
      if (sequence == 0) currentTimestamp = waitNextMillis(currentTimestamp);
    } else sequence = 0;

    lastTimestamp = currentTimestamp;

    return currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS) | (nodeId << SEQUENCE_BITS) | sequence;
  }

  public long generateUnique() {
    return nextId();
  }

  // Get current timestamp in milliseconds, adjust for the custom epoch.
  private long timestamp() {
    return Instant.now().toEpochMilli() - customEpoch;
  }

  // Block and wait till next millisecond
  private long waitNextMillis(long currentTimestamp) {
    while (currentTimestamp == lastTimestamp) currentTimestamp = timestamp();
    return currentTimestamp;
  }

  @Override
  public String toString() {
    return "IdGenerator{" +
      "customEpoch=" + customEpoch +
      ", nodeId=" + nodeId +
      ", sequence=" + sequence +
      ", lastTimestamp=" + lastTimestamp +
      '}';
  }

  private long createNodeId() {
    long myNodeId;
    try {
      StringBuilder sb = new StringBuilder();
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        byte[] mac = networkInterface.getHardwareAddress();
        if (mac != null) for (byte macPort : mac) {
          sb.append(String.format("%02X", macPort));
        }
      }
      myNodeId = sb.toString().hashCode();
    } catch (Exception ex) {
      myNodeId = (new SecureRandom().nextInt());
    }
    myNodeId = myNodeId & MAX_NODE_ID;
    return myNodeId;
  }

  public long[] parse(long id) {
    long maskNodeId = ((1L << NODE_ID_BITS) - 1) << SEQUENCE_BITS;
    long maskSequence = (1L << SEQUENCE_BITS) - 1;

    long myTimestamp = (id >> (NODE_ID_BITS + SEQUENCE_BITS)) + customEpoch;
    long myNodeId = (id & maskNodeId) >> SEQUENCE_BITS;
    long mySequence = id & maskSequence;

    return new long[]{myTimestamp, myNodeId, mySequence};
  }

}
