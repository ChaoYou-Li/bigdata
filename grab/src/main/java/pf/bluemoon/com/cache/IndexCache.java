package pf.bluemoon.com.cache;

import com.sun.xml.internal.ws.handler.HandlerException;
import lombok.Data;
import org.springframework.stereotype.Component;
import pf.bluemoon.com.config.InitTable;
import pf.bluemoon.com.entity.vo.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author chaoyou
 * @Date Create in 2023-08-15 14:33
 * @Modified by
 * @Version 1.0.0
 * @Description 服务缓存层
 */
@Data
@Component
public class IndexCache implements IdxService {
    public static IndexCache indexCache;

    private Map<String, Object> getCacheMap(String tableName) {
        try {
            String methodName = "get" + tableName.substring(0, 1).toUpperCase() + tableName.substring(1);
            Method method = indexCache.getClass().getMethod(methodName );
            return  (Map<String, Object>)method.invoke(indexCache);
        } catch (Exception e){
            throw new RuntimeException("读取缓存数据失败：" + e.getMessage());
        }
    }

    /**
     * 更新文件存储索引元数据
     *      1、当前主键未存在元数据，则初始化一个元数据到缓存
     *      2、当前主键已存在元数据，则覆盖原来缓存中的元数据
     *
     * @param tableName 表名
     * @param primryKeyVO 索引元数据
     * @return
     */
    public boolean setPkIdxMsg(String tableName, PrimryKeyVO primryKeyVO){
        if (null == tableName || null == primryKeyVO ||
                null == primryKeyVO.getKeyValue() || null == primryKeyVO.getWriteVO()){
            return false;
        }
        Map<String, WriteVO> keyMapWrite = getCacheByPk(tableName);
        keyMapWrite.put(primryKeyVO.getKeyValue().toString(), primryKeyVO.getWriteVO());
        return true;
    }

    /**
     * 读取文件存储索引元数据
     *
     * @param keyValue 主键
     * @param tableName 表名
     * @return
     */
    @Override
    public PrimryKeyVO getPkIdxMsg(Object keyValue, String tableName) throws CloneNotSupportedException {
        if (null == keyValue || null == tableName){
            return null;
        }
        Map<String, WriteVO> keyMapWrite = getCacheByPk(tableName);
        WriteVO writeVO = keyMapWrite.get(keyValue.toString());
        if (null == writeVO){
            return null;
        }
        WriteVO clone = (WriteVO) writeVO.clone();
        return new PrimryKeyVO(keyValue, clone);
    }

    public boolean rmPkIdxMsg(String tableName, PrimryKeyVO primryKeyVO){
        if (null == tableName || null == primryKeyVO ||
                null == primryKeyVO.getKeyValue() || null == primryKeyVO.getWriteVO()){
            return false;
        }
        Map<String, WriteVO> keyMapWrite = getCacheByPk(tableName);
        keyMapWrite.remove(primryKeyVO.getKeyValue().toString());
        return true;
    }

    public boolean setUkIdxMsg(String tableName, SingleKeyVO singleKeyVo){
        if (null == tableName || null == singleKeyVo || null == singleKeyVo.getKeyName() ||
                null == singleKeyVo.getKeyValue() || null == singleKeyVo.getWriteVO()){
            return false;
        }
        Map<String, Map<String, WriteVO>> fkNameMapKeyMapWrite = getCacheByUk(tableName);
        fkNameMapKeyMapWrite.get(singleKeyVo.getKeyName()).put(singleKeyVo.getKeyValue().toString(), singleKeyVo.getWriteVO());
        return true;
    }

    @Override
    public SingleKeyVO getUkIdxMsg(String keyName, Object keyValue, String tableName) throws CloneNotSupportedException {
        if (null == keyName || null == keyValue || null == tableName){
            return null;
        }
        Map<String, Map<String, WriteVO>> keyNameMapKeyValueMapWrite = getCacheByUk(tableName);
        WriteVO writeVO = keyNameMapKeyValueMapWrite.get(keyName).get(keyValue.toString());
        if (null == writeVO){
            return null;
        }
        WriteVO clone = (WriteVO) writeVO.clone();
        return new SingleKeyVO(keyName, keyValue, writeVO);
    }

    @Override
    public boolean rmUkIdxMsg(String tableName, SingleKeyVO singleKeyVo) {
        if (null == tableName || null == singleKeyVo || null == singleKeyVo.getKeyName() ||
                null == singleKeyVo.getKeyValue() || null == singleKeyVo.getWriteVO()){
            return false;
        }
        Map<String, Map<String, WriteVO>> keyNameMapKeyValueMapWrite = getCacheByUk(tableName);
        keyNameMapKeyValueMapWrite.get(singleKeyVo.getKeyName()).remove(singleKeyVo.getKeyValue().toString());
        return true;
    }

    @Override
    public boolean setFkIdxMsg(String tableName, SingleKeyVO singleKeyVO) {
        if (null == tableName || null == singleKeyVO ||
                null == singleKeyVO.getKeyName() || null == singleKeyVO.getKeyValue() ||
                null == singleKeyVO.getWriteVO()){
            return false;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByfk(tableName);
        Map<String, List<WriteVO>> keyValueMapGroup = fkNameMapKeyMapWrite.get(singleKeyVO.getKeyName());
        List<WriteVO> writeVOList = keyValueMapGroup.get(singleKeyVO.getKeyValue().toString());
        if (null == writeVOList){
            writeVOList = new ArrayList<>();
        }
        writeVOList.add(singleKeyVO.getWriteVO());
        keyValueMapGroup.put(singleKeyVO.getKeyValue().toString(), writeVOList);
        return true;
    }

    @Override
    public boolean setFkIdxMsg(String tableName, GroupKeyVO groupKeyVO) {
        if (null == tableName || null == groupKeyVO ||
                null == groupKeyVO.getKeyName() || null == groupKeyVO.getKeyValue() ||
                null == groupKeyVO.getWriteVOList()){
            return false;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByfk(tableName);
        Map<String, List<WriteVO>> keyValueMapGroup = fkNameMapKeyMapWrite.get(groupKeyVO.getKeyName());
        List<WriteVO> writeVOList = keyValueMapGroup.get(groupKeyVO.getKeyValue().toString());
        if (null == writeVOList){
            writeVOList = new ArrayList<>();
        }
        writeVOList.addAll(groupKeyVO.getWriteVOList());
        keyValueMapGroup.put(groupKeyVO.getKeyValue().toString(), writeVOList);
        return true;
    }

    @Override
    public GroupKeyVO getFkIdxMsg(String keyName, Object keyValue, String tableName) {
        if (null == keyName || null == keyValue || null == tableName){
            return null;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByfk(tableName);
        List<WriteVO> writeVOList = fkNameMapKeyMapWrite.get(keyName).get(keyValue.toString());
        if (null == writeVOList){
            return null;
        }
        return new GroupKeyVO(keyName, keyValue, new ArrayList<>(writeVOList));
    }

    @Override
    public boolean rmFkIdxMsg(String tableName, SingleKeyVO singleKeyVO) {
        if (null == tableName || null == singleKeyVO ||
                null == singleKeyVO.getKeyName() || null == singleKeyVO.getKeyValue() ||
                null == singleKeyVO.getWriteVO()){
            return false;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByfk(tableName);
        Map<String, List<WriteVO>> keyValueMapGroup = fkNameMapKeyMapWrite.get(singleKeyVO.getKeyName());
        List<WriteVO> writeVOList = keyValueMapGroup.get(singleKeyVO.getKeyValue().toString());
        if (null == writeVOList){
            return false;
        }
        return writeVOList.remove(singleKeyVO.getWriteVO());
    }

    @Override
    public boolean setFtIdxMsg(String tableName, SingleKeyVO singleKeyVO) {
        if (null == tableName || null == singleKeyVO ||
                null == singleKeyVO.getKeyName() || null == singleKeyVO.getKeyValue() ||
                null == singleKeyVO.getWriteVO()){
            return false;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByft(tableName);
        Map<String, List<WriteVO>> keyValueMapGroup = fkNameMapKeyMapWrite.get(singleKeyVO.getKeyName());
        List<WriteVO> writeVOList = keyValueMapGroup.get(singleKeyVO.getKeyValue().toString());
        if (null == writeVOList){
            writeVOList = new ArrayList<>();
        }
        writeVOList.add(singleKeyVO.getWriteVO());
        keyValueMapGroup.put(singleKeyVO.getKeyValue().toString(), writeVOList);
        return true;
    }

    @Override
    public boolean setFtIdxMsg(String tableName, GroupKeyVO groupKeyVO) {
        if (null == tableName || null == groupKeyVO ||
                null == groupKeyVO.getKeyName() || null == groupKeyVO.getKeyValue() ||
                null == groupKeyVO.getWriteVOList()){
            return false;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByft(tableName);
        Map<String, List<WriteVO>> keyValueMapGroup = fkNameMapKeyMapWrite.get(groupKeyVO.getKeyName());
        List<WriteVO> writeVOList = keyValueMapGroup.get(groupKeyVO.getKeyValue().toString());
        if (null == writeVOList){
            writeVOList = new ArrayList<>();
        }
        writeVOList.addAll(groupKeyVO.getWriteVOList());
        keyValueMapGroup.put(groupKeyVO.getKeyValue().toString(), writeVOList);
        return true;
    }

    @Override
    public GroupKeyVO getFtIdxMsg(String keyName, Object keyValue, String tableName) {
        if (null == keyName || null == keyValue || null == tableName){
            return null;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByft(tableName);
        List<WriteVO> writeVOList = fkNameMapKeyMapWrite.get(keyName).get(keyValue.toString());
        if (null == writeVOList){
            return null;
        }
        return new GroupKeyVO(keyName, keyValue, new ArrayList<>(writeVOList));
    }

    @Override
    public boolean rmFtIdxMsg(String tableName, SingleKeyVO singleKeyVO) {
        if (null == tableName || null == singleKeyVO ||
                null == singleKeyVO.getKeyName() || null == singleKeyVO.getKeyValue() ||
                null == singleKeyVO.getWriteVO()){
            return false;
        }
        Map<String, Map<String, List<WriteVO>>> fkNameMapKeyMapWrite = getCacheByft(tableName);
        Map<String, List<WriteVO>> keyValueMapGroup = fkNameMapKeyMapWrite.get(singleKeyVO.getKeyName());
        List<WriteVO> writeVOList = keyValueMapGroup.get(singleKeyVO.getKeyValue().toString());
        return writeVOList.remove(singleKeyVO.getWriteVO());
    }

    @Override
    public Map<String, WriteVO> getCacheByPk(String tableName) {
        if (null == tableName){
            return null;
        }
        return (Map<String, WriteVO>) getCacheMap(tableName).get(InitTable.PRIMRY_KEY);
    }

    @Override
    public Map<String, Map<String, WriteVO>> getCacheByUk(String tableName) {
        if (null == tableName){
            return null;
        }
        return (Map<String, Map<String, WriteVO>>) getCacheMap(tableName).get(InitTable.UNIQUE_KEY);
    }

    @Override
    public Map<String, Map<String, List<WriteVO>>> getCacheByfk(String tableName) {
        if (null == tableName){
            return null;
        }
        return (Map<String, Map<String, List<WriteVO>>>) getCacheMap(tableName).get(InitTable.FOREIGN_KEY);
    }

    @Override
    public Map<String, Map<String, List<WriteVO>>> getCacheByft(String tableName) {
        if (null == tableName){
            return null;
        }
        return (Map<String, Map<String, List<WriteVO>>>) getCacheMap(tableName).get(InitTable.FILE_TEXT);
    }

    public <T> void saveIdx(String tableName, T data, WriteVO writeVO) {
        TableDdlVO tableDdlVO = FileDDLCache.getTable(tableName);


    }
}


