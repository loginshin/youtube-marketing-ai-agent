import { useEffect, useRef, useState } from 'react'
import { generateDraft, summarizeVideo, type VideoContext } from './analysisApi'

type Analysis = {
  busy: boolean
  message: string
  concern: string
  evidence: string
  draft: string
  summaryFailed: boolean
}

const initial: Analysis = {
  busy: true, message: '영상 설명과 공개 댓글을 분석하고 있습니다.',
  concern: '', evidence: '', draft: '', summaryFailed: false,
}

export function useVideoAnalysis() {
  const [entries, setEntries] = useState<Record<string, Analysis>>({})
  const started = useRef(new Set<string>())
  const requests = useRef(new Set<AbortController>())
  useEffect(() => {
    const pending = requests.current
    return () => pending.forEach(request => request.abort())
  }, [])

  function update(id: string, changes: Partial<Analysis>) {
    setEntries(previous => ({ ...previous, [id]: { ...previous[id], ...changes } }))
  }

  async function analyze(video: VideoContext) {
    if (started.current.has(video.id)) return
    started.current.add(video.id)
    const controller = new AbortController()
    requests.current.add(controller)
    update(video.id, initial)
    let summarized = false
    try {
      const summary = await summarizeVideo(video, controller.signal)
      summarized = true
      if (summary.status === 'insufficient') {
        update(video.id, { busy: false, message: '자료가 부족합니다. 확인한 영상 근거를 입력해 초안을 생성하세요.' })
        return
      }
      const evidence = summary.source + ' · ' + summary.quote
      update(video.id, { concern: summary.summary, evidence, message: '근거를 바탕으로 댓글 초안을 생성하고 있습니다.' })
      const draft = await generateDraft(video.title, `고민 요약: ${summary.summary}\n출처와 인용: ${evidence}`, controller.signal)
      update(video.id, { busy: false, draft, message: '초안을 생성했습니다. 근거와 본문을 검토해주세요.' })
    } catch (error) {
      if (controller.signal.aborted) return
      if (!summarized) started.current.delete(video.id)
      update(video.id, { busy: false, summaryFailed: !summarized,
        message: error instanceof Error && error.name !== 'TimeoutError' ? error.message : '요청 시간이 초과되었습니다.' })
    } finally {
      requests.current.delete(controller)
    }
  }

  function edit(id: string, changes: Partial<Analysis>) {
    started.current.add(id)
    update(id, { ...changes, message: '', summaryFailed: false })
  }

  return { entries, analyze, edit }
}
