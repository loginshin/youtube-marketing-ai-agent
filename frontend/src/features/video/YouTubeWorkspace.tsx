import { ApprovalPanel } from '../comment/ApprovalPanel'
import { useVideoAnalysis } from '../analysis/useVideoAnalysis'
import type { Detail } from './useVideoSearch'
import { useState } from 'react'
import { DraftEditor } from '../analysis/DraftEditor'
import { VideoList } from './VideoList'
import type { Video } from './types'

import { useVideoSearch } from './useVideoSearch'

export function YouTubeWorkspace() {

  const { query, setQuery, items, detail, busy, message, load, hasMore, loadMore } =
    useVideoSearch()
  const [format, setFormat] = useState('all')
  const visibleItems = items.filter(({ durationSeconds }) => {
    if (format === 'all') return true
    if (durationSeconds == null) return false
    return format === 'short' ? durationSeconds <= 180 : durationSeconds > 180
  })
  const selected = detail && visibleItems.some(item => item.id === detail.id) ? detail : null

  return (
    <>
      <form
        className="video-search"
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
      <div className={selected ? 'dashboard' : 'results-only'}>
        <section>
          <div className="list-heading">
            <h2>영상 목록</h2>
            {items.length > 0 && <small>{visibleItems.length}개</small>}
          </div>
          <div className="length-filter" role="group" aria-label="영상 길이">
            {Object.entries({ all: '전체', short: '쇼츠 후보', long: '롱폼' }).map(([value, label]) => (
              <button key={value} type="button" aria-pressed={format === value}
                onClick={() => setFormat(value)}>{label}</button>
            ))}
          </div>
          {format === 'short' && <p>3분 이하 · 짧은 일반 영상도 포함됩니다.</p>}
          {format === 'long' && <p>3분 초과 영상을 표시합니다.</p>}
          {items.length > 0 && visibleItems.length === 0 && (
            <p role="status">현재 목록에 해당 길이의 영상이 없습니다.</p>
          )}
          {items.length === 0 && (
            <div className="empty-result">
              <span aria-hidden="true">▷</span>
              <p role="status">{message}</p>
            </div>
          )}
          <VideoList
            items={visibleItems}
            selected={detail?.id ?? ''}
            disabled={busy}
            onSelect={(id) => void load(id)}
          />
          {hasMore && (
            <button type="button" disabled={busy} onClick={() => void loadMore()}>
              다음 결과 불러오기
            </button>
          )}
        </section>
        {selected && <SelectedVideo key={selected.id} detail={selected} />}
      </div>
    </>
  )
}

function SelectedVideo({ detail }: { detail: Detail }) {
  const analysis = useVideoAnalysis(detail)
  const video: Video = {
    ...detail, art: '▶', color: '#365862',
    concern: analysis.concern, evidence: analysis.evidence, draft: analysis.draft,
  }

  return (
    <>
      <section>
        <h2>분석과 초안</h2>
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
        <p role="status">{analysis.message}</p>
        {analysis.reconnect && <button onClick={analysis.reconnect}>작업 다시 연결</button>}
        {analysis.retrySummary && (
          <button onClick={analysis.regenerate}>영상 분석 다시 시도</button>
        )}
        {analysis.busy && analysis.concern && <p>{analysis.concern}</p>}
        {!analysis.busy && (
          <DraftEditor
            video={video}
            evidence={video.evidence}
            onEvidenceChange={(text) => analysis.edit({ evidence: text })}
                draft={video.draft}
            onEdit={(text) => analysis.edit({ draft: text })}
            onGenerate={analysis.regenerate}
            actualVideo
          />
        )}
      </section>
      <section>
        <h2>작업 상태</h2>
        <ApprovalPanel
          videoId={video.id}
          title={video.title}
          draft={video.draft}
        />
      </section>
    </>
  )
}
