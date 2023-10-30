package pf.bluemoon.com.service;

import pf.bluemoon.com.entity.vo.WriteVO;

import java.util.List;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 10:32
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public interface BaseService<T> {

    /**
     * 根据指定主键读取数据
     *
     * @param value
     * @return
     */
    T selectByPk(Object value);

    T selectByUk(String keyName, Object value);

    List<T> selectByFk(String keyName, Object value);

    List<T> selectByFt(String keyName, Object value);

    /**
     * 根据主键索引读取当前数据表的所有数据
     *
     * @return
     */
    List<T> selectALl();

    List<T> selectByIds(List<Object> ids);

    T save(T t);

    List<T> batchSave(List<T> ts);

    boolean update(T t);

    boolean batchUpdate(List<T> ts);

    T delete(Object id);

}
