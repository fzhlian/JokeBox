import { globalRdbStore } from './RdbStore';

export class SettingsDao {
  async getString(key: string, defaultValue: string): Promise<string> {
    const rows = await globalRdbStore.rawStore.querySql(
      'SELECT value FROM app_setting WHERE key = ?',
      [key]
    );
    try {
      if (rows.goToFirstRow()) {
        return rows.getString(rows.getColumnIndex('value'));
      }
      return defaultValue;
    } finally {
      rows.close();
    }
  }

  async getBoolean(key: string, defaultValue: boolean): Promise<boolean> {
    const value = await this.getString(key, defaultValue ? '1' : '0');
    return value === '1';
  }

  async getNumber(key: string, defaultValue: number): Promise<number> {
    const value = await this.getString(key, `${defaultValue}`);
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : defaultValue;
  }

  async putString(key: string, value: string): Promise<void> {
    await globalRdbStore.execute(
      'REPLACE INTO app_setting(key, value, updatedAt) VALUES(?, ?, ?)',
      [key, value, Date.now()]
    );
  }

  async putBoolean(key: string, value: boolean): Promise<void> {
    await this.putString(key, value ? '1' : '0');
  }

  async putNumber(key: string, value: number): Promise<void> {
    await this.putString(key, `${value}`);
  }
}

export const settingsDao = new SettingsDao();
