import { useEffect, useRef, useState } from 'react'
import { toPng } from 'html-to-image'

type Props = {
  videoId: string
  title: string
  job?: { stage: string; attempt: string; startedAt?: string; failureReason?: string; analysisType?: string }
}

export function AnalysisProgress({ videoId, title, job }: Props) {
  const panel = useRef<HTMLDivElement>(null)
  const [now, setNow] = useState(() => Date.now())
  const [capture, setCapture] = useState('')
  const [retry, setRetry] = useState(0)
  const [image, setImage] = useState<string | null>(null)
  const running = job?.stage === 'SUMMARY' || job?.stage === 'DRAFT'
  useEffect(() => {
    if (!running) return
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [running])
  useEffect(() => {
    if (job?.stage !== 'FAILED' || !panel.current) return
    let cancelled = false
    const element = panel.current
    const endpoint = `/api/analysis/videos/${videoId}/history/${job.attempt}/screenshot`
    async function save() {
      setCapture('실패 화면을 저장하고 있습니다.')
      try {
        const existing = await fetch(endpoint)
        if (!existing.ok) throw new Error()
        let { image: stored } = await existing.json()
        if (!stored) {
          stored = await toPng(element, { pixelRatio: 1, backgroundColor: '#191c27', skipFonts: true })
          const response = await fetch(endpoint, {
            method: 'PUT', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ image: stored }),
          })
          if (!response.ok) throw new Error()
        }
        if (!cancelled) { setImage(stored); setCapture('실패 화면을 저장했습니다.') }
      } catch {
        if (!cancelled) setCapture('화면 저장에 실패했습니다.')
      }
    }
    void save()
    return () => { cancelled = true }
  }, [videoId, job?.attempt, job?.stage, retry])
  if (!job) return null
  const elapsed = job.startedAt ? Math.max(0, Math.floor((now - Date.parse(job.startedAt)) / 1000)) : null
  return (
    <>
      <div ref={panel} className="analysis-progress">
        <strong>{title}</strong>
        <p>{job.analysisType === 'VIDEO' ? '영상 화면·음성 분석' : '콘텐츠 분석'}</p>
        {running && <><progress aria-label="분석 진행 중" /><p>
          {job.stage === 'SUMMARY' ? '1/2 · AI 분석 응답 대기' : '2/2 · 댓글 초안 생성'}
          {elapsed !== null && ` · ${elapsed}초 경과`}
        </p><small>AI 응답 대기 중에는 처리 비율을 확인할 수 없습니다.</small></>}
        {job.stage === 'READY' && <p role="status">분석과 초안 생성 완료</p>}
        {job.stage === 'INSUFFICIENT' && <p role="status">분석 완료 · 고민을 확인할 근거가 부족합니다.</p>}
        {job.stage === 'FAILED' && <p role="alert">{job.failureReason || '이전 작업이 실패했습니다. 저장된 상세 사유가 없습니다.'}</p>}
      </div>
      {job.stage === 'FAILED' && <div>
        <p role="status">{capture}</p>
        {image && <a href={image} download={`analysis-${job.attempt}.png`}>실패 화면 다운로드</a>}
        {capture === '화면 저장에 실패했습니다.' && <button onClick={() => setRetry(value => value + 1)}>화면 저장 다시 시도</button>}
      </div>}
    </>
  )
}
