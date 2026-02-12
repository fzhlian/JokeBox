# Android MVP Scaffold

## 模块
- `data/db`：Room 实体与 DAO
- `data/repo`：设置、来源、播放仓库
- `domain/pipeline`：Fetcher/Importer/Processor
- `domain/policy`：normalize、分龄策略、sha256、simhash
- `ui`：ViewModel 与 Main 页面

## 启动路径
1. `JokeBoxApp` 初始化默认设置与内置源
2. Main 页面可执行：选龄、确认合规、更新、上一个/下一个、收藏、重置已播

## 待办（第二阶段）
1. Sources 页面（新增/编辑 USER_ONLINE，导入 JSON/CSV/TXT/HTML）
2. 合规声明强制勾选 + 输入确认文本
3. Settings 页面（UI/内容语言 SYSTEM/MANUAL、TTS 音色/语速/音调）
4. 错误可见化与重试策略 UI
