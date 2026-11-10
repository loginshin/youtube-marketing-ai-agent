import { useState } from 'react'
import { ExampleWorkspace } from './features/video/ExampleWorkspace'
import { YouTubeWorkspace } from './features/video/YouTubeWorkspace'
import { useServerHealth } from './shared/useServerHealth'

export default function App() {
  const [source, setSource] = useState('example')
  const message = useServerHealth()
  const usingExamples = source === 'example'

  return (
    <main>
      <header>
        <h1>댓글 작업실</h1>
        <span>{usingExamples ? '예시 데이터' : 'YouTube 실제 조회'}</span>
      </header>
      <label>
        데이터 소스
        <select
          value={source}
          onChange={(event) => setSource(event.target.value)}
        >
          <option value="example">예시 영상</option>
          <option value="youtube">YouTube 검색</option>
        </select>
      </label>
      <ExampleWorkspace active={usingExamples} />
      {!usingExamples && <YouTubeWorkspace />}
      <footer>{message}</footer>
    </main>
  )
}
