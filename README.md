## 一个用java编写的代码沙箱服务

用于接受代码并执行
- 可以使用java原生执行或者Docker容器执行
- 采用模版方法
- 可选java安全管理器
#### 流程
1. 保存代码为文件
2. 编译代码， 得到class文件
3. 执行代码
4. 收集整理输出结果
5. 文件清理
