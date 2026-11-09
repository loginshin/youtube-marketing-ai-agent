import type { Video } from '../video/examples'

export function DraftEditor({ video, draft, onEdit }: { video: Video; draft: string; onEdit: (text: string) => void }) {
  const length = Array.from(draft.trim()).length
  const valid = length >= 40 && length <= 120
  return <>
    <h3>{video.title}</h3><p>{video.concern}</p>
    <blockquote>{video.evidence}</blockquote>
    <label htmlFor="draft">댓글 초안</label>
    <textarea id="draft" value={draft} onChange={e => onEdit(e.target.value)} rows={5} aria-invalid={!valid} aria-describedby="length" />
    <small id="length" aria-live="polite">{length}자 · {valid ? '글자 수 충족' : '40~120자로 작성하세요'}</small>
  </>
}
