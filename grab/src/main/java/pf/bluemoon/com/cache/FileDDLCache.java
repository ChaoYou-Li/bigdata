package pf.bluemoon.com.cache;

import pf.bluemoon.com.entity.vo.TableDdlVO;
import pf.bluemoon.com.exception.ExceptEnum;
import pf.bluemoon.com.exception.ParamException;
import pf.bluemoon.com.exception.TableException;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-21 13:58
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class FileDDLCache {
    private static Map<String, TableDdlVO> cache = new HashMap<>(4);

    public static TableDdlVO getTable(String tableName){
        if (null == tableName){
            throw new ParamException(ExceptEnum.PARAM_CANNOT_BE_NULL.getMsg());
        }
        TableDdlVO tableDdlVO = cache.get(tableName);
        if (null == tableDdlVO){
            throw new TableException(ExceptEnum.NOT_FOUNT_TABLE.getMsg());
        }
        return tableDdlVO;
    }
}
