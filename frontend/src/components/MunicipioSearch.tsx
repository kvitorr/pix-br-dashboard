import { useState, useRef, useEffect } from 'react';
import type { MunicipioListItem } from '../types/dashboard';

interface MunicipioSearchProps {
  municipios: MunicipioListItem[];
  selected: MunicipioListItem | null;
  onSelect: (municipio: MunicipioListItem | null) => void;
  loading?: boolean;
}

export function MunicipioSearch({ municipios, selected, onSelect, loading = false }: MunicipioSearchProps) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const filtered = query.length >= 2
    ? municipios
        .filter(m =>
          m.municipioNome.toLowerCase().includes(query.toLowerCase())
        )
        .slice(0, 50)
    : [];

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (municipio: MunicipioListItem) => {
    onSelect(municipio);
    setQuery('');
    setOpen(false);
  };

  const handleClear = () => {
    onSelect(null);
    setQuery('');
    setOpen(false);
  };

  return (
    <div ref={containerRef} className="relative flex-1 min-w-[280px]">
      <div className={`flex items-center gap-2 border rounded-input px-3 py-1.5 bg-white ${selected ? 'border-accent' : 'border-border'}`}>
        {selected && (
          <div className="flex items-center gap-1 bg-accent-bg border border-accent/30 rounded px-2 py-[3px] text-[13px] shrink-0">
            <span className="font-medium text-accent">{selected.municipioNome}</span>
            <span className="text-secondary text-xs">— {selected.estado}</span>
            <button
              onClick={handleClear}
              className="ml-1 text-accent/60 hover:text-accent font-bold text-base leading-none"
              aria-label="Limpar seleção"
            >
              ×
            </button>
          </div>
        )}
        <input
          type="text"
          placeholder={loading ? 'Carregando municípios...' : selected ? 'Trocar município...' : 'Buscar município...'}
          disabled={loading}
          className="flex-1 min-w-[100px] text-[13px] focus:outline-none bg-transparent disabled:text-muted"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => query.length >= 2 && setOpen(true)}
        />
      </div>

      {open && filtered.length > 0 && (
        <ul className="absolute z-50 top-full mt-1 left-0 right-0 bg-white border border-border rounded-input shadow-lg max-h-64 overflow-y-auto">
          {filtered.map((m) => (
            <li
              key={m.municipioIbge}
              className="px-3 py-2 text-[13px] cursor-pointer hover:bg-accent-bg flex items-center justify-between"
              onMouseDown={() => handleSelect(m)}
            >
              <span className="font-medium text-main">{m.municipioNome}</span>
              <span className="text-muted text-xs ml-2">{m.estado}</span>
            </li>
          ))}
        </ul>
      )}

      {open && query.length >= 2 && filtered.length === 0 && (
        <div className="absolute z-50 top-full mt-1 left-0 right-0 bg-white border border-border rounded-input shadow-lg px-3 py-2 text-[13px] text-secondary">
          Nenhum município encontrado
        </div>
      )}
    </div>
  );
}
