package pf.bluemoon.com.exception;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-17 18:20
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ParamException extends RuntimeException implements BaseException {

    public ParamException(String msg) {
        super(msg);
    }

    public static int getCode(){
        return paramCode;
    }
}
