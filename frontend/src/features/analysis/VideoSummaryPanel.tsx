import { useEffect, useRef, useState } from 'react'

type VideoContext = {
  title: string
  description: string
  comments: string[]
}

type Summary =
  | { status: 'insufficient' }
  | { status: 'ready'; summary: string; source: string; quote: string }

export function VideoSummaryPanel({
  title,
  description,
  comments,
}: VideoContext) {
  const [summary, setSummary] = useState<Summary | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const request = useRef<AbortController | null>(null)
  useEffect(() => () => request.current?.abort(), [])

  async function summarize() {
    if (request.current) return
    const controller = new AbortController()
    request.current = controller
    setBusy(true)
    setError('')
    try {
      const response = await fetch('/api/analysis/summary', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, description, comments }),
        signal: AbortSignal.any([
          controller.signal,
          AbortSignal.timeout(75000),
        ]),
      })
      const data = await response.json().catch(() => null)
      if (!response.ok || !data)
        throw new Error(
          data?.message ||
            '요약을 가져오지 못했습니다. 서버 연결을 확인해주세요.',
        )
      if (!controller.signal.aborted) setSummary(data)
    } catch (cause) {
      if (!controller.signal.aborted)
        setError(
          cause instanceof Error && cause.name !== 'TimeoutError'
            ? cause.message
            : '요약 시간이 초과되었습니다.',
        )
    } finally {
      if (!controller.signal.aborted) setBusy(false)
      request.current = null
    }
  }

  return (
    <section>
      <button type="button" onClick={() => void summarize()} disabled={busy}>
        {busy ? '고민 요약 중…' : 'AI 고민 요약'}
      </button>
      <div aria-live="polite">
        {error && <p>{error}</p>}
        {summary?.status === 'insufficient' && (
          <p>자료에서 고민을 뒷받침할 인용을 확인하지 못했습니다.</p>
        )}
        {summary?.status === 'ready' && (
          <>
            <p>{summary.summary}</p>
            <blockquote>
              {summary.quote}
              <footer>{summary.source}</footer>
            </blockquote>
          </>
        )}
      </div>
    </section>
  )
}
