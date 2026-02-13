export class TextNormalizer {
  static normalize(text: string): string {
    return text
      .trim()
      .toLowerCase()
      .replace(/[\u200B-\u200D\uFEFF]/g, '')
      .replace(/\s+/g, ' ')
      .replace(/\u3000/g, ' ')
  }
}

export class ContentPolicy {
  private static hardBan: string[] = ['hate', 'terror', 'kill', '毒品', '仇恨', '极端']
  private static youthBan: string[] = ['sex', 'nsfw', '暴力', '色情']

  static allow(contentNorm: string, ageGroup: number): boolean {
    if (this.hardBan.some(x => contentNorm.includes(x))) {
      return false
    }
    if (ageGroup <= 1) {
      if (this.youthBan.some(x => contentNorm.includes(x))) {
        return false
      }
      if (contentNorm.length < 8) {
        return false
      }
    }
    return true
  }
}

export class Dedup {
  static exactHash(contentNorm: string): string {
    // TODO: replace by SHA-256 from crypto API.
    let hash = 0
    for (let i = 0; i < contentNorm.length; i++) {
      hash = ((hash << 5) - hash) + contentNorm.charCodeAt(i)
      hash |= 0
    }
    return `h${Math.abs(hash).toString(16)}`
  }

  static simHash(contentNorm: string): string {
    // TODO: replace by true 64-bit simhash implementation.
    return this.exactHash(contentNorm).padEnd(16, '0').slice(0, 16)
  }
}
