import { useEffect, useRef, useState } from 'react'
import type { Video } from './types'


type Summary = Pick<Video, 'id' | 'title' | 'channel' | 'date' | 'thumbnailUrl' | 'durationSeconds'>
const koreanDate = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  dateStyle: 'long',
})
export type Detail = Summary & {
  description: string
  comments: string[]
  warning: string | null
}

type Filters = { order: string; days: string }
const defaults = { order: "date", days: "0" }
type Workspace = { filters?: Filters; query: string; items: Video[]; detail: Detail }

function restore(): Workspace | null {
  try { return JSON.parse(sessionStorage.getItem('clipdesk.workspace') ?? 'null') }
  catch { return null }
}

export function useVideoSearch() {
  const [restored] = useState(restore)
  const [query, setQuery] = useState(restored?.query ?? '연애')
  const [filters, setFilters] = useState(restored?.filters ?? defaults)
  const [applied, setApplied] = useState(restored?.filters ?? defaults)
  const [items, setItems] = useState<Video[]>(restored?.items ?? [])
  const [nextPage, setNextPage] = useState<{ token: string; query: string; after?: string } | null>(null)
  const [detail, setDetail] = useState<Detail | null>(restored?.detail ?? null)
  const [selectedId, setSelectedId] = useState(restored?.detail?.id ?? "")
  const [detailBusy, setDetailBusy] = useState(false)
  const [detailMessage, setDetailMessage] = useState("")
  const detailRequest = useRef<AbortController | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(
    restored ? '이전 영상 작업을 복원했습니다.' : '검색 버튼을 눌러 영상을 불러오세요.',
  )
  const request = useRef<AbortController | null>(null)
  useEffect(() => () => {
    request.current?.abort()
    detailRequest.current?.abort()
  }, [])

  function changeFilters(next: Filters) {
    setFilters(next)
    if (query.trim()) void load(undefined, false, next)
  }

  async function selectVideo(id: string) {
    detailRequest.current?.abort()
    const controller = new AbortController()
    detailRequest.current = controller
    setSelectedId(id)
    setDetail(null)
    setDetailBusy(true)
    setDetailMessage("영상 설명과 댓글을 불러오는 중입니다.")
    sessionStorage.removeItem('clipdesk.workspace')
    try {
      const response = await fetch('/api/videos/' + encodeURIComponent(id), {
        signal: AbortSignal.any([controller.signal, AbortSignal.timeout(45000)]),
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.message || "영상 조회에 실패했습니다.")
      if (controller.signal.aborted) return
      setDetail(data as Detail)
      setDetailMessage("")
      try {
        sessionStorage.setItem('clipdesk.workspace', JSON.stringify({ query, filters: applied, items, detail: data }))
      } catch { setDetailMessage("브라우저에 마지막 영상 위치를 저장하지 못했습니다.") }
    } catch (error) {
      if (!controller.signal.aborted) setDetailMessage(error instanceof Error && error.name !== 'TimeoutError'
        ? error.message : "요청 시간이 초과되었습니다. 다시 시도해주세요.")
    } finally {
      if (detailRequest.current === controller) {
        setDetailBusy(false)
        detailRequest.current = null
      }
    }
  }

  async function load(id?: string, append = false, criteria = filters) {
    if (id) return selectVideo(id)
    if (request.current) return
    if (append && !nextPage) return
    const searchQuery = append && nextPage ? nextPage.query : query.trim()
    const params = new URLSearchParams({ query: searchQuery, order: criteria.order })
    const after = append ? nextPage?.after : Number(criteria.days) > 0
      ? new Date(Date.now() - Number(criteria.days) * 86400000).toISOString() : undefined
    if (after) params.set("publishedAfter", after)
    if (append && nextPage) params.set('pageToken', nextPage.token)
    const controller = new AbortController()
    request.current = controller
    setBusy(true)
    if (!append) setDetail(null)
    if (!append) {
      detailRequest.current?.abort()
      detailRequest.current = null
      setDetailBusy(false)
      setDetailMessage("")
      setSelectedId("")
      setApplied(criteria)
      setItems([])
      setNextPage(null)
      sessionStorage.removeItem('clipdesk.workspace')
    }
    setMessage(
      id
        ? '영상 설명과 댓글을 불러오는 중입니다.'
        : '영상을 검색하고 있습니다.',
    )
    try {
      const response = await fetch(
        id
          ? `/api/videos/${encodeURIComponent(id)}`
          : `/api/videos?${params}`,
        {
          signal: AbortSignal.any([
            controller.signal,
            AbortSignal.timeout(45000),
          ]),
        },
      )
      const data = await response.json()
      if (!response.ok)
        throw new Error(
          data.message ??
            (response.status === 404
              ? '영상을 찾을 수 없습니다.'
              : '영상 조회에 실패했습니다.'),
        )
      if (controller.signal.aborted) return
        const results = data as Summary[]
        const videos = results.map((item) => ({
          ...item,
          date: koreanDate.format(new Date(item.date)),
          art: '▶',
          color: '#365862',
          concern: '',
          evidence: '',
          draft: '',
        }))
        setItems((previous) => {
          const combined = append ? [...previous, ...videos] : videos
          return [...new Map(combined.map((video) => [video.id, video])).values()]
        })
        const token = response.headers.get('X-Next-Page-Token')
        setNextPage(token ? { token, query: searchQuery, after } : null)
        setMessage(
          results.length
            ? '분석할 영상을 선택하세요.'
            : '조회된 영상이 없습니다. 다른 검색어로 시도하세요.',
        )
    } catch (error) {
      if (!controller.signal.aborted)
        setMessage(
          error instanceof Error && error.name !== 'TimeoutError'
            ? error.message
            : '요청 시간이 초과되었습니다. 다시 시도해주세요.',
        )
    } finally {
      if (!controller.signal.aborted) setBusy(false)
      request.current = null
    }
  }

  return { selectedId, detailBusy, detailMessage, filters, changeFilters, query, setQuery, items, detail, busy, message, load,
    hasMore: nextPage !== null && nextPage.query === query.trim() && filters.order === applied.order && filters.days === applied.days,
    loadMore: () => load(undefined, true) }
}
