package pf.bluemoon.com.service.Impl;

import com.alibaba.fastjson.JSONObject;
import com.sun.xml.internal.ws.handler.HandlerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import pf.bluemoon.com.anno.PrimryKey;
import pf.bluemoon.com.cache.FileDDLCache;
import pf.bluemoon.com.cache.IndexCache;
import pf.bluemoon.com.config.InitCache;
import pf.bluemoon.com.config.InitTable;
import pf.bluemoon.com.config.ScheduledTask;
import pf.bluemoon.com.entity.Book;
import pf.bluemoon.com.entity.vo.*;
import pf.bluemoon.com.service.BaseService;
import pf.bluemoon.com.utils.FileUtils;

import java.lang.reflect.Method;
import java.util.*;


/**
 * @Author chaoyou
 * @Date Create in 2023-08-16 11:50
 * @Modified by
 * @Version 1.0.0
 * @Description
 */
public class BaseServiceImpl<T> extends BaseAbstract<T> implements BaseService<T> {

    @Value("${grab.server.data.io.type}")
    private String dataIoType;

    private static final String IO_TYPE = "type";
    private static final String IO_LINE = "line";

    @Autowired
    private IndexCache indexCache;
    @Autowired
    private ScheduledTask task;

    /**
     * 读数据接口
     *
     * @param keyName 当根据（唯一索引、外键索引、普通索引）字段读取数据时必填，索引名称
     * @param keyValue 索引值
     * @param idxType 索引类型（详情查看 InitTable）
     * @return
     * @throws ClassNotFoundException
     */
    private List<T> read(String keyName, Object keyValue, String idxType) {
        if (null == keyValue || null == keyName || null == idxType){
            throw new NullPointerException("读操作失败：keyValue or keyName cannot be null!!!");
        }
        try {
            GroupKeyVO groupKeyVO;
            if (InitTable.FOREIGN_KEY.equals(idxType)){
                groupKeyVO = indexCache.getFkIdxMsg(keyName, keyValue, getTableName());
            } else if (InitTable.FILE_TEXT.equals(idxType)){
                groupKeyVO = indexCache.getFtIdxMsg(keyName, keyValue, getTableName());
            } else {
                throw new NullPointerException("读操作失败：无法查找的索引类型");
            }
            List<T> dataList = new ArrayList<>();
            for (WriteVO writeVO : groupKeyVO.getWriteVOList()) {
                if (IO_LINE.equals(dataIoType)){
                    dataList.add(
                            JSONObject.parseObject(
                                    FileUtils.readLine(writeVO.getPosition(), getFilePath()),
                                    getGenericsClass()
                            )
                    );
                } else {
                    dataList.add(
                            JSONObject.parseObject(
                                    FileUtils.readByte(getFilePath(), writeVO.getPosition(), writeVO.getLength()),
                                    getGenericsClass()
                            )
                    );
                }
            }
            return dataList;
        } catch (Exception e){
            throw new NullPointerException("读操作失败：" + e.getMessage());
        }
    }

    private T readByPK(Object keyValue){
        if (null == keyValue){
            throw new NullPointerException("读操作失败：keyValue or idxType cannot be null!!!");
        }
        try {
            PrimryKeyVO pkIdxMsg = indexCache.getPkIdxMsg(keyValue, getTableName());
            if (null == pkIdxMsg || null == pkIdxMsg.getWriteVO()){
                return null;
            }
            WriteVO writeVO = pkIdxMsg.getWriteVO();
            if (IO_LINE.equals(dataIoType)){
                return JSONObject.parseObject(
                        FileUtils.readLine(writeVO.getPosition(), getFilePath()),
                        getGenericsClass()
                );
            } else {
                return JSONObject.parseObject(
                        FileUtils.readByte(getFilePath(), writeVO.getPosition(), writeVO.getLength()),
                        getGenericsClass()
                );
            }
        } catch (Exception e){
            throw new NullPointerException("读操作失败：" + e.getMessage());
        }
    }

    private T readByUK(String keyName, Object keyValue){
        if (null == keyValue || null == keyName){
            throw new NullPointerException("读操作失败：keyValue or idxType cannot be null!!!");
        }
        try {
            SingleKeyVO singleKeyVO = indexCache.getUkIdxMsg(keyName, keyValue, getTableName());
            if (null == singleKeyVO || null == singleKeyVO.getWriteVO()){
                return null;
            }
            WriteVO writeVO = singleKeyVO.getWriteVO();
            if (IO_LINE.equals(dataIoType)){
                return JSONObject.parseObject(
                        FileUtils.readLine(writeVO.getPosition(), getFilePath()),
                        getGenericsClass()
                );
            } else {
                return JSONObject.parseObject(
                        FileUtils.readByte(getFilePath(), writeVO.getPosition(), writeVO.getLength()),
                        getGenericsClass()
                );
            }
        } catch (Exception e){
            throw new NullPointerException("读操作失败：" + e.getMessage());
        }
    }

    /**
     * 写数据接口
     *
     * @param data 执行写的数据
     * @return
     */
    private synchronized WriteVO write(T data) {
        if (null == data){
            throw new NullPointerException("写操作失败：filePath or data cannot be null!!!");
        }

        WriteVO writeVO;
        try {
            if (IO_LINE.equals(dataIoType)){
                writeVO = FileUtils.appendLine(data, getFilePath());
            } else {
                writeVO = FileUtils.appendByte(data, getFilePath());
            }
            if (null == writeVO){
                throw new HandlerException("无法完成文件 I/O 功能");
            }

            // 保存索引逻辑
            return saveIdx(data, writeVO);
        } catch (Exception e){
            throw new HandlerException("写操作失败：" + e.getMessage());
        }
    }

    private synchronized List<WriteVO> batchWrite(List<T> data) {
        if (null == data || data.isEmpty()){
            throw new NullPointerException("写操作失败：filePath or data cannot be null!!!");
        }

        Map<T, WriteVO> writeVOMap;
        try {
            if (IO_LINE.equals(dataIoType)){
                writeVOMap = FileUtils.batchAppendLine(data, getFilePath());
            } else {
                writeVOMap = FileUtils.batchAppendByte(data, getFilePath());
            }
            if (null == writeVOMap){
                throw new HandlerException("无法完成文件 I/O 功能");
            }

            // 保存索引逻辑
            List<WriteVO> writeVOList = new ArrayList<>();
            Iterator<Map.Entry<T, WriteVO>> iterator = writeVOMap.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<T, WriteVO> entry = iterator.next();
                T t = entry.getKey();
                writeVOList.add(saveIdx(t, entry.getValue()));
            }
            return writeVOList;
        } catch (Exception e){
            throw new HandlerException("写操作失败：" + e.getMessage());
        }
    }

    private WriteVO saveIdx(T data, WriteVO writeVO) {
        PrimryKeyVO primryKeyVO = new PrimryKeyVO();
        List<SingleKeyVO> unList = new ArrayList<>();
        List<SingleKeyVO> fkList = new ArrayList<>();
        List<SingleKeyVO> ftList = new ArrayList<>();
        String tableName = null;
        try {
            /**
             * 数据写入成功，更新数据存储在文件的元数据：主键索引、外键索引、普通检索索引、唯一索引
             */
            tableName = getTableName();
            TableDdlVO tableDdlVO = FileDDLCache.getTable(tableName);

            indexCache.saveIdx(tableName, data, writeVO);

            // 主键索引
            primryKeyVO.setKeyValue(getPrimryKey(data));
            primryKeyVO.setWriteVO(writeVO);
            if (!indexCache.setPkIdxMsg(tableName, primryKeyVO)){
                throw new HandlerException("无法有效维护数据索引元数据到缓存中");
            }

            // 唯一索引
            if (null != tableDdlVO.getUniKeyMapFieldList() && tableDdlVO.getUniKeyMapFieldList().size() > 0){
                Map<String, List<String>> ukMapFieldList = tableDdlVO.getUniKeyMapFieldList();
                Iterator<Map.Entry<String, List<String>>> iterator = ukMapFieldList.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String, List<String>> entry = iterator.next();
                    SingleKeyVO singleKeyVO = new SingleKeyVO();
                    singleKeyVO.setKeyName(entry.getKey());
                    StringBuffer keyValue = new StringBuffer();
                    for (String field : entry.getValue()) {
                        keyValue.append(getValueByField(field, data));
                    }
                    singleKeyVO.setKeyValue(keyValue);
                    singleKeyVO.setWriteVO(writeVO);
                    unList.add(singleKeyVO);
                    if (!indexCache.setUkIdxMsg(tableName, singleKeyVO)){
                        throw new RuntimeException("保存唯一索引元数据到缓存层失败");
                    }
                }
            }

            // 外键索引
            if (null != tableDdlVO.getForeignKeyMapField() && tableDdlVO.getForeignKeyMapField().size() > 0){
                Map<String, String> foreignKeyMapField = tableDdlVO.getForeignKeyMapField();
                Iterator<Map.Entry<String, String>> iterator = foreignKeyMapField.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String, String> entry = iterator.next();
                    SingleKeyVO singleKeyVO = new SingleKeyVO();
                    singleKeyVO.setKeyName(entry.getKey());
                    singleKeyVO.setKeyValue(getValueByField(entry.getValue(), data));
                    singleKeyVO.setWriteVO(writeVO);
                    fkList.add(singleKeyVO);
                    if (!indexCache.setFkIdxMsg(tableName, singleKeyVO)){
                        throw new RuntimeException("保存外键索引元数据到缓存层失败");
                    }
                }
            }

            // 普通检索索引
            if (null != tableDdlVO.getIndexList() && tableDdlVO.getIndexList().size() > 0){
                for (String field : tableDdlVO.getIndexList()) {
                    SingleKeyVO singleKeyVO = new SingleKeyVO();
                    singleKeyVO.setKeyName(field);
                    singleKeyVO.setKeyValue(getValueByField(field, data));
                    singleKeyVO.setWriteVO(writeVO);
                    ftList.add(singleKeyVO);
                    if (!indexCache.setFtIdxMsg(tableName, singleKeyVO)){
                        throw new RuntimeException("保存普通检索索引元数据到缓存层失败");
                    }
                }
            }

            return writeVO;
        } catch (Exception e){
            /**
             * 索引元数据保存失败执行回滚缓存操作
             */
            // 回滚主键索引缓存
            indexCache.rmPkIdxMsg(tableName, primryKeyVO);
            // 回滚外键索引缓存
            for (SingleKeyVO groupKeyVO : fkList) {
                indexCache.rmFkIdxMsg(tableName, groupKeyVO);
            }
            // 回滚唯一键索引缓存
            for (SingleKeyVO groupKeyVO : unList) {
                indexCache.setUkIdxMsg(tableName, groupKeyVO);
            }
            // 回滚普通检索索引缓存
            for (SingleKeyVO groupKeyVO : ftList) {
                indexCache.rmFtIdxMsg(tableName, groupKeyVO);
            }
            // 回滚缓存
            task.saveIdxCache();
            return null;
        }
    }

    @Override
    public T selectByPk(Object id) {
        return (T)readByPK(id);
    }

    @Override
    public T selectByUk(String keyName, Object value) {
        return (T)readByUK(keyName, value);
    }

    @Override
    public List<T> selectByFk(String keyName, Object value) {
        return read(keyName, value, InitTable.FOREIGN_KEY);
    }

    @Override
    public List<T> selectByFt(String keyName, Object value) {
        return read(keyName, value, InitTable.FILE_TEXT);
    }

    @Override
    public List<T> selectALl() {
        List<T> result = new ArrayList<>();
        try {
            Map<String, WriteVO> keyMapWrite = indexCache.getCacheByPk(getTableName());
            for (String id : keyMapWrite.keySet()) {
                result.add(readByPK(id));
            }
        } catch (Exception e){
            throw new NullPointerException("读操作失败：" + e.getMessage());
        }
        return result;
    }

    @Override
    public List<T> selectByIds(List<Object> ids) {
        List<T> books = new ArrayList<>();
        ids.forEach(id -> {
            books.add(readByPK(id));
        });
        return books;
    }

    @Override
    public T save(T t) {
        try {
            checkIdx(t);
        } catch (Exception e){
            throw new HandlerException("数据保存失败：" + e.getMessage());
        }
        write(t);
        return t;
    }

    private void checkIdx(T t) throws ClassNotFoundException, IllegalAccessException {
        String tableName = getTableName();
        initPrimryKey(t);
        TableDdlVO tableDdlVO = FileDDLCache.getTable(tableName);
        if (null == tableDdlVO){
            throw new HandlerException("未找到存储的数据表");
        }

        // 确保主键唯一性
        Map<String, WriteVO> pkMapWrite = indexCache.getCacheByPk(tableName);
        while (null != pkMapWrite.get(getPrimryKey(t))){
            initPrimryKey(t);
        }

        // 判断唯一键逻辑
        if (null != tableDdlVO.getUniKeyMapFieldList() && tableDdlVO.getUniKeyMapFieldList().size() > 0){
            Map<String, Map<String, WriteVO>> keyNameMapKeyValueMapWrite = indexCache.getCacheByUk(tableName);
            Iterator<Map.Entry<String, List<String>>> iterator = tableDdlVO.getUniKeyMapFieldList().entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, List<String>> entry = iterator.next();
                Map<String, WriteVO> keyValueMapWrite = keyNameMapKeyValueMapWrite.get(entry.getKey());
                List<String> fieldList = entry.getValue();
                StringBuffer keyValue = new StringBuffer();
                for (String field : fieldList) {
                    keyValue.append(getValueByField(field, t));
                }
                if (null != keyValueMapWrite.get(keyValue.toString())){
                    throw new HandlerException("唯一索引 [ " + entry.getKey() + " ] 数据不可重复");
                }
            }
        }
    }

    @Override
    public List<T> batchSave(List<T> ts) {
        try {
            for (T t : ts) {
                checkIdx(t);
            }

        } catch (Exception e){
            throw new HandlerException("数据保存失败：" + e.getMessage());
        }

        return null;
    }

    @Override
    public boolean update(T t) {
        try {
            String tableName = getTableName();
            initPrimryKey(t);
            TableDdlVO tableDdlVO = FileDDLCache.getTable(tableName);
            if (null == tableDdlVO){
                throw new HandlerException("未找到存储的数据表");
            }

            // 确保主键唯一性
            Map<String, WriteVO> pkMapWrite = indexCache.getCacheByPk(tableName);
            while (null == getPrimryKey(t) || null == pkMapWrite.get(getPrimryKey(t))){
                throw new HandlerException("未找到存储的数据：{tableName=" + tableName + ", id=" + getPrimryKey(t) + "}");
            }

            // 判断唯一键逻辑
            if (null != tableDdlVO.getUniKeyMapFieldList() && tableDdlVO.getUniKeyMapFieldList().size() > 0){
                Map<String, Map<String, WriteVO>> keyNameMapKeyValueMapWrite = indexCache.getCacheByUk(tableName);
                Iterator<Map.Entry<String, List<String>>> iterator = tableDdlVO.getUniKeyMapFieldList().entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String, List<String>> entry = iterator.next();
                    Map<String, WriteVO> keyValueMapWrite = keyNameMapKeyValueMapWrite.get(entry.getKey());
                    List<String> fieldList = entry.getValue();
                    StringBuffer keyValue = new StringBuffer();
                    for (String field : fieldList) {
                        keyValue.append(getValueByField(field, t));
                    }
                    if (null != keyValueMapWrite.get(keyValue.toString())){
                        throw new HandlerException("唯一索引 [ " + entry.getKey() + " ] 数据不可重复");
                    }
                }
            }

        } catch (Exception e){
            throw new HandlerException("数据保存失败：" + e.getMessage());
        }
        write(t);
        return true;
    }

    @Override
    public boolean batchUpdate(List<T> ts) {
        return false;
    }

    @Override
    public T delete(Object id) {
        return null;
    }
}
