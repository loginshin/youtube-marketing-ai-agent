import { useEffect, useState } from 'react'

export function useServerHealth() {
  const [message, setMessage] = useState('서버 연결을 확인하고 있습니다.')

  useEffect(() => {
    const controller = new AbortController()
    async function checkHealth() {
      try {
        const response = await fetch('/actuator/health', {
          signal: controller.signal,
        })
        if (!response.ok || (await response.json()).status !== 'UP') {
          throw new Error('Backend unavailable')
        }
        setMessage('서버 연결 정상')
      } catch {
        if (!controller.signal.aborted) {
          setMessage(
            '서버에 연결할 수 없습니다. 백엔드 실행 후 새로고침하세요.',
          )
        }
      }
    }
    void checkHealth()
    return () => controller.abort()
  }, [])

  return message
}
