# change-classes-logger

一个帮助你在 IntelliJ IDEA 中手动热重载已修改类文件的插件。

![插件界面截图](img/screenshot.png)

## 项目背景

本插件提供了选择性热重载类文件的能力，解决了 IntelliJ IDEA 内置热重载机制的一个常见限制：

### 必要性
IntelliJ IDEA 默认的热重载机制会自动重载 classpath 下所有已修改的类文件。虽然这在大多数情况下是合理的，但当增量编译出现问题，错误地重新编译了不必要的类时，这个机制就会失效。本插件通过提供手动控制哪些类需要热重载的能力，有效解决了这个问题。

### 可行性
当只修改方法实现时，只需要重载当前类即可。即使是对类的成员和方法进行增删，我们也可以通过检查 git 状态来确定哪些类发生了变化，从而使用本工具进行有针对性的热重载。这种实现方式既简单又可靠，能够满足大多数开发场景的需求。

## 功能特点

- 显示所有类文件，并通过颜色标记不同状态：
  - 蓝色：已修改的文件
  - 绿色：新增的文件
  - 紫色：比调试会话更新的文件
  - 默认颜色：未更改的文件
- 支持手动选择热重载
- 实时显示类文件数量
- 支持文件搜索过滤
- 支持多种源文件类型（.java, .kt, .scala, .groovy）
- 与 [HotSwapHelper](https://github.com/gejun123456/HotSwapHelper) 配合使用可获得更强大的热重载能力

## 系统要求

### 构建要求
- JDK 21 或更高版本
- Gradle 8.0 或更高版本

### 目标 IDE 版本
- IntelliJ IDEA 2025.1 或更高版本
- 支持 Java 和 Kotlin 项目

## 安装方法

### 方法一：从 Release 下载（推荐）

1. 访问 [GitHub Releases](https://github.com/biuld/change-classes-logger/releases) 页面
2. 下载最新版本的 `changed-classes-logger-*.zip` 文件
3. 在 IntelliJ IDEA 中安装插件
   - 打开 IntelliJ IDEA
   - 进入 Settings/Preferences -> Plugins
   - 点击齿轮图标，选择 "Install Plugin from Disk..."
   - 选择下载的 zip 文件
   - 重启 IDE

### 方法二：从源码构建

1. 克隆代码到本地
```bash
git clone https://github.com/biuld/change-classes-logger.git
cd change-classes-logger
```

2. 构建插件
```bash
./gradlew buildPlugin
```
构建完成后，插件文件将位于 `build/distributions/change-classes-logger-1.1-SNAPSHOT.zip`

3. 在 IntelliJ IDEA 中安装插件
   - 打开 IntelliJ IDEA
   - 进入 Settings/Preferences -> Plugins
   - 点击齿轮图标，选择 "Install Plugin from Disk..."
   - 选择刚才构建的 `change-classes-logger-1.1-SNAPSHOT.zip` 文件
   - 重启 IDE

## 使用方法

1. 启动调试会话（Debug Session）
2. 修改源代码文件
3. 编译项目（Build Project）
   > 推荐执行 Gradle 的 class task：`./gradlew classes`
4. 在 IDE 右侧工具栏中找到 "Changed Classes" 标签
5. 点击刷新按钮（Refresh）更新文件列表
6. 文件将以不同颜色显示其状态：
   - 蓝色：已修改的文件
   - 绿色：新增的文件
   - 紫色：比调试会话更新的文件
   - 默认颜色：未更改的文件
7. 在右侧面板中选中需要热重载的文件
8. 右键点击选中的文件，选择 "HotSwap" 进行热重载

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

本项目采用 GNU 通用公共许可证 v3.0 发布。详见 [LICENSE](LICENSE) 文件。

本程序是自由软件：你可以根据自由软件基金会发布的 GNU 通用公共许可证第 3 版或更高版本的规定重新分发和/或修改它。

本程序是基于希望它有用而分发的，但没有任何保证；甚至没有对适销性或特定用途适用性的隐含保证。有关更多详细信息，请参阅 GNU 通用公共许可证。

你应该已经收到了一份 GNU 通用公共许可证的副本。如果没有，请访问 <https://www.gnu.org/licenses/>。 