package pf.bluemoon.com.common.nio;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author chaoyou
 * @Date Create in 18:44 2022/9/15
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
@Data
public class RpcModel {
    private String serviceName;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
}
