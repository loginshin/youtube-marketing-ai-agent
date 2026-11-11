export type VideoContext = {
  id: string
  title: string
  description: string
  comments: string[]
}

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

export async function generateDraft(title: string, evidence: string, signal: AbortSignal): Promise<string> {
  const result = await request('draft', { title, evidence }, signal)
  if (typeof result.draft !== 'string' || !result.draft.trim()) throw new Error('댓글 초안을 확인할 수 없습니다.')
  return result.draft
}
