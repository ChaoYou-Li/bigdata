package pf.bluemoon.com.entity.resp;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-08 14:49
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class ResponseBean {

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 400;
    private int code;
    private boolean success;
    private String msg;
    private Object data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public ResponseBean() {
    }

    public ResponseBean(int code, boolean success) {
        new ResponseBean(code, success, null, null);
    }

    public ResponseBean(int code, boolean success, String msg) {
        new ResponseBean(code, success, msg, null);
    }

    public ResponseBean(int code, boolean success, String msg, Object t) {
        this.code = code;
        this.success = success;
        this.msg = msg;
        this.data = t;
    }

    public static ResponseBean fail(){
        return new ResponseBean(FAIL_CODE, false);
    }

    public static ResponseBean success(){
        return new ResponseBean(SUCCESS_CODE, true);
    }
}
