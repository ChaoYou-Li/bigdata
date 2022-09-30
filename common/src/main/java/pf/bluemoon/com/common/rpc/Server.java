package pf.bluemoon.com.common.rpc;

import java.io.IOException;

/**
 * @Author chaoyou
 * @Date Create in 17:46 2022/9/13
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public interface Server {
    public void stop();

    public void start() throws IOException;

    public void register(Class serviceInterface, Class impl);

    public boolean isRunning();

    public int getPort();
}
