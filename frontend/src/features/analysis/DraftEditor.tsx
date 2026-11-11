import { useEffect, useId, useRef, useState } from 'react'
import { generateDraft } from './analysisApi'
import type { Video } from '../video/examples'

export function DraftEditor({
  video,
  draft,
  onEdit,
  evidence,
  onEvidenceChange,
  actualVideo = false,
  onGenerate,
}: {
  video: Video
  draft: string
  onEdit: (text: string) => void
  evidence: string
  onEvidenceChange: (text: string) => void
  onGenerate?: () => void
  actualVideo?: boolean
}) {
  const editorId = useId()
  const evidenceId = editorId + '-evidence'
  const draftId = editorId + '-draft'
  const lengthId = editorId + '-length'
  const length = Array.from(draft.trim()).length
  const valid = length >= 40 && length <= 120
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
      const draft = await generateDraft(video.title, evidence.trim(), controller.signal)
      if (controller.signal.aborted) return
      onEdit(draft)
      setMessage('AI 초안이 생성되었습니다. 근거와 본문을 검토해주세요.')
    } catch (error) {
      setMessage(
        controller.signal.aborted
          ? '요청이 취소되거나 시간이 초과되었습니다. 다시 시도해주세요.'
          : error instanceof Error
            ? error.message
            : 'AI 요청에 실패했습니다.',
      )
    } finally {
      window.clearTimeout(timeout)
      request.current = null
      setPending(false)
    }
  }

  return (
    <div className="draft-editor">
      <header>
        <h3>{video.title}</h3>
        <p>{video.concern}</p>
      </header>
      <div className="editor-field">
        <label htmlFor={evidenceId}>분석 근거와 출처</label>
        <textarea
          id={evidenceId}
          value={evidence}
          maxLength={10000}
          disabled={pending}
          onChange={(event) => onEvidenceChange(event.target.value)}
          rows={3}
        />
        <p>
          {actualVideo
            ? '직접 분석 근거와 출처를 입력하면 AI 초안을 생성할 수 있습니다. 자막은 자동 수집하지 않습니다.'
            : '예시 영상의 근거를 사용해 실제 AI 초안을 생성합니다.'}
        </p>
        <button
          disabled={pending || !evidence.trim()}
          onClick={onGenerate ?? (() => void generate())}
        >
          {pending ? '생성 중…' : 'AI 초안 생성·재생성'}
        </button>
        <p role="status">{message}</p>
      </div>
      <div className="editor-field">
        <label htmlFor={draftId}>댓글 초안</label>
        <textarea
          id={draftId}
          value={draft}
          disabled={pending}
          onChange={(e) => onEdit(e.target.value)}
          rows={5}
          aria-invalid={!valid}
          aria-describedby={lengthId}
        />
        <small id={lengthId} aria-live="polite">
          {length}자 · {valid ? '글자 수 충족' : '40~120자로 작성하세요'}
        </small>
      </div>
    </div>
  )
}
