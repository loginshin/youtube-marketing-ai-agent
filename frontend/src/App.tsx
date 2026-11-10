import { useState } from 'react'
import { ExampleWorkspace } from './features/video/ExampleWorkspace'
import { YouTubeWorkspace } from './features/video/YouTubeWorkspace'
import { useServerHealth } from './shared/useServerHealth'

export default function App() {
  const [example, setExample] = useState(false)
  const health = useServerHealth()
  const page = example ? '예시 작업실' : '영상 작업실'

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <img src="/favicon.svg" width="32" height="32" alt="" />
          클립데스크
        </div>
        <nav aria-label="작업 공간">
          <button
            aria-current={!example ? 'page' : undefined}
            onClick={() => setExample(false)}
          >
            <span aria-hidden="true">▸</span> 영상 작업실
          </button>
          <button
            aria-current={example ? 'page' : undefined}
            onClick={() => setExample(true)}
          >
            <span aria-hidden="true">▦</span> 예시 작업실
          </button>
        </nav>
      </aside>
      <div className="content">
        <header className="topbar">
          <span>워크스페이스 ／ {page}</span>
          <small>{health}</small>
        </header>
        <main>
          <h1>{page}</h1>
          <div hidden={example}>
            <YouTubeWorkspace />
          </div>
          <ExampleWorkspace active={example} />
        </main>
      </div>
    </div>
  )
}
