import common from '@ohos.app.ability.common';
import { sourceDao } from '../data/Repositories';
import { globalRdbStore } from '../data/RdbStore';
import { SourceConfig, SourceType } from '../model/Models';

const SAMPLE_JOKES: string[] = [
  'Programmer joke #1: I changed one line and everything works. Nobody knows why.',
  'Programmer joke #2: There are two hard problems in CS: cache invalidation and naming things.',
  'Programmer joke #3: Product says small change. Compiler says full rebuild.',
  'Programmer joke #4: I write TODO so future me can be surprised.'
];

@Component
export struct AgeGateView {
  @Link ageLocked: boolean;
  @Link complianceAccepted: boolean;
  @Link selectedAgeGroup: string;

  build() {
    Column({ space: 12 }) {
      Text('First Launch').fontSize(24).fontWeight(FontWeight.Bold)
      Text('Choose age group and confirm compliance. This page is shown only once.')
      Row({ space: 8 }) {
        Button(this.selectedAgeGroup === 'teen' ? 'Teen (Selected)' : 'Teen')
          .onClick(() => this.selectedAgeGroup = 'teen')
        Button(this.selectedAgeGroup === 'youth' ? 'Youth (Selected)' : 'Youth')
          .onClick(() => this.selectedAgeGroup = 'youth')
        Button(this.selectedAgeGroup === 'adult' ? 'Adult (Selected)' : 'Adult')
          .onClick(() => this.selectedAgeGroup = 'adult')
      }
      Scroll() {
        Text(
          'Compliance Statement: This app provides general joke content only. You agree to local laws, ' +
          'platform terms, and age suitability responsibilities. User-provided sources are your responsibility.'
        )
      }.height(120)
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
  @Link selectedAgeGroup: string;
  @Link currentLanguage: string;
  @Link unplayedCount: number;
  @Link processingCount: number;
  @Link currentJoke: string;
  @Link message: string;
  @Link jokeIndex: number;

  private prev(): void {
    this.jokeIndex = this.jokeIndex <= 0 ? SAMPLE_JOKES.length - 1 : this.jokeIndex - 1;
    this.currentJoke = SAMPLE_JOKES[this.jokeIndex];
    this.message = 'Loaded previous joke';
  }

  private next(): void {
    this.jokeIndex = (this.jokeIndex + 1) % SAMPLE_JOKES.length;
    this.currentJoke = SAMPLE_JOKES[this.jokeIndex];
    this.unplayedCount = this.unplayedCount > 0 ? this.unplayedCount - 1 : 0;
    this.message = 'Loaded next joke';
  }

  build() {
    Column({ space: 10 }) {
      Text(`Age Group: ${this.selectedAgeGroup}`)
      Text(`Language: ${this.currentLanguage}`)
      Text(`Unplayed: ${this.unplayedCount}, Pending: ${this.processingCount}`)

      Row({ space: 8 }) {
        Button('Prev').onClick(() => this.prev())
        Button('Next').onClick(() => this.next())
      }
      Row({ space: 8 }) {
        Button('Favorite').onClick(() => this.message = 'Favorite toggled')
        Button('Update').onClick(() => this.message = 'Update requested')
      }
      Button('Clear and Refetch Chinese').onClick(() => {
        this.currentLanguage = 'zh-Hans';
        this.unplayedCount = 20;
        this.processingCount = 0;
        this.currentJoke = SAMPLE_JOKES[0];
        this.message = 'Refetch requested';
      })
      Row({ space: 8 }) {
        Button('Speak').onClick(() => this.message = 'Speak started')
        Button('Stop').onClick(() => this.message = 'Speak stopped')
        Button('Process').onClick(() => this.message = 'Process requested')
      }
      Button('Reset Played').onClick(() => {
        this.unplayedCount = SAMPLE_JOKES.length;
        this.message = 'Played reset';
      })
      Row({ space: 8 }) {
        Button('Sources').onClick(() => this.route = 'sources')
        Button('Settings').onClick(() => this.route = 'settings')
      }
      if (this.message) {
        Text(this.message).fontColor('#666666')
      }
      Divider()
      Text(this.currentJoke ? this.currentJoke : 'No joke available')
    }.padding(16)
  }
}

@Component
export struct SourcesView {
  @Link route: string;
  @State sourceName: string = '';
  @State sourceUrl: string = '';
  @State itemsPath: string = 'data.items';
  @State contentPath: string = 'content';
  @State languagePath: string = 'language';
  @State sourceUrlPath: string = 'url';
  @State licenseNote: string = '';
  @State error: string = '';
  @State success: string = '';
  @State sourceList: SourceConfig[] = [];

  aboutToAppear(): void {
    this.reload();
  }

  private async reload(): Promise<void> {
    this.sourceList = await sourceDao.listAll();
  }

  private async testSource(): Promise<void> {
    const testResult = sourceDao.testConfig(this.sourceUrl.trim(), this.itemsPath.trim(), this.contentPath.trim());
    if (testResult) {
      this.error = testResult;
      this.success = '';
      return;
    }
    this.error = '';
    this.success = 'Connection test passed (schema check only)';
  }

  private async saveSource(): Promise<void> {
    const name = this.sourceName.trim();
    const url = this.sourceUrl.trim();
    if (!name) {
      this.error = 'Source name is required';
      return;
    }
    const testResult = sourceDao.testConfig(url, this.itemsPath.trim(), this.contentPath.trim());
    if (testResult) {
      this.error = testResult;
      return;
    }
    const config = {
      request: { method: 'GET', url },
      parser: {
        type: 'json',
        itemsPath: this.itemsPath.trim(),
        fieldMap: {
          content: this.contentPath.trim(),
          language: this.languagePath.trim(),
          sourceUrl: this.sourceUrlPath.trim()
        }
      }
    };
    await sourceDao.upsert({
      sourceId: `user-${Date.now()}`,
      type: SourceType.USER_ONLINE,
      name,
      enabled: true,
      supportedLanguages: ['zh-Hans', 'zh-Hant', 'en'],
      configJson: JSON.stringify(config),
      licenseNote: this.licenseNote.trim(),
      toSNote: 'User configured source.',
      createdAt: Date.now(),
      updatedAt: Date.now()
    });
    this.error = '';
    this.success = 'Saved';
    await this.reload();
  }

  build() {
    Column({ space: 10 }) {
      Text('Sources').fontSize(22).fontWeight(FontWeight.Bold)
      TextInput({ placeholder: 'Source Name', text: this.sourceName }).onChange(v => this.sourceName = v)
      TextInput({ placeholder: 'URL', text: this.sourceUrl }).onChange(v => this.sourceUrl = v)
      TextInput({ placeholder: 'Items Path', text: this.itemsPath }).onChange(v => this.itemsPath = v)
      TextInput({ placeholder: 'Content Path', text: this.contentPath }).onChange(v => this.contentPath = v)
      TextInput({ placeholder: 'Language Path (optional)', text: this.languagePath }).onChange(v => this.languagePath = v)
      TextInput({ placeholder: 'Source URL Path (optional)', text: this.sourceUrlPath }).onChange(v => this.sourceUrlPath = v)
      TextInput({ placeholder: 'License Note (optional)', text: this.licenseNote }).onChange(v => this.licenseNote = v)
      if (this.error) {
        Text(this.error).fontColor(Color.Red)
      }
      if (this.success) {
        Text(this.success).fontColor(Color.Green)
      }
      Row({ space: 8 }) {
        Button('Back').onClick(() => this.route = 'main')
        Button('Test').onClick(() => { this.testSource(); })
        Button('Save').onClick(() => { this.saveSource(); })
      }
      Button('Clear User Source Data').onClick(async () => {
        await sourceDao.clearUserOnline();
        await this.reload();
        this.success = 'User online sources cleared';
      })
      Divider()
      Text(`Configured Sources: ${this.sourceList.length}`)
      List() {
        ForEach(this.sourceList, (item: SourceConfig) => {
          ListItem() {
            Column({ space: 6 }) {
              Text(item.name).fontSize(16).fontWeight(FontWeight.Medium)
              Text(item.sourceId).fontSize(12).fontColor('#666666')
              Row({ space: 8 }) {
                Toggle({ type: ToggleType.Switch, isOn: item.enabled }).onChange(async (v: boolean) => {
                  await sourceDao.setEnabled(item.sourceId, v);
                  await this.reload();
                })
                Button('Delete').onClick(async () => {
                  await sourceDao.delete(item.sourceId);
                  await this.reload();
                })
              }
            }.padding({ top: 8, bottom: 8 })
          }
        }, (item: SourceConfig) => item.sourceId)
      }.height('40%')
    }.padding(16)
  }
}

@Component
export struct SettingsView {
  @Link route: string;
  @Link uiLanguageMode: string;
  @Link uiLanguage: string;
  @Link contentLanguageMode: string;
  @Link contentLanguage: string;
  @Link autoUpdate: boolean;
  @Link autoProcess: boolean;
  @Link ttsVoiceProfileId: string;
  @Link ttsSpeed: number;
  @Link ttsPitch: number;
  @Link unplayedCount: number;
  @Link message: string;

  build() {
    Column({ space: 10 }) {
      Text('Settings').fontSize(22).fontWeight(FontWeight.Bold)
      Text('UI Language Mode')
      Row({ space: 8 }) {
        Button(this.uiLanguageMode === 'SYSTEM' ? 'System (Selected)' : 'System')
          .onClick(() => this.uiLanguageMode = 'SYSTEM')
        Button(this.uiLanguageMode === 'MANUAL' ? 'Manual (Selected)' : 'Manual')
          .onClick(() => this.uiLanguageMode = 'MANUAL')
      }
      Text('UI Language')
      Row({ space: 8 }) {
        Button('zh-Hans').onClick(() => this.uiLanguage = 'zh-Hans')
        Button('zh-Hant').onClick(() => this.uiLanguage = 'zh-Hant')
        Button('en').onClick(() => this.uiLanguage = 'en')
      }

      Text('Content Language Mode')
      Row({ space: 8 }) {
        Button(this.contentLanguageMode === 'SYSTEM' ? 'System (Selected)' : 'System')
          .onClick(() => this.contentLanguageMode = 'SYSTEM')
        Button(this.contentLanguageMode === 'MANUAL' ? 'Manual (Selected)' : 'Manual')
          .onClick(() => this.contentLanguageMode = 'MANUAL')
      }
      Text('Content Language')
      Row({ space: 8 }) {
        Button('zh-Hans').onClick(() => this.contentLanguage = 'zh-Hans')
        Button('zh-Hant').onClick(() => this.contentLanguage = 'zh-Hant')
        Button('en').onClick(() => this.contentLanguage = 'en')
      }

      Row({ space: 8 }) {
        Toggle({ type: ToggleType.Switch, isOn: this.autoUpdate }).onChange((v: boolean) => this.autoUpdate = v)
        Text(`Auto Update: ${this.autoUpdate ? 'On' : 'Off'}`)
      }
      Row({ space: 8 }) {
        Toggle({ type: ToggleType.Switch, isOn: this.autoProcess }).onChange((v: boolean) => this.autoProcess = v)
        Text(`Background Process: ${this.autoProcess ? 'On' : 'Off'}`)
      }

      Text('TTS Voice')
      Row({ space: 8 }) {
        Button(this.ttsVoiceProfileId === 'default' ? 'System Default (Selected)' : 'System Default')
          .onClick(() => this.ttsVoiceProfileId = 'default')
        Button(this.ttsVoiceProfileId !== 'default' ? 'Volcengine Cute Girl (Selected)' : 'Volcengine Cute Girl')
          .onClick(() => this.ttsVoiceProfileId = 'ICL_zh_female_keainvsheng_tob')
      }
      Text(`TTS Speed: ${this.ttsSpeed.toFixed(2)}`)
      Slider({ value: this.ttsSpeed, min: 0.5, max: 2.0, step: 0.1 }).onChange((v: number) => this.ttsSpeed = v)
      Text(`TTS Pitch: ${this.ttsPitch.toFixed(2)}`)
      Slider({ value: this.ttsPitch, min: 0.5, max: 2.0, step: 0.1 }).onChange((v: number) => this.ttsPitch = v)

      Button('Clear Played').onClick(() => {
        this.unplayedCount = SAMPLE_JOKES.length;
        this.message = 'Played state cleared';
      })
      Button('Back').onClick(() => this.route = 'main')
    }.padding(16)
  }
}

@Entry
@Component
struct Index {
  @State ageLocked: boolean = false;
  @State complianceAccepted: boolean = false;
  @State selectedAgeGroup: string = 'adult';
  @State route: string = 'main';
  @State dbReady: boolean = false;
  @State dbError: string = '';

  @State currentLanguage: string = 'zh-Hans';
  @State unplayedCount: number = SAMPLE_JOKES.length;
  @State processingCount: number = 0;
  @State currentJoke: string = SAMPLE_JOKES[0];
  @State message: string = '';
  @State jokeIndex: number = 0;

  @State uiLanguageMode: string = 'SYSTEM';
  @State uiLanguage: string = 'zh-Hans';
  @State contentLanguageMode: string = 'SYSTEM';
  @State contentLanguage: string = 'zh-Hans';
  @State autoUpdate: boolean = true;
  @State autoProcess: boolean = true;
  @State ttsVoiceProfileId: string = 'default';
  @State ttsSpeed: number = 1.0;
  @State ttsPitch: number = 1.0;

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
      if (this.dbError) {
        Text(this.dbError).fontColor(Color.Red).padding(16)
      } else if (!this.dbReady) {
        Text('Initializing storage...').padding(16)
      } else if (!this.ageLocked || !this.complianceAccepted) {
        AgeGateView({
          ageLocked: $ageLocked,
          complianceAccepted: $complianceAccepted,
          selectedAgeGroup: $selectedAgeGroup
        })
      } else if (this.route === 'main') {
        MainView({
          route: $route,
          selectedAgeGroup: $selectedAgeGroup,
          currentLanguage: $currentLanguage,
          unplayedCount: $unplayedCount,
          processingCount: $processingCount,
          currentJoke: $currentJoke,
          message: $message,
          jokeIndex: $jokeIndex
        })
      } else if (this.route === 'sources') {
        SourcesView({ route: $route })
      } else {
        SettingsView({
          route: $route,
          uiLanguageMode: $uiLanguageMode,
          uiLanguage: $uiLanguage,
          contentLanguageMode: $contentLanguageMode,
          contentLanguage: $contentLanguage,
          autoUpdate: $autoUpdate,
          autoProcess: $autoProcess,
          ttsVoiceProfileId: $ttsVoiceProfileId,
          ttsSpeed: $ttsSpeed,
          ttsPitch: $ttsPitch,
          unplayedCount: $unplayedCount,
          message: $message
        })
      }
    }
    .title('JokeBox')
  }
}
