package pf.bluemoon.com.enums;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 11:01
 * @Modified by
 * @Version 1.0.0
 * @Description 书架类型
 */
public enum BookType {
    FICTION(1, "小说", "fiction"),
    HISTORY(2, "历史", "history"),
    CULTURE(3, "文化", "culture"),
    PHILOSOPHY(4, "哲学", "philosophy"),
    POLITICS(5, "政治", "politics"),
    SCIENCE(6, "科学", "science");


    private Integer code;
    private String name;

    private String pkg;

    BookType(Integer code, String name, String pkg) {
        this.code = code;
        this.name = name;
        this.pkg = pkg;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getPkg() {
        return pkg;
    }

    public BookType getEnumByCode(Integer code){
        if (null == code){
            return null;
        }
        for (BookType type : BookType.values()) {
            if (type.getCode().equals(code)){
                return type;
            }
        }
        return null;
    }

    public BookType getEnumByName(String name){
        if (null == name){
            return null;
        }
        for (BookType type : BookType.values()) {
            if (type.getName().equals(name)){
                return type;
            }
        }
        return null;
    }
}
