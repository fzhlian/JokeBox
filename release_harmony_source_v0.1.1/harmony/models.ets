export enum SourceType {
  BUILTIN = 'BUILTIN',
  USER_ONLINE = 'USER_ONLINE',
  USER_OFFLINE = 'USER_OFFLINE'
}

export enum RawStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  DONE = 'DONE',
  DROPPED = 'DROPPED',
  FAILED = 'FAILED'
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

export interface RawQueue {
  rawId: string;
  ownerSourceId: string;
  ownerSourceType: SourceType;
  language?: string;
  ageGroupHint?: number;
  payloadJson: string;
  fetchedAt: number;
  status: RawStatus;
  failCount: number;
  lastError?: string;
  dropReason?: string;
}

export interface Joke {
  id: string;
  ageGroup: number;
  language: string;
  content: string;
  contentNorm: string;
  simHash: string;
  sourceId: string;
  sourceType: SourceType;
  sourceUrl?: string;
  createdAt: number;
  updatedAt: number;
}
