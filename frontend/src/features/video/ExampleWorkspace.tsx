import { useState } from 'react'
import { JobPanel } from '../comment/JobPanel'
import { DraftEditor } from '../analysis/DraftEditor'
import { videos } from './examples'
import { VideoList } from './VideoList'

export function ExampleWorkspace({ active }: { active: boolean }) {
  const [listState, setListState] = useState('ready')
  const [selected, select] = useState(videos[0].id)
  const [edits, setEdits] = useState<Record<string, string>>({})
  const [evidenceById, setEvidenceById] = useState<Record<string, string>>({})
  const video = videos.find((v) => v.id === selected)!
  if (!active) return null
  return (
    <>
      <label className="scenario">
        목록 예시{' '}
        <select
          value={listState}
          onChange={(e) => setListState(e.target.value)}
        >
          <option value="ready">영상 있음</option>
          <option value="empty">빈 목록</option>
          <option value="loading">불러오는 중</option>
          <option value="error">조회 오류</option>
        </select>
      </label>
      {listState !== 'ready' && (
        <section aria-live="polite">
          <h2>영상 목록</h2>
          <p>
            {listState === 'empty'
              ? '조회된 영상이 없습니다.'
              : listState === 'loading'
                ? '영상 목록을 불러오는 중입니다.'
                : '영상 목록을 가져오지 못했습니다.'}
          </p>
          {listState !== 'loading' && (
            <button onClick={() => setListState('ready')}>다시 불러오기</button>
          )}
        </section>
      )}
      <div className="dashboard" hidden={listState !== 'ready'}>
        <section>
          <h2>영상 목록</h2>
          <VideoList selected={selected} onSelect={select} />
        </section>
        <section>
          <h2>분석과 초안</h2>
          <DraftEditor
            key={selected}
            video={video}
            evidence={evidenceById[selected] ?? video.evidence}
            onEvidenceChange={(text) =>
              setEvidenceById((previous) => ({ ...previous, [selected]: text }))
            }
            draft={edits[selected] ?? video.draft}
            onEdit={(text) =>
              setEdits((previous) => ({ ...previous, [selected]: text }))
            }
          />
        </section>
        <section>
          <h2>작업 상태</h2>
          <JobPanel video={video} draft={edits[selected] ?? video.draft} />
        </section>
      </div>
    </>
  )
}
