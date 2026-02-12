import { sourceDao } from '../data/Repositories';

@Component
export struct AgeGateView {
  @Link ageLocked: boolean;
  @Link complianceAccepted: boolean;

  build() {
    Column({ space: 12 }) {
      Text('首次启动').fontSize(24).fontWeight(FontWeight.Bold)
      Text('请先选择年龄段并确认合规声明，完成后将不再重复展示。')
      Button('确认并继续').onClick(() => {
        this.ageLocked = true;
        this.complianceAccepted = true;
      })
    }.padding(16)
  }
}

@Component
export struct MainView {
  @Link route: string;

  build() {
    Column({ space: 12 }) {
      Text('JokeBox Main').fontSize(24).fontWeight(FontWeight.Bold)
      Button('来源管理').onClick(() => this.route = 'sources')
      Button('设置').onClick(() => this.route = 'settings')
    }.padding(16)
  }
}

@Component
export struct SourcesView {
  @Link route: string;
  @State sourceName: string = '';
  @State sourceUrl: string = '';
  @State error: string = '';

  private async saveSource(): Promise<void> {
    if (!this.sourceName.trim()) {
      this.error = '来源名称不能为空';
      return;
    }
    if (!this.sourceUrl.startsWith('http://') && !this.sourceUrl.startsWith('https://')) {
      this.error = 'URL 必须以 http:// 或 https:// 开头';
      return;
    }
    this.error = '';
    await sourceDao.upsert({
      sourceId: `user-${Date.now()}`,
      type: 'USER_ONLINE',
      name: this.sourceName,
      enabled: true,
      supportedLanguages: ['zh-Hans', 'en'],
      configJson: JSON.stringify({ request: { method: 'GET', url: this.sourceUrl } }),
      createdAt: Date.now(),
      updatedAt: Date.now()
    });
    this.sourceName = '';
    this.sourceUrl = '';
  }

  build() {
    Column({ space: 12 }) {
      Text('Sources').fontSize(22).fontWeight(FontWeight.Bold)
      TextInput({ placeholder: '来源名称', text: this.sourceName }).onChange(v => this.sourceName = v)
      TextInput({ placeholder: 'URL', text: this.sourceUrl }).onChange(v => this.sourceUrl = v)
      if (this.error) {
        Text(this.error).fontColor(Color.Red)
      }
      Row({ space: 8 }) {
        Button('返回').onClick(() => this.route = 'main')
        Button('保存').onClick(() => { this.saveSource(); })
      }
    }.padding(16)
  }
}

@Component
export struct SettingsView {
  @Link route: string;
  @State autoUpdate: boolean = true;
  @State autoProcess: boolean = true;

  build() {
    Column({ space: 12 }) {
      Text('Settings').fontSize(22).fontWeight(FontWeight.Bold)
      Toggle({ type: ToggleType.Switch, isOn: this.autoUpdate })
        .onChange(v => this.autoUpdate = v)
      Text(`自动更新: ${this.autoUpdate ? '开' : '关'}`)
      Toggle({ type: ToggleType.Switch, isOn: this.autoProcess })
        .onChange(v => this.autoProcess = v)
      Text(`后台处理: ${this.autoProcess ? '开' : '关'}`)
      Button('返回').onClick(() => this.route = 'main')
    }.padding(16)
  }
}

@Entry
@Component
struct Index {
  @State ageLocked: boolean = false;
  @State complianceAccepted: boolean = false;
  @State route: string = 'main';

  build() {
    Navigation() {
      Column() {
        if (!this.ageLocked || !this.complianceAccepted) {
          AgeGateView({ ageLocked: $ageLocked, complianceAccepted: $complianceAccepted })
        } else if (this.route === 'main') {
          MainView({ route: $route })
        } else if (this.route === 'sources') {
          SourcesView({ route: $route })
        } else {
          SettingsView({ route: $route })
        }
      }
    }
    .title('JokeBox')
  }
}
