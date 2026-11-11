import { useEffect, useState } from 'react'

type Entry = {
  attempt: string
  recordedAt: string | null
  outcome: 'READY' | 'INSUFFICIENT' | 'FAILED'
  evidence: string
  draft: string
}

const labels = { READY: '초안 생성', INSUFFICIENT: '근거 부족', FAILED: '생성 실패' }
const time = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', dateStyle: 'medium', timeStyle: 'medium',
})

export function AnalysisHistory({ videoId }: { videoId: string }) {
  const [open, setOpen] = useState(false)
  const [entries, setEntries] = useState<Entry[] | null>(null)
  const [error, setError] = useState(false)
  useEffect(() => {
    if (!open) return
    const controller = new AbortController()
    async function read() {
      try {
        const response = await fetch(`/api/analysis/videos/${videoId}/history`, {
          signal: controller.signal, cache: 'no-store',
        })
        if (!response.ok) throw new Error()
        const rows: Entry[] = await response.json()
        if (!controller.signal.aborted) { setEntries(rows); setError(false) }
      } catch {
        if (!controller.signal.aborted) setError(true)
      }
    }
    void read()
    return () => controller.abort()
  }, [open, videoId])

  return (
    <details onToggle={event => setOpen(event.currentTarget.open)}>
      <summary>AI 생성 이력</summary>
      {error ? <p role="alert">이력을 불러오지 못했습니다. 닫았다 다시 열어주세요.</p>
        : entries === null ? <p>이력을 불러오는 중입니다.</p>
        : entries.length === 0 ? <p>기록된 생성 작업이 없습니다.</p>
        : <ol>{entries.map(entry => (
          <li key={entry.attempt}>
            <strong>{labels[entry.outcome]}</strong> · {entry.recordedAt ? time.format(new Date(entry.recordedAt)) : '이전 기록 · 시각 미상'}
            {entry.draft && <blockquote>{entry.draft}</blockquote>}
            {entry.evidence && <p>근거 · {entry.evidence}</p>}
          </li>
        ))}</ol>}
    </details>
  )
}
