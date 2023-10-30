package pf.bluemoon.com.cache;

import pf.bluemoon.com.entity.vo.GroupKeyVO;
import pf.bluemoon.com.entity.vo.PrimryKeyVO;
import pf.bluemoon.com.entity.vo.SingleKeyVO;
import pf.bluemoon.com.entity.vo.WriteVO;

import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-09-11 09:58
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public interface IdxService {

    /**
     * 主键操作接口
     */
    boolean setPkIdxMsg(String tableName, PrimryKeyVO primryKeyVO);
    PrimryKeyVO getPkIdxMsg(Object keyValue, String tableName) throws CloneNotSupportedException;
    boolean rmPkIdxMsg(String tableName, PrimryKeyVO primryKeyVO);

    /**
     * 唯一键操作接口
     */
    boolean setUkIdxMsg(String tableName, SingleKeyVO singleKeyVo);
    SingleKeyVO getUkIdxMsg(String keyName, Object keyValue, String tableName) throws CloneNotSupportedException;
    boolean rmUkIdxMsg(String tableName, SingleKeyVO singleKeyVo);

    /**
     * 外键操作接口
     */
    boolean setFkIdxMsg(String tableName, SingleKeyVO singleKeyVO);
    boolean setFkIdxMsg(String tableName, GroupKeyVO groupKeyVO);
    GroupKeyVO getFkIdxMsg(String keyName, Object keyValue, String tableName);
    boolean rmFkIdxMsg(String tableName, SingleKeyVO singleKeyVO);

    /**
     * 普通检索索引操作接口
     */
    boolean setFtIdxMsg(String tableName, SingleKeyVO singleKeyVO);
    boolean setFtIdxMsg(String tableName, GroupKeyVO groupKeyVO);
    GroupKeyVO getFtIdxMsg(String keyName, Object keyValue, String tableName);
    boolean rmFtIdxMsg(String tableName, SingleKeyVO singleKeyVO);

    Map<String, WriteVO> getCacheByPk(String tableName);

    Map<String, Map<String, WriteVO>> getCacheByUk(String tableName);

    Map<String, Map<String, List<WriteVO>>> getCacheByfk(String tableName);

    Map<String, Map<String, List<WriteVO>>> getCacheByft(String tableName);
}
