import { videos } from './examples'

export function VideoList({ selected, onSelect }: { selected: string; onSelect: (id: string) => void }) {
  return <div className="videos">{videos.map(v => <button type="button" key={v.id} aria-pressed={selected === v.id} onClick={() => onSelect(v.id)}>
    <span className="thumb" aria-hidden="true" style={{ background: v.color }}>{v.art}</span>
    <span><strong>{v.title}</strong><small>{v.channel} · {v.date}</small></span>
  </button>)}</div>
}
