# MiniBoss

Mini-boss conversion, powers, AI goals and loot for Rainbow Power.

## 环境

- Minecraft 1.20.1
- Forge 47.4.6+
- Java 17

## 构建

`powershell
.\gradlew.bat build
`

产物：uild/libs/miniboss-1.0.0.jar

## 安装

把 jar 放入整合包的 mods 目录，客户端与服务端都需要（纯客户端/纯服务端功能请自行加 clientSideOnly 或说明）。

## 当前内容

本项目由 Forge MDK 初始化，入口类：

- 包名：$Package
- 主类：$Name
- modId：$ModId

玩法功能尚未实现。

## 目录

`
src/main/java/com/rainbow/miniboss/   源码
src/main/resources/                            资源与 mods.toml
gradle.properties                              mod 元数据
`

## 开发约定

- 版本号改 gradle.properties 的 mod_version。
- 构建产物按 2MiniBoss-1.20.1-<version>.jar 命名后部署。
- 提交前必须 gradlew build 通过。

## 许可证

MIT，见 [LICENSE](LICENSE)。