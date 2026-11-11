export type VideoContext = {
  id: string
  title: string
  description: string
  comments: string[]
}

type Summary =
  | { status: 'insufficient' }
  | { status: 'ready'; summary: string; source: string; quote: string }

async function request(path: string, body: object, signal: AbortSignal) {
  const response = await fetch('/api/analysis/' + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
    signal: AbortSignal.any([signal, AbortSignal.timeout(75000)]),
  })
  const data = await response.json().catch(() => null)
  if (!response.ok || !data) throw new Error(data?.message || 'AI 요청에 실패했습니다.')
  return data
}

export async function summarizeVideo({ title, description, comments }: VideoContext, signal: AbortSignal): Promise<Summary> {
  const result = await request('summary', { title, description, comments }, signal)
  if (result.status === 'insufficient' || (result.status === 'ready' && result.quote && result.source && result.summary)) return result
  throw new Error('영상 요약 형식을 확인할 수 없습니다.')
}

export async function generateDraft(title: string, evidence: string, signal: AbortSignal): Promise<string> {
  const result = await request('draft', { title, evidence }, signal)
  if (typeof result.draft !== 'string' || !result.draft.trim()) throw new Error('댓글 초안을 확인할 수 없습니다.')
  return result.draft
}
