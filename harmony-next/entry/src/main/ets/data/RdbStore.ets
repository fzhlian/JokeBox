import rdb from '@ohos.data.rdb';
import UIAbility from '@ohos.app.ability.UIAbility';

const DB_NAME: string = 'jokebox_next.db';
const DB_VERSION: number = 1;

const CREATE_SQLS: string[] = [
  `CREATE TABLE IF NOT EXISTS source_config (
    sourceId TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    name TEXT NOT NULL,
    enabled INTEGER NOT NULL,
    supportedLanguages TEXT,
    configJson TEXT,
    licenseNote TEXT,
    toSNote TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
  );`,
  `CREATE TABLE IF NOT EXISTS raw_queue (
    rawId INTEGER PRIMARY KEY AUTOINCREMENT,
    ownerSourceId TEXT NOT NULL,
    ownerSourceType TEXT NOT NULL,
    language TEXT,
    ageGroupHint INTEGER,
    payloadJson TEXT NOT NULL,
    fetchedAt INTEGER NOT NULL,
    status TEXT NOT NULL,
    failCount INTEGER NOT NULL DEFAULT 0,
    lastError TEXT,
    dropReason TEXT
  );`,
  `CREATE TABLE IF NOT EXISTS joke (
    id TEXT PRIMARY KEY,
    ageGroup INTEGER NOT NULL,
    language TEXT NOT NULL,
    content TEXT NOT NULL,
    contentNorm TEXT NOT NULL,
    simHash TEXT NOT NULL,
    sourceId TEXT NOT NULL,
    sourceType TEXT NOT NULL,
    sourceUrl TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
  );`,
  `CREATE TABLE IF NOT EXISTS played (jokeId TEXT PRIMARY KEY, playedAt INTEGER NOT NULL);`,
  `CREATE TABLE IF NOT EXISTS favorite (jokeId TEXT PRIMARY KEY, starredAt INTEGER NOT NULL);`,
  `CREATE TABLE IF NOT EXISTS playback_state (
    key TEXT PRIMARY KEY,
    lastJokeId TEXT,
    playedCount INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
  );`
];

export class JokeRdbStore {
  private store?: rdb.RdbStore;

  async open(context: UIAbility['context']): Promise<void> {
    this.store = await rdb.getRdbStore(context, {
      name: DB_NAME,
      securityLevel: rdb.SecurityLevel.S1
    }, DB_VERSION);
    for (const sql of CREATE_SQLS) {
      await this.store.executeSql(sql);
    }
  }

  async execute(sql: string, bindArgs: Array<rdb.ValueType> = []): Promise<void> {
    if (!this.store) {
      throw new Error('RDB store not initialized');
    }
    await this.store.executeSql(sql, bindArgs);
  }

  get rawStore(): rdb.RdbStore {
    if (!this.store) {
      throw new Error('RDB store not initialized');
    }
    return this.store;
  }
}

export const globalRdbStore = new JokeRdbStore();
