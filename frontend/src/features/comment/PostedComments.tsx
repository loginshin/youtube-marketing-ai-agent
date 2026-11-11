import { useState, useEffect } from 'react'

type Receipt = {
  commentId: string; videoTitle: string; videoUrl: string; commentUrl: string
  profileName: string; authorName: string; authorChannelId: string; body: string; publishedAt: string
}
const time = new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', dateStyle: 'medium', timeStyle: 'medium' })

export function PostedComments() {
  const [rows, setRows] = useState<Receipt[] | null>(null)
  const [error, setError] = useState(false)
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      try {
        const response = await fetch('/api/posted-comments', { cache: 'no-store', signal: controller.signal })
        if (!response.ok) throw new Error()
        const data = await response.json()
        if (!controller.signal.aborted) { setRows(data); setError(false) }
      } catch { if (!controller.signal.aborted) setError(true) }
    }
    void load()
    return () => controller.abort()
  }, [revision])
  return <div className="usage">
    <button onClick={() => setRevision(value => value + 1)}>새로고침</button>
    <section><h2>최근 게시 댓글 · 최대 100건</h2>
      {error ? <p role="alert">게시 이력을 불러오지 못했습니다.</p> : rows === null ? <p>불러오는 중입니다.</p>
        : rows.length === 0 ? <p>게시가 확인된 댓글이 없습니다.</p>
        : rows.map(row => <article key={row.commentId}>
          <h3><a href={row.videoUrl} target="_blank" rel="noreferrer">{row.videoTitle}</a></h3>
          <small>{time.format(new Date(row.publishedAt))} KST · {row.profileName} · <a href={'https://www.youtube.com/channel/' + encodeURIComponent(row.authorChannelId)} target="_blank" rel="noreferrer">{row.authorName}</a></small>
          <blockquote>{row.body}</blockquote>
          <a href={row.commentUrl} target="_blank" rel="noreferrer">YouTube에서 댓글 보기</a>
        </article>)}
    </section>
  </div>
}
