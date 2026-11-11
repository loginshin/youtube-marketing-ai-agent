import { useEffect, useState } from 'react'

type Settings = {
  instructions: string
  version: number
  modelTier: string
  models: { tier: string; label: string; modelId: string }[]
}
const endpoint = '/api/analysis/draft/prompt'

export function PromptSettings() {
  const [saved, setSaved] = useState<Settings | null>(null)
  const [text, setText] = useState('')
  const [modelTier, setModelTier] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(true)
  const [reload, setReload] = useState(0)
  const [conflict, setConflict] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    async function read() {
      setBusy(true)
      try {
        const response = await fetch(endpoint, {
          cache: 'no-store',
          signal: controller.signal,
        })
        if (!response.ok) throw new Error()
        const value: Settings = await response.json()
        if (controller.signal.aborted) return
        setSaved(value)
        setText(value.instructions)
        setModelTier(value.modelTier)
        setConflict(false)
        setMessage('')
      } catch {
        if (!controller.signal.aborted)
          setMessage('작성 지침을 불러오지 못했습니다.')
      } finally {
        if (!controller.signal.aborted) setBusy(false)
      }
    }
    void read()
    return () => controller.abort()
  }, [reload])

  async function save() {
    if (!saved || busy) return
    setBusy(true)
    try {
      const response = await fetch(endpoint, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ instructions: text, modelTier, version: saved.version }),
      })
      if (response.status === 409) {
        setConflict(true)
        setMessage(
          '다른 화면에서 먼저 저장했습니다. 필요한 문구를 복사한 뒤 최신 지침을 불러오세요.',
        )
        return
      }
      if (!response.ok) throw new Error()
      const value: Settings = await response.json()
      setSaved(value)
      setText(value.instructions)
      setModelTier(value.modelTier)
      setMessage('저장했습니다. 다음 영상 분석과 초안 생성부터 적용됩니다.')
    } catch {
      setMessage('저장 결과를 확인하지 못했습니다. 다시 시도해주세요.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="prompt-settings">
      <div className="editor-field">
        <label htmlFor="ai-model">OpenAI 모델</label>
        <select
          id="ai-model"
          value={modelTier}
          disabled={busy || !saved}
          onChange={(event) => {
            setModelTier(event.target.value)
            if (!conflict) setMessage('')
          }}
        >
          {saved?.models.map(({ tier, label, modelId }) => (
            <option key={tier} value={tier}>{label} · {modelId}</option>
          ))}
        </select>
      </div>
      <div className="editor-field">
        <label htmlFor="prompt-instructions">댓글 초안 작성 지침</label>
        <p>
          말투, 강조할 내용, 브랜드 표현을 입력하세요. 영상 근거와 40~120자 작성
          기준은 별도로 적용됩니다.
        </p>
        <textarea
          id="prompt-instructions"
          rows={8}
          maxLength={4000}
          value={text}
          disabled={busy || !saved}
          onChange={(event) => {
            setText(event.target.value)
            if (!conflict) setMessage('')
          }}
        />
        <small>{text.length} / 4000자</small>
      </div>
      <footer className="settings-actions">
        <p role="status">{message}</p>
        <button
          onClick={() => void save()}
          disabled={
            busy ||
            conflict ||
            !saved ||
            !text.trim() ||
            (text.trim() === saved.instructions && modelTier === saved.modelTier)
          }
        >
          설정 저장
        </button>
        {(!saved || conflict) && !busy && (
          <button onClick={() => setReload((n) => n + 1)}>
            최신 지침 불러오기
          </button>
        )}
      </footer>
    </section>
  )
}
