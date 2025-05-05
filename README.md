# Change-classes-logger

一个帮助你在 IntelliJ IDEA 中手动热重载已修改类文件的插件。

![插件界面截图](img/screenshot.png)

## 功能特点

- 自动检测并显示已修改的类文件
- 支持手动选择要热重载的类
- 实时显示修改文件数量
- 支持文件搜索过滤
- 友好的用户界面

## 系统要求

### 构建要求
- JDK 21 或更高版本
- Gradle 8.0 或更高版本

### 目标 IDE 版本
- IntelliJ IDEA 2025.1 或更高版本
- 支持 Java 和 Kotlin 项目

## 安装方法

1. 克隆代码到本地
```bash
git clone https://github.com/biuld/change-classes-logger.git
cd change-classes-logger
```

2. 构建插件
```bash
./gradlew buildPlugin
```
构建完成后，插件文件将位于 `build/distributions/change-classes-logger-1.0-SNAPSHOT.zip`

3. 在 IntelliJ IDEA 中安装插件
   - 打开 IntelliJ IDEA
   - 进入 Settings/Preferences -> Plugins
   - 点击齿轮图标，选择 "Install Plugin from Disk..."
   - 选择刚才构建的 `change-classes-logger-1.0-SNAPSHOT.zip` 文件
   - 重启 IDE

## 使用方法

1. 启动调试会话（Debug Session）
2. 修改源代码文件
3. 编译项目（Build Project）
   > 推荐执行 Gradle 的 class task：`./gradlew classes`
4. 在 IDE 右侧工具栏中找到 "Changed Classes" 标签
5. 点击刷新按钮（Refresh）更新已修改的文件列表
6. 在右侧面板中选中需要热重载的文件
7. 右键点击选中的文件，选择 "HotSwap" 进行热重载

## 开发

本项目使用 Kotlin 和 Gradle 构建。

```bash
# 克隆项目
git clone https://github.com/biuld/change-classes-logger.git

# 构建插件
./gradlew buildPlugin

# 运行测试
./gradlew test
```

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。 