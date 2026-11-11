import { useEffect, useState } from 'react'
import './usage.css'

type Summary = {
  provider: string
  operation: string
  calls: number
  measuredCalls: number
  inputTokens: number
  outputTokens: number
}

const number = new Intl.NumberFormat('ko-KR')

export function UsageDashboard() {
  const [days, setDays] = useState(7)
  const [refresh, setRefresh] = useState(0)
  const [rows, setRows] = useState<Summary[] | null>(null)
  const [error, setError] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setRows(null)
      setError(false)
      try {
        const response = await fetch('/api/usage?days=' + days, { signal: controller.signal, cache: 'no-store' })
        if (!response.ok) throw new Error()
        const result: Summary[] = await response.json()
        if (!controller.signal.aborted) setRows(result)
      } catch {
        if (!controller.signal.aborted) setError(true)
      }
    }
    void load()
    return () => controller.abort()
  }, [days, refresh])

  const ai = rows?.filter(row => row.provider === 'OpenAI') ?? []
  const youtube = rows?.filter(row => row.provider === 'YouTube') ?? []
  const tokens = ai.reduce((sum, row) => sum + row.inputTokens + row.outputTokens, 0)
  const missing = ai.reduce((sum, row) => sum + row.calls - row.measuredCalls, 0)

  return <div className="usage">
    <div className="usage-controls">
      <label>조회 기간 <select value={days} onChange={event => setDays(Number(event.target.value))}>
        <option value={1}>오늘</option><option value={7}>최근 7일</option><option value={30}>최근 30일</option>
      </select></label>
      <button onClick={() => setRefresh(n => n + 1)}>새로고침</button>
    </div>
    <p>한국 시간 기준으로 이 앱에서 보낸 API 요청을 집계합니다.</p>
    {error ? <p role="alert">사용량을 불러오지 못했습니다. 새로고침으로 다시 시도하세요.</p>
      : rows === null ? <p role="status">사용량을 불러오는 중입니다.</p>
      : <>
        <section><h2>OpenAI</h2><strong className="usage-total">{number.format(tokens)} 토큰</strong>
          {ai.length ? <table><thead><tr><th>모델</th><th>호출 수</th><th>입력 토큰</th><th>출력 토큰</th></tr></thead>
            <tbody>{ai.map(row => <tr key={row.operation}><th scope="row">{row.operation}</th>
              <td>{number.format(row.calls)}</td><td>{row.measuredCalls ? number.format(row.inputTokens) : '—'}</td>
              <td>{row.measuredCalls ? number.format(row.outputTokens) : '—'}</td></tr>)}</tbody></table>
            : <p>이 기간에 기록된 OpenAI 호출이 없습니다.</p>}
          {missing > 0 && <p>토큰 사용량이 반환되지 않은 호출 {number.format(missing)}건은 토큰 합계에서 제외했습니다.</p>}
        </section>
        <section><h2>YouTube Data API</h2>
          <strong className="usage-total">{number.format(youtube.reduce((sum, row) => sum + row.calls, 0))}회 호출</strong>
          {youtube.length ? <table><thead><tr><th>API 메서드</th><th>호출 수</th></tr></thead>
            <tbody>{youtube.map(row => <tr key={row.operation}><th scope="row">{row.operation}</th>
              <td>{number.format(row.calls)}</td></tr>)}</tbody></table>
            : <p>이 기간에 기록된 YouTube 호출이 없습니다.</p>}
        </section>
      </>}
  </div>
}

