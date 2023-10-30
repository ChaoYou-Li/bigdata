package pf.bluemoon.com.exception;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-17 18:06
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class TableException extends RuntimeException implements BaseException {
    public TableException(String msg) {
        super(msg);
    }

    public static int getCode(){
        return tableCode;
    }
}
