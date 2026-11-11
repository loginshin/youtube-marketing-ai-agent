import { useEffect, useRef, useState } from 'react'
import type { Video } from './types'
import type { VideoContext } from '../analysis/analysisApi'

type Summary = Pick<Video, 'id' | 'title' | 'channel' | 'date' | 'thumbnailUrl' | 'durationSeconds'>
const koreanDate = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  dateStyle: 'long',
})
type Detail = Summary & {
  description: string
  comments: string[]
  warning: string | null
}

export function useVideoSearch(onSelect: (video: VideoContext) => void) {
  const [query, setQuery] = useState('연애')
  const [items, setItems] = useState<Video[]>([])
  const [nextPage, setNextPage] = useState<{ token: string; query: string } | null>(null)
  const [detail, setDetail] = useState<Detail | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState(
    '검색 버튼을 눌러 최신 영상을 불러오세요.',
  )
  const request = useRef<AbortController | null>(null)
  useEffect(() => () => request.current?.abort(), [])

  async function load(id?: string, append = false) {
    if (request.current) return
    if (append && !nextPage) return
    const searchQuery = append && nextPage ? nextPage.query : query.trim()
    const params = new URLSearchParams({ query: searchQuery })
    if (append && nextPage) params.set('pageToken', nextPage.token)
    const controller = new AbortController()
    request.current = controller
    setBusy(true)
    if (!append) setDetail(null)
    if (!id && !append) {
      setItems([])
      setNextPage(null)
    }
    setMessage(
      id
        ? '영상 설명과 댓글을 불러오는 중입니다.'
        : '최신 영상을 검색하고 있습니다.',
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
      if (id) {
        const selected = data as Detail
        setDetail(selected)
        onSelect(selected)
        setMessage('영상 설명과 공개 댓글을 불러왔습니다.')
      } else {
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
        setNextPage(token ? { token, query: searchQuery } : null)
        setMessage(
          results.length
            ? '분석할 영상을 선택하세요.'
            : '조회된 영상이 없습니다. 다른 검색어로 시도하세요.',
        )
      }
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

  return { query, setQuery, items, detail, busy, message, load,
    hasMore: nextPage !== null && nextPage.query === query.trim(),
    loadMore: () => load(undefined, true) }
}
