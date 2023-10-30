package pf.bluemoon.com.exception;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-17 17:58
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public enum ExceptEnum {
    NOT_FOUNT_TABLE(100001, "找不到数据表"),
    PARAM_CANNOT_BE_NULL(100002, "参数不能为空");

    private int code;
    private String msg;

    ExceptEnum(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
