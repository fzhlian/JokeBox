import common from '@ohos.app.ability.common';
import { sourceDao } from '../data/Repositories';
import { globalRdbStore } from '../data/RdbStore';
import { SourceConfig, SourceType } from '../model/Models';

@Component
export struct AgeGatePage {
  @Link ageLocked: boolean;
  @Link complianceAccepted: boolean;
  @State ageChoice: string = 'adult';

  build() {
    Column({ space: 12 }) {
      Text('First Launch').fontSize(24).fontWeight(FontWeight.Bold)
      Text('Please choose age group and confirm compliance before continuing.')
      Row({ space: 8 }) {
        Button(this.ageChoice === 'teen' ? 'Teen (Selected)' : 'Teen').onClick(() => this.ageChoice = 'teen')
        Button(this.ageChoice === 'adult' ? 'Adult (Selected)' : 'Adult').onClick(() => this.ageChoice = 'adult')
      }
      Button('Confirm and Continue').onClick(() => {
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
      Text('JokeBox NEXT Main').fontSize(24).fontWeight(FontWeight.Bold)
      Button('Sources').onClick(() => this.route = 'sources')
      Button('Settings').onClick(() => this.route = 'settings')
    }.padding(16)
  }
}

@Component
export struct SourcesView {
  @Link route: string;
  @State sourceName: string = '';
  @State sourceUrl: string = '';
  @State error: string = '';
  @State success: string = '';
  @State sourceList: SourceConfig[] = [];

  aboutToAppear(): void {
    this.reload();
  }

  private async reload(): Promise<void> {
    this.sourceList = await sourceDao.listAll();
  }

  private async saveSource(): Promise<void> {
    const name = this.sourceName.trim();
    const url = this.sourceUrl.trim();
    if (!name) {
      this.error = 'Source name is required';
      this.success = '';
      return;
    }
    if (!url) {
      this.error = 'URL is required';
      this.success = '';
      return;
    }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      this.error = 'URL must start with http:// or https://';
      this.success = '';
      return;
    }
    this.error = '';
    await sourceDao.upsert({
      sourceId: `user-${Date.now()}`,
      type: SourceType.USER_ONLINE,
      name,
      enabled: true,
      supportedLanguages: ['zh-Hans', 'en'],
      configJson: JSON.stringify({ request: { method: 'GET', url } }),
      createdAt: Date.now(),
      updatedAt: Date.now()
    });
    this.sourceName = '';
    this.sourceUrl = '';
    this.success = 'Saved';
    await this.reload();
  }

  build() {
    Column({ space: 12 }) {
      Text('Sources').fontSize(22).fontWeight(FontWeight.Bold)
      TextInput({ placeholder: 'Source Name', text: this.sourceName }).onChange(v => this.sourceName = v)
      TextInput({ placeholder: 'URL', text: this.sourceUrl }).onChange(v => this.sourceUrl = v)
      if (this.error) {
        Text(this.error).fontColor(Color.Red)
      }
      if (this.success) {
        Text(this.success).fontColor(Color.Green)
      }
      Row({ space: 8 }) {
        Button('Back').onClick(() => this.route = 'main')
        Button('Save').onClick(() => { this.saveSource(); })
      }
      Divider()
      Text(`Configured Sources: ${this.sourceList.length}`)
      List() {
        ForEach(this.sourceList, (item: SourceConfig) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(item.name).fontSize(16).fontWeight(FontWeight.Medium)
              Text(item.sourceId).fontSize(12).fontColor('#666666')
            }.padding({ top: 8, bottom: 8 })
          }
        }, (item: SourceConfig) => item.sourceId)
      }.height('45%')
    }.padding(16)
  }
}

@Component
export struct SettingsPage {
  @Link route: string;
  @State autoUpdate: boolean = true;
  @State autoProcess: boolean = true;

  build() {
    Column({ space: 12 }) {
      Text('Settings').fontSize(22).fontWeight(FontWeight.Bold)
      Toggle({ type: ToggleType.Switch, isOn: this.autoUpdate })
        .onChange(v => this.autoUpdate = v)
      Text(`Auto Update: ${this.autoUpdate ? 'On' : 'Off'}`)
      Toggle({ type: ToggleType.Switch, isOn: this.autoProcess })
        .onChange(v => this.autoProcess = v)
      Text(`Background Process: ${this.autoProcess ? 'On' : 'Off'}`)
      Button('Back').onClick(() => this.route = 'main')
    }.padding(16)
  }
}

@Entry
@Component
struct Index {
  @State ageLocked: boolean = false;
  @State complianceAccepted: boolean = false;
  @State route: string = 'main';
  @State dbReady: boolean = false;
  @State dbError: string = '';

  async aboutToAppear(): Promise<void> {
    if (this.dbReady) {
      return;
    }
    try {
      const ctx = getContext(this) as common.UIAbilityContext;
      await globalRdbStore.open(ctx);
      this.dbReady = true;
    } catch (err) {
      this.dbError = `Database init failed: ${JSON.stringify(err)}`;
    }
  }

  build() {
    Navigation() {
      Column() {
        if (this.dbError) {
          Text(this.dbError).fontColor(Color.Red).padding(16)
        } else if (!this.dbReady) {
          Text('Initializing storage...').padding(16)
        } else if (!this.ageLocked || !this.complianceAccepted) {
          AgeGatePage({ ageLocked: $ageLocked, complianceAccepted: $complianceAccepted })
        } else if (this.route === 'main') {
          MainView({ route: $route })
        } else if (this.route === 'sources') {
          SourcesView({ route: $route })
        } else {
          SettingsPage({ route: $route })
        }
      }
    }
    .title('JokeBox NEXT')
  }
}
