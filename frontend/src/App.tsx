import { PromptSettings } from './features/analysis/PromptSettings'
import { useState } from 'react'
import { ExampleWorkspace } from './features/video/ExampleWorkspace'
import { YouTubeWorkspace } from './features/video/YouTubeWorkspace'
import { useServerHealth } from './shared/useServerHealth'

export default function App() {
  const [workspace, setWorkspace] = useState<'video' | 'example' | 'prompt'>(
    'video',
  )
  const [visitedPrompt, setVisitedPrompt] = useState(false)
  const example = workspace === 'example'
  const health = useServerHealth()
  const page = {
    video: '영상 작업실',
    example: '예시 작업실',
    prompt: 'AI 설정',
  }[workspace]

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <img src="/favicon.svg" width="32" height="32" alt="" />
          클립데스크
        </div>
        <nav aria-label="작업 공간">
          <button
            aria-current={workspace === 'video' ? 'page' : undefined}
            onClick={() => setWorkspace('video')}
          >
            <span aria-hidden="true">▸</span> 영상 작업실
          </button>
          <button
            aria-current={example ? 'page' : undefined}
            onClick={() => setWorkspace('example')}
          >
            <span aria-hidden="true">▦</span> 예시 작업실
          </button>
          <button
            aria-current={workspace === 'prompt' ? 'page' : undefined}
            onClick={() => {
              setVisitedPrompt(true)
              setWorkspace('prompt')
            }}
          >
            AI 설정
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
          <div hidden={workspace !== 'video'}>
            <YouTubeWorkspace />
          </div>
          <ExampleWorkspace active={example} />
          {visitedPrompt && (
            <div hidden={workspace !== 'prompt'}>
              <PromptSettings />
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
