import { useState } from 'react'
import type { Video } from '../video/examples'

const outcomes: Record<string, string> = {
  letters: '완료: 내 댓글을 확인했습니다.',
  distance: '실패: 댓글 입력창을 열 수 없습니다.',
  timing: '확인 대기: 게시 결과를 확인할 수 없어 재게시를 보류합니다.',
}

type Entry = { body: string; time: string }

export function JobPanel({ video, draft }: { video: Video; draft: string }) {
  const [jobs, setJobs] = useState<Record<string, Entry>>({})
  const entry = jobs[video.id]
  const length = Array.from(draft.trim()).length
  return <>
    <p role="status">{entry ? outcomes[video.id] : '승인 전'}</p>
    <button disabled={!!entry || length < 40 || length > 120} onClick={() => setJobs(previous => ({
      ...previous, [video.id]: { body: draft, time: new Date().toLocaleTimeString('ko-KR') },
    }))}>예시 작업 승인</button>
    <h3>작업 이력</h3>
    {entry ? <ol><li>{entry.time} · 초안 승인 <blockquote>{entry.body}</blockquote></li>
      <li>{entry.time} · {outcomes[video.id]}</li></ol> : <p>아직 기록된 작업이 없습니다.</p>}
  </>
}
