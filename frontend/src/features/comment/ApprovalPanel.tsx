import { useEffect, useRef, useState } from 'react'

type Job = {
  profile: string
  body: string
  status: string
  createdAt: string
  events: { status: string; occurredAt: string; failureReason?: string; screenshotPath?: string }[]
}

const labels: Record<string, string> = {
  APPROVED: '승인됨',
  RUNNING: '진행 중',
  COMPLETED: '완료',
  FAILED: '실패',
  CONFIRMATION_PENDING: '확인 대기',
}

type Props = { videoId: string; title: string; draft: string }

export function ApprovalPanel({ videoId, title, draft }: Props) {
  const endpoint = '/api/videos/' + encodeURIComponent(videoId) + '/job'
  const [revision, setRevision] = useState(0)
  const [job, setJob] = useState<Job | null>(null)
  const [phase, setPhase] = useState<'loading' | 'ready' | 'saving' | 'error'>(
    'loading',
  )
  const [message, setMessage] = useState('')
  const saving = useRef(false)
  const [checking, setChecking] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [progress, setProgress] = useState('')
  const [postedUrl, setPostedUrl] = useState('')
  const [commentLink, setCommentLink] = useState('')
  const profile = '유튜브 1'
  const body = draft.trim()
  const length = Array.from(body).length

  useEffect(() => {
    const controller = new AbortController()
    async function read() {
      try {
        const response = await fetch(endpoint, {
          cache: 'no-store',
          signal: AbortSignal.any([
            controller.signal,
            AbortSignal.timeout(15000),
          ]),
        })
        if (!response.ok) throw new Error()
        const stored: Job | null =
          response.status === 204 ? null : await response.json()
        if (!controller.signal.aborted) {
          setJob(stored)
          setPhase('ready')
        }
      } catch {
        if (!controller.signal.aborted) setPhase('error')
      }
    }
    void read()
    return () => controller.abort()
  }, [endpoint, revision])

  useEffect(() => {
    if (!publishing) return
    const controller = new AbortController()
    let timer: ReturnType<typeof setTimeout>
    async function poll() {
      try {
        const response = await fetch(endpoint + '/progress', { signal: controller.signal, cache: 'no-store' })
        if (response.ok && response.status !== 204) {
          const result = await response.json()
          if (!controller.signal.aborted) setProgress(result.message)
        }
      } catch {
        // The publication response supplies the final status.
      } finally {
        if (!controller.signal.aborted) timer = setTimeout(poll, 500)
      }
    }
    void poll()
    return () => { controller.abort(); clearTimeout(timer) }
  }, [endpoint, publishing])

  function refresh() {
    setPhase('loading')
    setRevision((value) => value + 1)
  }

  async function approve() {
    if (phase !== 'ready' || job || saving.current) return
    saving.current = true
    setPhase('saving')
    setMessage('')
    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ profile, body }),
        signal: AbortSignal.timeout(20000),
      })
      if (!response.ok) {
        const data = await response.json().catch(() => null)
        setMessage(data?.message || '승인 요청 결과를 확인해주세요.')
      }
    } catch {
      setMessage('서버 응답을 받지 못해 저장 여부를 다시 확인합니다.')
    } finally {
      saving.current = false
      refresh()
    }
  }

  async function publication(action: 'publish' | 'verify') {
    if (checking || saving.current) return
    saving.current = true
    setChecking(true)
    setProgress('')
    setPublishing(action === 'publish')
    setMessage(action === 'publish' ? '승인 본문을 입력하고 게시 결과를 확인합니다.' : 'YouTube에서 댓글을 확인합니다.')
    try {
      const response = await fetch(endpoint + '/' + action, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(action === 'verify' ? { url: commentLink.trim() } : {}),
      })
      if (!response.ok) {
        setMessage(response.status === 429 ? '오늘의 게시 한도에 도달했습니다.' :
          '결과를 확인할 수 없습니다. 댓글 링크, 영상, 승인 본문과 공개 여부를 확인하세요.')
        return
      }
      const data = await response.json()
      setPostedUrl(data.commentUrl ?? '')
      setMessage(data.message ?? '댓글 작성 계정과 게시 시각을 게시 이력에 저장했습니다.')
    } catch {
      setMessage('서버 연결을 확인한 뒤 작업 이력을 다시 조회하세요.')
    } finally {
      saving.current = false
      setPublishing(false)
      setChecking(false)
      refresh()
    }
  }

  return (
    <>
      <p role="status">{publishing && progress ? progress : message}</p>
      {postedUrl && <a href={postedUrl} target="_blank" rel="noreferrer">게시한 댓글 보기</a>}
      {phase === 'loading' && <p>작업 이력을 확인하고 있습니다.</p>}
      {phase === 'error' && (
        <>
          <p>저장된 작업을 확인할 수 없습니다.</p>
          <button onClick={refresh}>이력 다시 조회</button>
        </>
      )}
      {phase === 'ready' && job && (
        <>
          <h3>{labels[job.status] ?? job.status}</h3>
          <p>
            {job.profile} · {new Date(job.createdAt).toLocaleString('ko-KR')}
          </p>
          <blockquote>{job.body}</blockquote>
          {job.status === 'APPROVED' && job.profile === '유튜브 1' && (
            <div className="publication-actions">
            <button disabled={checking} onClick={() => void publication('publish')}>댓글 달기</button>
            </div>
          )}
          {job.status === 'CONFIRMATION_PENDING' && (
            <details open>
              <summary>게시 결과 확인 대기</summary>
              <div className="editor-field">
                <label>내 댓글 링크
                  <input type="url" placeholder="YouTube 댓글의 작성 시각을 눌러 복사한 링크" value={commentLink}
                    onChange={event => setCommentLink(event.target.value)} />
                </label>
                <button disabled={checking || (!commentLink.trim() && job.status !== 'CONFIRMATION_PENDING')}
                  onClick={() => void publication('verify')}>게시 결과 확인</button>
              </div>
            </details>
          )}
          <h3>작업 이력</h3>
          <ol>
            {job.events.map((event, index) => (
              <li key={index}>
                {new Date(event.occurredAt).toLocaleString('ko-KR')} ·{' '}
                {labels[event.status] ?? event.status}
                {event.failureReason && <p>{event.failureReason}</p>}
                {event.screenshotPath && <a href={`/api/publication-failures/${encodeURIComponent(event.screenshotPath)}`}
                  target="_blank" rel="noreferrer">실패 화면 보기</a>}
              </li>
            ))}
          </ol>
        </>
      )}
      {(phase === 'ready' || phase === 'saving') && !job && (
        <>
          <h3>최종 승인</h3>
          <p>
            {title} · {profile}
          </p>
          <blockquote>{body || '댓글 초안을 작성해주세요.'}</blockquote>
          <button
            disabled={phase === 'saving' || length < 40 || length > 120}
            onClick={() => void approve()}
          >
            {phase === 'saving' ? '저장 중…' : '이 본문으로 승인 저장'}
          </button>
        </>
      )}
    </>
  )
}
