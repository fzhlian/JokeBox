import { sourceDao } from '../data/Repositories';

@Component
export struct AgeGatePage {
  @Link ageLocked: boolean;
  @Link complianceAccepted: boolean;

  build() {
    Column({ space: 12 }) {
      Text('首次启动').fontSize(24).fontWeight(FontWeight.Bold)
      Text('请选择年龄段并确认合规声明。')
      Button('确认并进入').onClick(() => {
        this.ageLocked = true;
        this.complianceAccepted = true;
      })
    }.padding(16)
  }
}

@Component
export struct MainPage {
  @Link route: string;

  build() {
    Column({ space: 12 }) {
      Text('JokeBox NEXT Main').fontSize(24).fontWeight(FontWeight.Bold)
      Button('Sources').onClick(() => this.route = 'sources')
      Button('Settings').onClick(() => this.route = 'settings')
    }.padding(16)
  }
}

@Component
export struct SourcesPage {
  @Link route: string;
  @State name: string = '';
  @State url: string = '';
  @State error: string = '';

  private async onSave(): Promise<void> {
    if (!this.name.trim()) {
      this.error = '名称不能为空';
      return;
    }
    if (!this.url.startsWith('http://') && !this.url.startsWith('https://')) {
      this.error = 'URL 不合法';
      return;
    }
    this.error = '';
    await sourceDao.upsert({
      sourceId: `next-${Date.now()}`,
      type: 'USER_ONLINE',
      name: this.name,
      enabled: true,
      supportedLanguages: ['zh-Hans', 'en'],
      configJson: JSON.stringify({ request: { method: 'GET', url: this.url } }),
      createdAt: Date.now(),
      updatedAt: Date.now()
    });
  }

  build() {
    Column({ space: 12 }) {
      Text('Sources').fontSize(22)
      TextInput({ placeholder: '名称', text: this.name }).onChange(v => this.name = v)
      TextInput({ placeholder: 'URL', text: this.url }).onChange(v => this.url = v)
      if (this.error) {
        Text(this.error).fontColor(Color.Red)
      }
      Row({ space: 8 }) {
        Button('返回').onClick(() => this.route = 'main')
        Button('保存').onClick(() => { this.onSave(); })
      }
    }.padding(16)
  }
}

@Component
export struct SettingsPage {
  @Link route: string;
  @State autoUpdate: boolean = true;

  build() {
    Column({ space: 12 }) {
      Text('Settings').fontSize(22)
      Toggle({ type: ToggleType.Switch, isOn: this.autoUpdate }).onChange(v => this.autoUpdate = v)
      Text(`自动更新: ${this.autoUpdate ? '开' : '关'}`)
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
          AgeGatePage({ ageLocked: $ageLocked, complianceAccepted: $complianceAccepted })
        } else if (this.route === 'main') {
          MainPage({ route: $route })
        } else if (this.route === 'sources') {
          SourcesPage({ route: $route })
        } else {
          SettingsPage({ route: $route })
        }
      }
    }.title('JokeBox NEXT')
  }
}
