import { SourceConfig, SourceType } from '../model/Models';
import { globalRdbStore } from './RdbStore';

export class SourceDao {
  async listAll(): Promise<SourceConfig[]> {
    const rows = await globalRdbStore.rawStore.querySql('SELECT * FROM source_config ORDER BY updatedAt DESC');
    const list: SourceConfig[] = [];
    try {
      while (rows.goToNextRow()) {
        list.push({
          sourceId: rows.getString(rows.getColumnIndex('sourceId')),
          type: rows.getString(rows.getColumnIndex('type')) as SourceType,
          name: rows.getString(rows.getColumnIndex('name')),
          enabled: rows.getLong(rows.getColumnIndex('enabled')) === 1,
          supportedLanguages: rows.getString(rows.getColumnIndex('supportedLanguages'))?.split(',') ?? [],
          configJson: rows.getString(rows.getColumnIndex('configJson')),
          licenseNote: rows.getString(rows.getColumnIndex('licenseNote')),
          toSNote: rows.getString(rows.getColumnIndex('toSNote')),
          createdAt: rows.getLong(rows.getColumnIndex('createdAt')),
          updatedAt: rows.getLong(rows.getColumnIndex('updatedAt'))
        });
      }
    } finally {
      rows.close();
    }
    return list;
  }

  async upsert(item: SourceConfig): Promise<void> {
    const now = Date.now();
    await globalRdbStore.execute(
      `REPLACE INTO source_config(sourceId,type,name,enabled,supportedLanguages,configJson,licenseNote,toSNote,createdAt,updatedAt)
       VALUES(?,?,?,?,?,?,?,?,?,?)`,
      [
        item.sourceId,
        item.type,
        item.name,
        item.enabled ? 1 : 0,
        (item.supportedLanguages ?? []).join(','),
        item.configJson ?? '',
        item.licenseNote ?? '',
        item.toSNote ?? '',
        item.createdAt || now,
        now
      ]
    );
  }

  async setEnabled(sourceId: string, enabled: boolean): Promise<void> {
    await globalRdbStore.execute(
      'UPDATE source_config SET enabled = ?, updatedAt = ? WHERE sourceId = ?',
      [enabled ? 1 : 0, Date.now(), sourceId]
    );
  }

  async delete(sourceId: string): Promise<void> {
    await globalRdbStore.execute('DELETE FROM source_config WHERE sourceId = ?', [sourceId]);
  }

  async clearUserOnline(): Promise<void> {
    await globalRdbStore.execute("DELETE FROM source_config WHERE type = 'USER_ONLINE'");
  }

  testConfig(url: string, itemsPath: string, contentPath: string): string {
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      return 'URL must start with http:// or https://';
    }
    if (!itemsPath.trim()) {
      return 'Items path is required';
    }
    if (!contentPath.trim()) {
      return 'Content path is required';
    }
    return '';
  }
}

export const sourceDao = new SourceDao();
