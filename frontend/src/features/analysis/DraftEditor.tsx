import { useEffect, useRef, useState } from 'react'
import type { Video } from '../video/examples'

export function DraftEditor({ video, draft, onEdit, actualVideo = false }: { video: Video; draft: string; onEdit: (text: string) => void; actualVideo?: boolean }) {
  const length = Array.from(draft.trim()).length
  const valid = length >= 40 && length <= 120
  const [evidence, setEvidence] = useState(video.evidence)
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const request = useRef<AbortController | null>(null)
  useEffect(() => () => request.current?.abort(), [])

  async function generate() {
    if (request.current) return
    const controller = new AbortController()
    request.current = controller
    setPending(true)
    setMessage('AI 초안을 생성하고 있습니다.')
    const timeout = window.setTimeout(() => controller.abort(), 70000)
    try {
      const response = await fetch('/api/analysis/draft', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: video.title, evidence: evidence.trim() }),
        signal: controller.signal,
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.message ?? 'AI 요청에 실패했습니다.')
      if (typeof data.draft !== 'string') throw new Error('AI 응답 형식이 올바르지 않습니다.')
      if (controller.signal.aborted) return
      onEdit(data.draft)
      setMessage('AI 초안이 생성되었습니다. 근거와 본문을 검토해주세요.')
    } catch (error) {
      setMessage(controller.signal.aborted ? '요청이 취소되거나 시간이 초과되었습니다. 다시 시도해주세요.'
        : error instanceof Error ? error.message : 'AI 요청에 실패했습니다.')
    } finally {
      window.clearTimeout(timeout)
      request.current = null
      setPending(false)
    }
  }

  return <>
    <h3>{video.title}</h3><p>{video.concern}</p>
    <label htmlFor="evidence">확인한 영상 근거</label><textarea id="evidence" value={evidence} maxLength={10000} disabled={pending} onChange={event => setEvidence(event.target.value)} rows={3} />
    <p>{actualVideo ? '직접 확인한 영상 근거를 입력하면 AI 초안을 생성할 수 있습니다. 자막은 자동 수집하지 않습니다.' : '예시 영상의 근거를 사용해 실제 AI 초안을 생성합니다.'}</p>
    <button disabled={pending || !evidence.trim()} onClick={() => void generate()}>{pending ? '생성 중…' : 'AI 초안 생성·재생성'}</button>
    <p role="status">{message}</p>
    <label htmlFor="draft">댓글 초안</label>
    <textarea id="draft" value={draft} disabled={pending} onChange={e => onEdit(e.target.value)} rows={5} aria-invalid={!valid} aria-describedby="length" />
    <small id="length" aria-live="polite">{length}자 · {valid ? '글자 수 충족' : '40~120자로 작성하세요'}</small>
  </>
}
