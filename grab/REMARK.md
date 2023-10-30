改造缓存回滚过程 -> 重新构造 pf.bluemoon.com.config.InitCache.startIdxCache 方法

## 索引缓存结构
### 主键索引
```json
{
  "{TABLE_NAME}": {
    "{IDX_TYPE}": {
      "{PRIMARY_KEY}": {"position": 10, "length": 1000, "hashCode": 1233924}
    }
  }
}
```
### 其他索引
```json
{
  "{TABLE_NAME}": {
    "{IDX_TYPE}": {
      "{IDX_KEY_NAME}": {
        "{IDX_KEY_VALUE}": [
          "{PRIMARY_KEY}"
        ]
      }
    }
  }
}
```