export type UiLang = 'zh' | 'en';

const ZH_PREFIX = 'zh';

const TEXTS: Record<string, { zh: string; en: string }> = {
  firstLaunchTitle: { zh: '首次启动', en: 'First Launch' },
  firstLaunchDesc: { zh: '请选择年龄段并确认合规声明，仅首次展示。', en: 'Choose age group and confirm compliance. This page is shown only once.' },
  teen: { zh: '少年', en: 'Teen' },
  youth: { zh: '青年', en: 'Youth' },
  adult: { zh: '成人', en: 'Adult' },
  selectedSuffix: { zh: '（已选）', en: ' (Selected)' },
  compliance: { zh: '合规声明：本应用仅提供一般笑话内容。请遵守当地法律法规与平台条款，用户自定义来源由用户自行负责。', en: 'Compliance: This app provides general joke content only. Follow local laws and platform terms. User configured sources are user responsibility.' },
  confirmAndContinue: { zh: '确认并继续', en: 'Confirm and Continue' },
  sources: { zh: '来源', en: 'Sources' },
  settings: { zh: '设置', en: 'Settings' },
  favorite: { zh: '收藏/取消', en: 'Favorite' },
  update: { zh: '更新', en: 'Update' },
  clearRefetchZh: { zh: '清库并重抓中文', en: 'Clear and Refetch Chinese' },
  speak: { zh: '朗读', en: 'Speak' },
  stop: { zh: '停止', en: 'Stop' },
  process: { zh: '仅处理', en: 'Process' },
  resetPlayed: { zh: '重置已播', en: 'Reset Played' },
  noJoke: { zh: '暂无可播放笑话', en: 'No joke available' },
  sourceName: { zh: '来源名称', en: 'Source Name' },
  url: { zh: '链接', en: 'URL' },
  itemsPath: { zh: 'Items Path', en: 'Items Path' },
  contentPath: { zh: 'Content Path', en: 'Content Path' },
  languagePath: { zh: 'Language Path（可选）', en: 'Language Path (Optional)' },
  sourceUrlPath: { zh: 'Source URL Path（可选）', en: 'Source URL Path (Optional)' },
  licenseNote: { zh: '授权说明（可选）', en: 'License Note (Optional)' },
  back: { zh: '返回', en: 'Back' },
  test: { zh: '测试', en: 'Test' },
  save: { zh: '保存', en: 'Save' },
  clearUserSource: { zh: '清空用户来源数据', en: 'Clear User Source Data' },
  sourceNameRequired: { zh: '来源名称不能为空', en: 'Source name is required' },
  urlRequired: { zh: 'URL 不能为空', en: 'URL is required' },
  urlPrefix: { zh: 'URL 必须以 http:// 或 https:// 开头', en: 'URL must start with http:// or https://' },
  itemsRequired: { zh: 'Items path 不能为空', en: 'Items path is required' },
  contentRequired: { zh: 'Content path 不能为空', en: 'Content path is required' },
  testPassed: { zh: '连接测试通过（仅结构校验）', en: 'Connection test passed (schema check only)' },
  saved: { zh: '保存成功', en: 'Saved' },
  cleared: { zh: '已清空用户在线来源', en: 'User online sources cleared' },
  configuredSources: { zh: '已配置来源', en: 'Configured Sources' },
  uiLanguageMode: { zh: '界面语言模式', en: 'UI Language Mode' },
  contentLanguageMode: { zh: '内容语言模式', en: 'Content Language Mode' },
  systemMode: { zh: '跟随系统', en: 'System' },
  manualMode: { zh: '手动', en: 'Manual' },
  uiLanguage: { zh: '界面语言', en: 'UI Language' },
  contentLanguage: { zh: '内容语言', en: 'Content Language' },
  autoUpdate: { zh: '自动更新', en: 'Auto Update' },
  autoProcess: { zh: '后台处理', en: 'Background Process' },
  ttsVoice: { zh: 'TTS 音色', en: 'TTS Voice' },
  systemDefaultVoice: { zh: '系统默认', en: 'System Default' },
  volcCuteGirl: { zh: '火山引擎·可爱女生', en: 'Volcengine Cute Girl' },
  clearPlayed: { zh: '清空已播', en: 'Clear Played' }
};

export function resolveUiLang(uiMode: string, uiLang: string, systemLang: string): UiLang {
  const lang = uiMode === 'MANUAL' ? uiLang : systemLang;
  return lang.toLowerCase().startsWith(ZH_PREFIX) ? 'zh' : 'en';
}

export function t(lang: UiLang, key: string): string {
  const item = TEXTS[key];
  if (!item) {
    return key;
  }
  return lang === 'zh' ? item.zh : item.en;
}

export function ageLabel(lang: UiLang, age: string): string {
  if (age === 'teen') return t(lang, 'teen');
  if (age === 'youth') return t(lang, 'youth');
  return t(lang, 'adult');
}
