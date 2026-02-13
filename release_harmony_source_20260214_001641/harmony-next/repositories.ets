import { Fetcher, Importer, Processor } from './pipeline'
import { RawQueue, RawStatus, SourceConfig, SourceType, Joke } from './models'
import { ContentPolicy, Dedup, TextNormalizer } from './text_processing'

export class MemorySourceRepo {
  private sources: SourceConfig[] = []

  setSources(items: SourceConfig[]): void {
    this.sources = items
  }

  listEnabledFetchable(): SourceConfig[] {
    return this.sources.filter(s => s.enabled && (s.type === SourceType.BUILTIN || s.type === SourceType.USER_ONLINE))
  }
}

export class MemoryQueueRepo {
  rawQueue: RawQueue[] = []
  jokes: Joke[] = []

  addRaw(items: RawQueue[]): void {
    this.rawQueue.push(...items)
  }

  listPending(limit: number): RawQueue[] {
    return this.rawQueue.filter(x => x.status === RawStatus.PENDING).slice(0, limit)
  }
}

export class LocalFetcher implements Fetcher {
  constructor(private sourceRepo: MemorySourceRepo, private queueRepo: MemoryQueueRepo) {}

  async fetchOnce(limit: number): Promise<number> {
    const enabled = this.sourceRepo.listEnabledFetchable()
    const now = Date.now()
    let count = 0
    for (const src of enabled) {
      const samples: RawQueue[] = []
      for (let i = 0; i < Math.min(3, limit); i++) {
        samples.push({
          rawId: `${src.sourceId}-${now}-${i}`,
          ownerSourceId: src.sourceId,
          ownerSourceType: src.type,
          payloadJson: JSON.stringify({ content: `sample-${i} from ${src.name}` }),
          fetchedAt: now,
          status: RawStatus.PENDING,
          failCount: 0
        })
      }
      this.queueRepo.addRaw(samples)
      count += samples.length
    }
    return count
  }
}

export class LocalImporter implements Importer {
  constructor(private queueRepo: MemoryQueueRepo) {}

  async importText(sourceId: string, text: string, language: string): Promise<void> {
    const now = Date.now()
    const lines = text.split('\n').map(x => x.trim()).filter(x => x.length > 0)
    this.queueRepo.addRaw(lines.map((line, index) => ({
      rawId: `${sourceId}-${now}-${index}`,
      ownerSourceId: sourceId,
      ownerSourceType: SourceType.USER_OFFLINE,
      language,
      payloadJson: JSON.stringify({ content: line, language }),
      fetchedAt: now,
      status: RawStatus.PENDING,
      failCount: 0
    })))
  }
}

export class LocalProcessor implements Processor {
  constructor(private queueRepo: MemoryQueueRepo) {}

  async processBatch(ageGroup: number, language: string, threshold: number, batchSize: number): Promise<number> {
    void threshold
    const pending = this.queueRepo.listPending(batchSize)
    let accepted = 0
    for (const raw of pending) {
      raw.status = RawStatus.PROCESSING
      try {
        const obj = JSON.parse(raw.payloadJson) as Record<string, string>
        const content = (obj.content ?? '').trim()
        const norm = TextNormalizer.normalize(content)
        if (!ContentPolicy.allow(norm, ageGroup)) {
          raw.status = RawStatus.DROPPED
          raw.dropReason = 'policy'
          continue
        }
        const id = Dedup.exactHash(norm)
        if (this.queueRepo.jokes.some(j => j.id === id)) {
          raw.status = RawStatus.DROPPED
          raw.dropReason = 'duplicate'
          continue
        }
        this.queueRepo.jokes.push({
          id,
          ageGroup,
          language,
          content,
          contentNorm: norm,
          simHash: Dedup.simHash(norm),
          sourceId: raw.ownerSourceId,
          sourceType: raw.ownerSourceType,
          createdAt: Date.now(),
          updatedAt: Date.now()
        })
        raw.status = RawStatus.DONE
        accepted++
      } catch (err) {
        raw.status = RawStatus.FAILED
        raw.failCount += 1
        raw.lastError = `${err}`
      }
    }
    return accepted
  }
}
