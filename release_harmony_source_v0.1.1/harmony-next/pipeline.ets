export interface Fetcher {
  fetchOnce(limit: number): Promise<number>;
}

export interface Importer {
  importText(sourceId: string, text: string, language: string): Promise<void>;
}

export interface Processor {
  processBatch(ageGroup: number, language: string, threshold: number, batchSize: number): Promise<number>;
}
