export enum SourceType {
  BUILTIN = 'BUILTIN',
  USER_ONLINE = 'USER_ONLINE',
  USER_OFFLINE = 'USER_OFFLINE'
}

export interface SourceConfig {
  sourceId: string;
  type: SourceType;
  name: string;
  enabled: boolean;
  supportedLanguages?: string[];
  configJson?: string;
  licenseNote?: string;
  toSNote?: string;
  createdAt: number;
  updatedAt: number;
}
