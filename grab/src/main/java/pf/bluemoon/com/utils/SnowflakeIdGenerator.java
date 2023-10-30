package pf.bluemoon.com.utils;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 15:12
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class SnowflakeIdGenerator {
    private static final long EPOCH = 1609459200000L; // 设置起始时间戳，如：2021-01-01 00:00:00
    private static final long WORKER_ID_BITS = 5L; // Worker ID 的位数
    private static final long DATA_CENTER_ID_BITS = 5L; // Data Center ID 的位数
    private static final long SEQUENCE_BITS = 12L; // 序列号的位数

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // Worker ID 的最大值
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS); // Data Center ID 的最大值

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS; // Worker ID 左移位数
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // Data Center ID 左移位数
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS; // 时间戳左移位数
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS); // 序列号的掩码

    private final long workerId;
    private final long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
        }
        if (dataCenterId < 0 || dataCenterId > MAX_DATA_CENTER_ID) {
            throw new IllegalArgumentException("Data Center ID must be between 0 and " + MAX_DATA_CENTER_ID);
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    public synchronized long generateId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Invalid system clock");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT) |
                (dataCenterId << DATA_CENTER_ID_SHIFT) |
                (workerId << WORKER_ID_SHIFT) |
                sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
