import { ApprovalPanel } from '../comment/ApprovalPanel'
import { VideoSummaryPanel } from '../analysis/VideoSummaryPanel'
import { useState } from 'react'
import { DraftEditor } from '../analysis/DraftEditor'
import { VideoList } from './VideoList'
import type { Video } from './types'

import { useVideoSearch } from './useVideoSearch'

export function YouTubeWorkspace() {
  const { query, setQuery, items, detail, busy, message, load } =
    useVideoSearch()
  const [drafts, setDrafts] = useState<Record<string, string>>({})
  const [evidenceById, setEvidenceById] = useState<Record<string, string>>({})
  const video: Video | null = detail
    ? {
        ...detail,
        art: '▶',
        color: '#365862',
        concern:
          '설명과 댓글은 참고 자료입니다. 실제 영상에서 확인한 발언·장면을 근거로 입력해주세요.',
        evidence: '',
        draft: '',
      }
    : null

  return (
    <>
      <form
        onSubmit={(event) => {
          event.preventDefault()
          void load()
        }}
      >
        <label htmlFor="video-query">YouTube 검색어</label>
        <input
          id="video-query"
          value={query}
          maxLength={100}
          disabled={busy}
          onChange={(event) => setQuery(event.target.value)}
        />
        <button disabled={busy || !query.trim()}>
          {busy ? '조회 중…' : '최신 영상 검색'}
        </button>
      </form>
      {items.length > 0 && <p role="status">{message}</p>}
      <div className={video ? 'dashboard' : 'results-only'}>
        <section>
          <h2>영상 목록</h2>
          {items.length === 0 && (
            <div className="empty-result">
              <span aria-hidden="true">▷</span>
              <p role="status">{message}</p>
            </div>
          )}
          <VideoList
            items={items}
            selected={detail?.id ?? ''}
            disabled={busy}
            onSelect={(id) => void load(id)}
          />
        </section>
        <section hidden={!video}>
          <h2>분석과 초안</h2>
          {video && detail ? (
            <>
              <a
                href={`https://www.youtube.com/watch?v=${encodeURIComponent(video.id)}`}
                target="_blank"
                rel="noreferrer"
              >
                YouTube에서 영상 확인
              </a>
              <details>
                <summary>영상 설명과 공개 댓글</summary>
                <h3>영상 설명</h3>
                <p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>
                  {detail.description || '설명이 없습니다.'}
                </p>
                <h3>관련도순 공개 댓글</h3>
                {detail.warning && <p role="status">{detail.warning}</p>}
                {detail.comments.length ? (
                  <ul>
                    {detail.comments.map((comment, index) => (
                      <li key={index}>{comment}</li>
                    ))}
                  </ul>
                ) : (
                  <p>조회 가능한 댓글이 없습니다.</p>
                )}
              </details>
              <VideoSummaryPanel
                key={video.id}
                title={detail.title}
                description={detail.description}
                comments={detail.comments}
              />
              <DraftEditor
                key={video.id}
                video={video}
                evidence={evidenceById[video.id] ?? video.evidence}
                onEvidenceChange={(text) =>
                  setEvidenceById((previous) => ({
                    ...previous,
                    [video.id]: text,
                  }))
                }
                draft={drafts[video.id] ?? ''}
                onEdit={(text) =>
                  setDrafts((previous) => ({ ...previous, [video.id]: text }))
                }
                actualVideo
              />
            </>
          ) : (
            <p>검색 후 영상을 선택해주세요.</p>
          )}
        </section>
        <section hidden={!video}>
          <h2>작업 상태</h2>
          {video ? (
            <ApprovalPanel
              key={video.id}
              videoId={video.id}
              title={video.title}
              draft={drafts[video.id] ?? ''}
            />
          ) : (
            <p>영상을 선택하면 승인 본문과 이력을 확인할 수 있습니다.</p>
          )}
        </section>
      </div>
    </>
  )
}
