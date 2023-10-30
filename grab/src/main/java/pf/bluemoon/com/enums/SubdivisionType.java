package pf.bluemoon.com.enums;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-14 11:21
 * @Modified by
 * @Version 1.0.0
 * @Description 图书细分类型
 */
public enum SubdivisionType {
    FANTASY(1, "玄幻", BookType.FICTION, "fantasy"),
    MARTIAL_ARTS(2, "武侠", BookType.FICTION, "martial"),
    FAIRY(3,"修仙", BookType.FICTION, "fairy"),
    SCIENCE(4,"科幻", BookType.FICTION, "science"),
    URBAN(5,"都市", BookType.FICTION, "urban"),
    ROMANCE(6,"言情", BookType.FICTION, "romance"),
    HISTORY(7,"历史", BookType.FICTION, "history"),
    MILITARY(8,"军事", BookType.FICTION, "military"),
    GAMES(9,"游戏", BookType.FICTION, "games"),
    SPORTS(10,"体育", BookType.FICTION, "sports");


    private Integer code;
    private String name;
    private BookType type;

    private String pkg;

    SubdivisionType(Integer code, String name, BookType type, String pkg) {
        this.code = code;
        this.name = name;
        this.type = type;
        this.pkg = pkg;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BookType getType() {
        return type;
    }

    public String getPkg() {
        return pkg;
    }

    public static SubdivisionType getEnumByCodeAndType(Integer code, BookType parent){
        if (null == code || null == parent){
            return null;
        }
        for (SubdivisionType type : SubdivisionType.values()) {
            if (type.getCode().equals(code) && type.getType().getCode().equals(parent.getCode())){
                return type;
            }
        }
        return null;
    }

    public static SubdivisionType getEnumByNameAndType(String name, BookType parent){
        if (null == name || null == parent){
            return null;
        }
        for (SubdivisionType type : SubdivisionType.values()) {
            if (type.getName().equals(name) && type.getType().getCode().equals(parent.getCode())){
                return type;
            }
        }
        return null;
    }

    public static SubdivisionType getEnumLikeNameAndType(String name, BookType parent){
        if (null == name || null == parent){
            return null;
        }
        for (SubdivisionType type : SubdivisionType.values()) {
            if (name.contains(type.getName()) && type.getType().getCode().equals(parent.getCode())){
                return type;
            }
        }
        return null;
    }
}
