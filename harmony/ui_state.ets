// ArkUI page state skeleton aligned with Android feature set.

export interface MainPageState {
  ageLocked: boolean
  ageGroup?: number
  complianceAccepted: boolean
  currentJoke?: string
  unplayedCount: number
  pendingCount: number
  message?: string
  tab: 'main' | 'sources' | 'settings'
}

export interface SourceEditorState {
  name: string
  url: string
  itemsPath: string
  contentPath: string
  languagePath: string
  sourceUrlPath: string
  licenseNote: string
}

export interface OfflineImportState {
  format: 'txt' | 'json' | 'csv' | 'html'
  content: string
}

export interface SettingsPageState {
  uiLanguageMode: 'SYSTEM' | 'MANUAL'
  uiLanguage: 'zh-Hans' | 'zh-Hant' | 'en'
  contentLanguageMode: 'SYSTEM' | 'MANUAL'
  contentLanguage: 'zh-Hans' | 'zh-Hant' | 'en'
  autoUpdateEnabled: boolean
  autoProcessEnabled: boolean
  ttsSpeed: number
  ttsPitch: number
}
