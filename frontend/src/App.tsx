import { YouTubeWorkspace } from './features/video/YouTubeWorkspace'
import { JobPanel } from './features/comment/JobPanel'
import { DraftEditor } from './features/analysis/DraftEditor'
import { videos } from './features/video/examples'
import { VideoList } from './features/video/VideoList'
import { useEffect, useState } from 'react'

export default function App() {
  const [source, setSource] = useState('example')
  const [listState, setListState] = useState('ready')
  const [selected, select] = useState(videos[0].id)
  const [edits, setEdits] = useState<Record<string, string>>({})
  const video = videos.find(v => v.id === selected)!
  const [message, setMessage] = useState('서버 연결을 확인하고 있습니다.')

  useEffect(() => {
    const controller = new AbortController()
    async function checkHealth() {
      try {
        const response = await fetch('/actuator/health', { signal: controller.signal })
        if (!response.ok || (await response.json()).status !== 'UP') {
          throw new Error('Backend unavailable')
        }
        setMessage('서버 연결 정상')
      } catch {
        if (!controller.signal.aborted) {
          setMessage('서버에 연결할 수 없습니다. 백엔드 실행 후 새로고침하세요.')
        }
      }
    }
    void checkHealth()
    return () => controller.abort()
  }, [])

  return <main>
    <header><h1>댓글 작업실</h1><span>{source === 'example' ? '예시 데이터' : 'YouTube 실제 조회'}</span></header><label>데이터 소스 <select value={source} onChange={event => setSource(event.target.value)}><option value="example">예시 영상</option><option value="youtube">YouTube 검색</option></select></label>{source === 'youtube' ? <YouTubeWorkspace /> : <>
    <label className="scenario">목록 예시 <select value={listState} onChange={e => setListState(e.target.value)}>
      <option value="ready">영상 있음</option><option value="empty">빈 목록</option>
      <option value="loading">불러오는 중</option><option value="error">조회 오류</option>
    </select></label>
    {listState !== 'ready' && <section aria-live="polite">
      <h2>영상 목록</h2><p>{listState === 'empty' ? '조회된 영상이 없습니다.' : listState === 'loading' ? '영상 목록을 불러오는 중입니다.' : '영상 목록을 가져오지 못했습니다.'}</p>
      {listState !== 'loading' && <button onClick={() => setListState('ready')}>다시 불러오기</button>}
    </section>}
    <div className="dashboard" hidden={listState !== 'ready'}>
      <section><h2>영상 목록</h2><VideoList selected={selected} onSelect={select} /></section>
      <section><h2>분석과 초안</h2><DraftEditor key={selected} video={video} draft={edits[selected] ?? video.draft} onEdit={text => setEdits(previous => ({ ...previous, [selected]: text }))} /></section>
      <section><h2>작업 상태</h2><JobPanel video={video} draft={edits[selected] ?? video.draft} /></section>
    </div>
    </>}<footer>{message}</footer>
  </main>
}
