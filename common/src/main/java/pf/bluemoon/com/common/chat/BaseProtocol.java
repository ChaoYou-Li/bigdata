package pf.bluemoon.com.common.chat;

import java.nio.channels.SelectionKey;

/**
 * @Author chaoyou
 * @Date Create in 9:22 2022/9/19
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public interface BaseProtocol {
    void init();
    void close();
    byte[] listen();
    void send(byte[] bytes);
    byte[] receive(SelectionKey key);
}
