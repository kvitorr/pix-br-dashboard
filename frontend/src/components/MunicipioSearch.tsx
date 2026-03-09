import { useState, useRef, useEffect } from 'react';
import type { MunicipioListItem } from '../types/dashboard';
import { useMunicipioSearch } from '../hooks/useMunicipio';

interface MunicipioSearchProps {
  selected: MunicipioListItem | null;
  onSelect: (municipio: MunicipioListItem | null) => void;
}

export function MunicipioSearch({ selected, onSelect }: MunicipioSearchProps) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { results, loading } = useMunicipioSearch(query);

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

  const showDropdown = open && query.length >= 2;

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
          placeholder={selected ? 'Trocar município...' : 'Buscar município...'}
          className="flex-1 min-w-[100px] text-[13px] focus:outline-none bg-transparent"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => query.length >= 2 && setOpen(true)}
        />
        {loading && (
          <svg className="animate-spin h-4 w-4 text-muted shrink-0" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l3-3-3-3v4a8 8 0 00-8 8h4z" />
          </svg>
        )}
      </div>

      {showDropdown && (
        <ul className="absolute z-50 top-full mt-1 left-0 right-0 bg-white border border-border rounded-input shadow-lg max-h-64 overflow-y-auto">
          {loading && (
            <li className="px-3 py-2 text-[13px] text-muted">Buscando...</li>
          )}
          {!loading && results.map((m) => (
            <li
              key={m.municipioIbge}
              className="px-3 py-2 text-[13px] cursor-pointer hover:bg-accent-bg flex items-center justify-between"
              onMouseDown={() => handleSelect(m)}
            >
              <span className="font-medium text-main">{m.municipioNome}</span>
              <span className="text-muted text-xs ml-2">{m.estado}</span>
            </li>
          ))}
          {!loading && results.length === 0 && (
            <li className="px-3 py-2 text-[13px] text-secondary">
              Nenhum município encontrado
            </li>
          )}
        </ul>
      )}
    </div>
  );
}
