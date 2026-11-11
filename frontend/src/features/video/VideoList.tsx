import { videos } from './examples'
import type { Video } from './types'

export function VideoList({
  selected,
  onSelect,
  items = videos,
  disabled = false,
}: {
  selected: string
  onSelect: (id: string) => void
  items?: Video[]
  disabled?: boolean
}) {
  return (
    <div className="videos">
      {items.map((v) => (
        <button
          type="button"
          disabled={disabled}
          key={v.id}
          aria-pressed={selected === v.id}
          onClick={() => onSelect(v.id)}
        >
          <span
            className={v.thumbnailUrl ? 'thumb video-thumbnail' : 'thumb'}
            aria-hidden="true"
            style={{ background: v.color }}
          >
            {v.art}
            {v.thumbnailUrl && (
              <img
                key={v.thumbnailUrl}
                src={v.thumbnailUrl}
                alt=""
                loading="lazy"
                onError={(event) => { event.currentTarget.hidden = true }}
              />
            )}
          </span>
          <span>
            <strong>{v.title}</strong>
            <small>
              {v.channel} · {v.date}
            </small>
          </span>
        </button>
      ))}
    </div>
  )
}
