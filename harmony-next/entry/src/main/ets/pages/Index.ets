import common from '@ohos.app.ability.common';
import i18n from '@ohos.i18n';
import { sourceDao } from '../data/Repositories';
import { globalRdbStore } from '../data/RdbStore';
import { SourceConfig, SourceType } from '../model/Models';
import { UiLang, ageLabel, resolveUiLang, t } from '../model/I18n';

const SAMPLE_JOKES_ZH: string[] = [
  '程序员笑话 1：改了一行，功能就好了。至于为什么，谁也说不清。',
  '程序员笑话 2：计算机里最难的两件事是命名和缓存失效。',
  '程序员笑话 3：产品说是小改动，编译器说要全量重建。',
  '程序员笑话 4：我先写个 TODO，留给未来的我惊喜。'
];

const SAMPLE_JOKES_EN: string[] = [
  'Programmer joke #1: I changed one line and everything works. Nobody knows why.',
  'Programmer joke #2: There are two hard problems in CS: cache invalidation and naming things.',
  'Programmer joke #3: Product says small change. Compiler says full rebuild.',
  'Programmer joke #4: I write TODO so future me can be surprised.'
];

function jokesByLanguageTag(language: string): string[] {
  return language.toLowerCase().startsWith('zh') ? SAMPLE_JOKES_ZH : SAMPLE_JOKES_EN;
}

@Component
export struct AgeGatePage {
  @Link ageLocked: boolean;
  @Link complianceAccepted: boolean;
  @Link selectedAgeGroup: string;
  @Prop lang: UiLang = 'en';

  private ageText(age: string): string {
    const base = ageLabel(this.lang, age);
    const selected = this.selectedAgeGroup === age ? t(this.lang, 'selectedSuffix') : '';
    return `${base}${selected}`;
  }

  build() {
    Column({ space: 12 }) {
      Text(t(this.lang, 'firstLaunchTitle')).fontSize(24).fontWeight(FontWeight.Bold)
      Text(t(this.lang, 'firstLaunchDesc'))
      Row({ space: 8 }) {
        Button(this.ageText('teen')).onClick(() => this.selectedAgeGroup = 'teen')
        Button(this.ageText('youth')).onClick(() => this.selectedAgeGroup = 'youth')
        Button(this.ageText('adult')).onClick(() => this.selectedAgeGroup = 'adult')
      }
      Scroll() {
        Text(t(this.lang, 'compliance'))
      }.height(120)
      Button(t(this.lang, 'confirmAndContinue')).onClick(() => {
        this.ageLocked = true;
        this.complianceAccepted = true;
      })
    }.padding(16)
  }
}

@Component
export struct MainPage {
  @Link route: string;
  @Link selectedAgeGroup: string;
  @Link currentLanguage: string;
  @Link unplayedCount: number;
  @Link processingCount: number;
  @Link currentJoke: string;
  @Link message: string;
  @Link jokeIndex: number;
  @Prop lang: UiLang = 'en';

  private jokes(): string[] {
    return jokesByLanguageTag(this.currentLanguage);
  }

  private prev(): void {
    const jokes = this.jokes();
    if (jokes.length === 0) {
      this.currentJoke = '';
      return;
    }
    this.jokeIndex = this.jokeIndex <= 0 ? jokes.length - 1 : this.jokeIndex - 1;
    this.currentJoke = jokes[this.jokeIndex];
  }

  private next(): void {
    const jokes = this.jokes();
    if (jokes.length === 0) {
      this.currentJoke = '';
      return;
    }
    this.jokeIndex = (this.jokeIndex + 1) % jokes.length;
    this.currentJoke = jokes[this.jokeIndex];
    this.unplayedCount = this.unplayedCount > 0 ? this.unplayedCount - 1 : 0;
  }

  build() {
    Column({ space: 10 }) {
      Text(`${t(this.lang, 'ageGroupLabel')}: ${ageLabel(this.lang, this.selectedAgeGroup)}`)
      Text(`${t(this.lang, 'languageLabel')}: ${this.currentLanguage}`)
      Text(`${t(this.lang, 'unplayedLabel')}: ${this.unplayedCount}, ${t(this.lang, 'pendingLabel')}: ${this.processingCount}`)

      Row({ space: 8 }) {
        Button(t(this.lang, 'prev')).onClick(() => this.prev())
        Button(t(this.lang, 'next')).onClick(() => this.next())
      }
      Row({ space: 8 }) {
        Button(t(this.lang, 'favorite')).onClick(() => this.message = t(this.lang, 'favorite'))
        Button(t(this.lang, 'update')).onClick(() => this.message = t(this.lang, 'update'))
      }
      Button(t(this.lang, 'clearRefetchZh')).onClick(() => {
        const jokes = jokesByLanguageTag('zh-Hans');
        this.currentLanguage = 'zh-Hans';
        this.unplayedCount = jokes.length;
        this.processingCount = 0;
        this.jokeIndex = 0;
        this.currentJoke = jokes.length > 0 ? jokes[0] : '';
      })
      Row({ space: 8 }) {
        Button(t(this.lang, 'speak')).onClick(() => this.message = t(this.lang, 'speak'))
        Button(t(this.lang, 'stop')).onClick(() => this.message = t(this.lang, 'stop'))
        Button(t(this.lang, 'process')).onClick(() => this.message = t(this.lang, 'process'))
      }
      Button(t(this.lang, 'resetPlayed')).onClick(() => {
        this.unplayedCount = this.jokes().length;
      })
      Row({ space: 8 }) {
        Button(t(this.lang, 'sources')).onClick(() => this.route = 'sources')
        Button(t(this.lang, 'settings')).onClick(() => this.route = 'settings')
      }
      if (this.message) {
        Text(this.message).fontColor('#666666')
      }
      Divider()
      Text(this.currentJoke ? this.currentJoke : t(this.lang, 'noJoke'))
    }.padding(16)
  }
}

@Component
export struct SourcesPage {
  @Link route: string;
  @Prop lang: UiLang = 'en';
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

  private validate(url: string): string {
    if (!this.sourceName.trim()) {
      return t(this.lang, 'sourceNameRequired');
    }
    if (!url) {
      return t(this.lang, 'urlRequired');
    }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return t(this.lang, 'urlPrefix');
    }
    if (!this.itemsPath.trim()) {
      return t(this.lang, 'itemsRequired');
    }
    if (!this.contentPath.trim()) {
      return t(this.lang, 'contentRequired');
    }
    return '';
  }

  private async testSource(): Promise<void> {
    const result = this.validate(this.sourceUrl.trim());
    this.error = result;
    this.success = result ? '' : t(this.lang, 'testPassed');
  }

  private async saveSource(): Promise<void> {
    const url = this.sourceUrl.trim();
    const result = this.validate(url);
    if (result) {
      this.error = result;
      this.success = '';
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
      name: this.sourceName.trim(),
      enabled: true,
      supportedLanguages: ['zh-Hans', 'zh-Hant', 'en'],
      configJson: JSON.stringify(config),
      licenseNote: this.licenseNote.trim(),
      toSNote: 'User configured source.',
      createdAt: Date.now(),
      updatedAt: Date.now()
    });
    this.error = '';
    this.success = t(this.lang, 'saved');
    await this.reload();
  }

  build() {
    Column({ space: 10 }) {
      Text(t(this.lang, 'sources')).fontSize(22).fontWeight(FontWeight.Bold)
      TextInput({ placeholder: t(this.lang, 'sourceName'), text: this.sourceName }).onChange(v => this.sourceName = v)
      TextInput({ placeholder: t(this.lang, 'url'), text: this.sourceUrl }).onChange(v => this.sourceUrl = v)
      TextInput({ placeholder: t(this.lang, 'itemsPath'), text: this.itemsPath }).onChange(v => this.itemsPath = v)
      TextInput({ placeholder: t(this.lang, 'contentPath'), text: this.contentPath }).onChange(v => this.contentPath = v)
      TextInput({ placeholder: t(this.lang, 'languagePath'), text: this.languagePath }).onChange(v => this.languagePath = v)
      TextInput({ placeholder: t(this.lang, 'sourceUrlPath'), text: this.sourceUrlPath }).onChange(v => this.sourceUrlPath = v)
      TextInput({ placeholder: t(this.lang, 'licenseNote'), text: this.licenseNote }).onChange(v => this.licenseNote = v)

      if (this.error) {
        Text(this.error).fontColor(Color.Red)
      }
      if (this.success) {
        Text(this.success).fontColor(Color.Green)
      }
      Row({ space: 8 }) {
        Button(t(this.lang, 'back')).onClick(() => this.route = 'main')
        Button(t(this.lang, 'test')).onClick(() => { this.testSource(); })
        Button(t(this.lang, 'save')).onClick(() => { this.saveSource(); })
      }
      Button(t(this.lang, 'clearUserSource')).onClick(async () => {
        await sourceDao.clearUserOnline();
        await this.reload();
        this.success = t(this.lang, 'cleared');
      })

      Divider()
      Text(`${t(this.lang, 'configuredSources')}: ${this.sourceList.length}`)
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
                Button(t(this.lang, 'delete')).onClick(async () => {
                  await sourceDao.delete(item.sourceId);
                  await this.reload();
                })
              }
            }.padding({ top: 8, bottom: 8 })
          }
        }, (item: SourceConfig) => item.sourceId)
      }.height('35%')
    }.padding(16)
  }
}

@Component
export struct SettingsPage {
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
  @Link currentLanguage: string;
  @Link unplayedCount: number;
  @Link message: string;
  @Prop lang: UiLang = 'en';

  build() {
    Column({ space: 10 }) {
      Text(t(this.lang, 'settings')).fontSize(22).fontWeight(FontWeight.Bold)
      Text(t(this.lang, 'uiLanguageMode'))
      Row({ space: 8 }) {
        Button(this.uiLanguageMode === 'SYSTEM' ? `${t(this.lang, 'systemMode')}${t(this.lang, 'selectedSuffix')}` : t(this.lang, 'systemMode'))
          .onClick(() => this.uiLanguageMode = 'SYSTEM')
        Button(this.uiLanguageMode === 'MANUAL' ? `${t(this.lang, 'manualMode')}${t(this.lang, 'selectedSuffix')}` : t(this.lang, 'manualMode'))
          .onClick(() => this.uiLanguageMode = 'MANUAL')
      }

      Text(t(this.lang, 'uiLanguage'))
      Row({ space: 8 }) {
        Button('zh-Hans').onClick(() => this.uiLanguage = 'zh-Hans')
        Button('zh-Hant').onClick(() => this.uiLanguage = 'zh-Hant')
        Button('en').onClick(() => this.uiLanguage = 'en')
      }

      Text(t(this.lang, 'contentLanguageMode'))
      Row({ space: 8 }) {
        Button(this.contentLanguageMode === 'SYSTEM' ? `${t(this.lang, 'systemMode')}${t(this.lang, 'selectedSuffix')}` : t(this.lang, 'systemMode'))
          .onClick(() => this.contentLanguageMode = 'SYSTEM')
        Button(this.contentLanguageMode === 'MANUAL' ? `${t(this.lang, 'manualMode')}${t(this.lang, 'selectedSuffix')}` : t(this.lang, 'manualMode'))
          .onClick(() => this.contentLanguageMode = 'MANUAL')
      }

      Text(t(this.lang, 'contentLanguage'))
      Row({ space: 8 }) {
        Button('zh-Hans').onClick(() => this.contentLanguage = 'zh-Hans')
        Button('zh-Hant').onClick(() => this.contentLanguage = 'zh-Hant')
        Button('en').onClick(() => this.contentLanguage = 'en')
      }

      Row({ space: 8 }) {
        Toggle({ type: ToggleType.Switch, isOn: this.autoUpdate }).onChange((v: boolean) => this.autoUpdate = v)
        Text(`${t(this.lang, 'autoUpdate')}: ${this.autoUpdate ? t(this.lang, 'on') : t(this.lang, 'off')}`)
      }
      Row({ space: 8 }) {
        Toggle({ type: ToggleType.Switch, isOn: this.autoProcess }).onChange((v: boolean) => this.autoProcess = v)
        Text(`${t(this.lang, 'autoProcess')}: ${this.autoProcess ? t(this.lang, 'on') : t(this.lang, 'off')}`)
      }

      Text(t(this.lang, 'ttsVoice'))
      Row({ space: 8 }) {
        Button(this.ttsVoiceProfileId === 'default' ? `${t(this.lang, 'systemDefaultVoice')}${t(this.lang, 'selectedSuffix')}` : t(this.lang, 'systemDefaultVoice'))
          .onClick(() => this.ttsVoiceProfileId = 'default')
        Button(this.ttsVoiceProfileId !== 'default' ? `${t(this.lang, 'volcCuteGirl')}${t(this.lang, 'selectedSuffix')}` : t(this.lang, 'volcCuteGirl'))
          .onClick(() => this.ttsVoiceProfileId = 'ICL_zh_female_keainvsheng_tob')
      }

      Text(`${t(this.lang, 'ttsSpeed')}: ${this.ttsSpeed.toFixed(2)}`)
      Slider({ value: this.ttsSpeed, min: 0.5, max: 2.0, step: 0.1 }).onChange((v: number) => this.ttsSpeed = v)
      Text(`${t(this.lang, 'ttsPitch')}: ${this.ttsPitch.toFixed(2)}`)
      Slider({ value: this.ttsPitch, min: 0.5, max: 2.0, step: 0.1 }).onChange((v: number) => this.ttsPitch = v)

      Button(t(this.lang, 'clearPlayed')).onClick(() => {
        this.unplayedCount = jokesByLanguageTag(this.currentLanguage).length;
        this.message = t(this.lang, 'clearPlayed');
      })
      Button(t(this.lang, 'back')).onClick(() => this.route = 'main')
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
  @State unplayedCount: number = SAMPLE_JOKES_ZH.length;
  @State processingCount: number = 0;
  @State currentJoke: string = SAMPLE_JOKES_ZH[0];
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
  @State systemLanguage: string = 'en';

  private currentUiLang(): UiLang {
    return resolveUiLang(this.uiLanguageMode, this.uiLanguage, this.systemLanguage);
  }

  async aboutToAppear(): Promise<void> {
    if (this.dbReady) {
      return;
    }
    try {
      this.systemLanguage = i18n.System.getSystemLanguage();
      const ctx = getContext(this) as common.UIAbilityContext;
      await globalRdbStore.open(ctx);
      this.dbReady = true;
      const jokes = jokesByLanguageTag(this.currentLanguage);
      this.unplayedCount = jokes.length;
      this.jokeIndex = 0;
      this.currentJoke = jokes.length > 0 ? jokes[0] : '';
    } catch (err) {
      this.dbError = `${t(this.currentUiLang(), 'dbInitFailed')}: ${JSON.stringify(err)}`;
    }
  }

  build() {
    const lang = this.currentUiLang();
    Navigation() {
      if (this.dbError) {
        Text(this.dbError).fontColor(Color.Red).padding(16)
      } else if (!this.dbReady) {
        Text(t(lang, 'initializingStorage')).padding(16)
      } else if (!this.ageLocked || !this.complianceAccepted) {
        AgeGatePage({
          ageLocked: $ageLocked,
          complianceAccepted: $complianceAccepted,
          selectedAgeGroup: $selectedAgeGroup,
          lang: lang
        })
      } else if (this.route === 'main') {
        MainPage({
          route: $route,
          selectedAgeGroup: $selectedAgeGroup,
          currentLanguage: $currentLanguage,
          unplayedCount: $unplayedCount,
          processingCount: $processingCount,
          currentJoke: $currentJoke,
          message: $message,
          jokeIndex: $jokeIndex,
          lang: lang
        })
      } else if (this.route === 'sources') {
        SourcesPage({ route: $route, lang: lang })
      } else {
        SettingsPage({
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
          currentLanguage: $currentLanguage,
          unplayedCount: $unplayedCount,
          message: $message,
          lang: lang
        })
      }
    }.title(t(lang, 'appTitleNext'))
  }
}


