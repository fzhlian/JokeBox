import { SourceConfig, SourceType } from '../../../../models';
import { globalRdbStore } from './RdbStore';

export class SourceDao {
  async listEnabled(): Promise<SourceConfig[]> {
    const rows = await globalRdbStore.rawStore.querySql('SELECT * FROM source_config WHERE enabled = 1 ORDER BY updatedAt DESC');
    const list: SourceConfig[] = [];
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
    return list;
  }

  async upsert(item: SourceConfig): Promise<void> {
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
        item.createdAt,
        Date.now()
      ]
    );
  }
}

export const sourceDao = new SourceDao();
