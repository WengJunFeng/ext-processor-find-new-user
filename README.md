# 标记新用户

## 1. 概述

假设一个用户开启 App 后一定会触发一个事件 `AppStartEvent`，那么当服务端处理到 `AppStartEvent` 事件时，可按如下逻辑判断是否是新用户：

```java
  boolean isNewUser(String distinctId) {
    // 获取已出现 ID 集合
    Set<String> existUserIdSet = getExistedIdSet();
    // 如果不在集合中, 那么是新用户
    if (!existUserIdSet.contains(distinctId)) {
      // 将 ID 加入到已出现集合里
      existUserIdSet.add(distinctId);
      return true;
    }
    return false;
  }
```

实际逻辑请参考：[cn.sensorsdata.sample.ExtProcessorFindNewUser](https://github.com/sensorsdata/ext-processor-find-new-user/blob/master/src/main/java/cn/sensorsdata/sample/ExtProcessorFindNewUser.java)

更多关于 `数据预处理模块` 可以参考：
[ExtProcessor 数据预处理模块](https://www.sensorsdata.cn/manual/ext_processor.html)

使用本例中的方式识别新用户需要一个可持久化并有较高内存配置的 redis 实例，各云平台一般都有相关服务。 

## 2. 使用

### 2.1 下载代码

```bash
git clone https://github.com/sensorsdata/ext-processor-find-new-user.git
cd ext-processor-find-new-user
```

### 2.2 根据实际使用修改

类 [cn.sensorsdata.sample.ExtProcessorFindNewUser](https://github.com/sensorsdata/ext-processor-find-new-user/blob/master/src/main/java/cn/sensorsdata/sample/ExtProcessorFindNewUser.java) 需要根据实际情况修改:

1. PROCESS_PROJECT: 需要处理的项目列表，不在列表中的项目不会触发识别的流程;
2. REDIS_HOST, REDIS_PORT, REDIS_PASSWORD, REDIS_DB_INDEX: 存储 ID 集合的 redis 配置;
3. 触发该流程的事件名，例子中是 `AppStartEvent`，需根据实际情况修改，或修改触发方式，如根据某个属性字段;
4. 将判断是否是新用户的结果放入事件的属性中的字段名，例子中是 `NewUserByExtProcessor`;

其他注意点：

1. redis 访问会降低数据处理速度，应尽量减少 redis 读写;
2. 请仔细参考 [ExtProcessor 数据预处理模块](https://www.sensorsdata.cn/manual/ext_processor.html) 中 `2.1 开发常见问题`

### 2.3 编译

```bash
mvn clean package
```

### 2.4 导入已经存在的 ID 列表

在上线该数据预处理模块之前，可以导入已知的老用户 ID 列表，可参考以下代码修改并运行导入:

[cn.sensorsdata.sample.ImportIds](https://github.com/sensorsdata/ext-processor-find-new-user/blob/master/src/main/java/cn/sensorsdata/sample/ImportIds.java)

### 2.5 测试

将 `target/ext-processor-find-new-user-0.1.jar` 拷贝到部署 Sensors Analytics 的机器上，运行命令启动测试运行：

```
~/sa/extractor/bin/ext-processor-utils --jar ext-processor-find-new-user-0.1.jar --class cn.sensorsdata.sample.ExtProcessorFindNewUser --method test
```

先后输入同一个 ID 两个不同时间 `time` 的数据（数据需根据实际逻辑实现修改）：

```
{"distinct_id":"2b0a6f51a3cd6775","time":1434556035000,"type":"track","event":"AppStartEvent","project":"production","properties":{}}
{"distinct_id":"2b0a6f51a3cd6775","time":1434556035111,"type":"track","event":"AppStartEvent","project":"production","properties":{}}
```

若输出：

```
{"distinct_id":"2b0a6f51a3cd6775","time":1434556935000,"type":"track","event":"AppStartEvent","project":"production","properties":{"NewUserByExtProcessor":true}}
{"distinct_id":"2b0a6f51a3cd6775","time":1434556035000,"type":"track","event":"AppStartEvent","project":"production","properties":{"NewUserByExtProcessor":false}}
```

则说明预处理模块有效。

### 2.6 部署

运行如下命令部署：

```
~/sa/extractor/bin/ext-processor-utils --jar ext-processor-find-new-user-0.1.jar --class cn.sensorsdata.sample.ExtProcessorFindNewUser --method install
```

