import { useEffect, useState } from 'react'
import type { VideoContext } from './analysisApi'

type Stage = 'SUMMARY' | 'DRAFT' | 'READY' | 'FAILED' | 'INSUFFICIENT'
type Snapshot = { startedAt?: string; failureReason?: string; analysisType?: string; stage: Stage; attempt: string; concern: string; evidence: string; draft: string }
type Edits = { attempt: string; evidence?: string; draft?: string }
const messages: Record<Stage, string> = {
  SUMMARY: '서버에서 AI 분석을 진행하고 있습니다.',
  DRAFT: '서버에서 댓글 초안을 생성하고 있습니다.',
  READY: '저장된 초안입니다. 근거와 본문을 검토해주세요.',
  FAILED: '생성이 중단되었습니다. 저장된 근거와 초안을 확인하고 다시 시도하세요.',
  INSUFFICIENT: '확인한 영상 근거를 입력해 초안을 생성하세요.',
}

function savedEdits(id: string): Edits | null {
  try { return JSON.parse(localStorage.getItem('clipdesk.edits.' + id) ?? 'null') }
  catch { return null }
}

export function useVideoAnalysis(video: VideoContext | null) {
  const [result, setResult] = useState<{ id: string; job: Snapshot; edits: Edits | null } | null>(null)
  const [action, setAction] = useState<{ id: string; attempt?: string; evidence?: string; analyzeVideo?: boolean } | null>(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  useEffect(() => {
    if (!video) return
    const controller = new AbortController()
    const signal = controller.signal
    let timer: ReturnType<typeof setTimeout>
    const url = '/api/analysis/videos/' + encodeURIComponent(video.id)
    async function load(first: boolean) {
      if (!video) return
      const retry = first && action?.id === video.id && action.attempt
      try {
        const input = { title: video.title, description: video.description, comments: video.comments, analyzeVideo: !!action?.analyzeVideo }
        const response = await fetch(retry ? url + '/retry' : url, {
          method: first ? 'POST' : 'GET',
          headers: first ? { 'Content-Type': 'application/json' } : undefined,
          body: first ? JSON.stringify(retry ? { input, attempt: action?.attempt, evidence: action?.evidence } : input) : undefined,
          signal: AbortSignal.any([signal, AbortSignal.timeout(15000)]),
          cache: 'no-store',
        })
        if (!response.ok) {
          const failure = await response.json().catch(() => null)
          throw new Error(failure?.message || "분석 요청에 실패했습니다.")
        }
        const job: Snapshot = await response.json()
        if (signal.aborted) return
        setSubmitting(false)
        setError('')
        const edits = savedEdits(video.id)
        setResult({ id: video.id, job, edits: edits?.attempt === job.attempt || job.stage === 'FAILED' ? edits : null })
        if (job.stage === 'SUMMARY' || job.stage === 'DRAFT') timer = setTimeout(() => void load(false), 1100)
      } catch (error) {
        if (!signal.aborted) setSubmitting(false)
        if (!signal.aborted) setError(error instanceof Error ? error.message : '작업 상태를 확인하지 못했습니다. 다시 연결하세요.')
      }
    }
    void load(true)
    return () => { controller.abort(); clearTimeout(timer) }
  }, [video, action])

  const current = result?.id === video?.id ? result : null
  const job = current?.job
  function edit(changes: Partial<Pick<Edits, 'draft' | 'evidence'>>) {
    if (!current || !job) return
    const edits = { ...current.edits, ...changes, attempt: job.attempt }
    try { localStorage.setItem('clipdesk.edits.' + current.id, JSON.stringify(edits)) }
    catch { setError('브라우저에 수정본을 저장할 공간이 부족합니다.') }
    setResult({ ...current, edits })
  }
  const busy = submitting || !job || job.stage === 'SUMMARY' || job.stage === 'DRAFT'
  const evidence = current?.edits?.evidence ?? job?.evidence ?? ''
  return {
    job, busy, concern: job?.concern ?? '', evidence, draft: current?.edits?.draft ?? job?.draft ?? '',
    message: error || (job ? messages[job.stage] : '저장된 작업을 확인하고 있습니다.'),
    reconnect: error && video ? () => { setError(''); setAction({ id: video.id }) } : null,
    retrySummary: job?.stage === 'FAILED' && !evidence.trim(),
    regenerate: () => {
      if (video && job && !busy) {
        setSubmitting(true)
        setError('')
        setAction({ id: video.id, attempt: job.attempt, evidence })
      }
    },
    analyze: (analyzeVideo: boolean) => {
      if (video && job && !busy) {
        setSubmitting(true)
        setError("")
        setAction({ id: video.id, attempt: job.attempt, evidence: "", analyzeVideo })
      }
    },
    edit,
  }
}
