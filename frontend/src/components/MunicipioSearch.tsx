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
      <div className={`flex items-center gap-2 border rounded-lg px-3 py-1.5 bg-white ${selected ? 'border-blue-300' : 'border-gray-200'}`}>
        {selected && (
          <div className="flex items-center gap-1 bg-blue-50 border border-blue-200 rounded px-2 py-0.5 text-sm shrink-0">
            <span className="font-medium text-blue-800">{selected.municipioNome}</span>
            <span className="text-blue-500 text-xs">— {selected.estado}</span>
            <button
              onClick={handleClear}
              className="ml-1 text-blue-400 hover:text-blue-700 font-bold text-base leading-none"
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
          className="flex-1 min-w-[100px] text-sm focus:outline-none bg-transparent disabled:text-gray-400"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => query.length >= 2 && setOpen(true)}
        />
      </div>

      {open && filtered.length > 0 && (
        <ul className="absolute z-50 top-full mt-1 left-0 right-0 bg-white border border-gray-200 rounded-lg shadow-lg max-h-64 overflow-y-auto">
          {filtered.map((m) => (
            <li
              key={m.municipioIbge}
              className="px-3 py-2 text-sm cursor-pointer hover:bg-blue-50 flex items-center justify-between"
              onMouseDown={() => handleSelect(m)}
            >
              <span className="font-medium text-gray-800">{m.municipioNome}</span>
              <span className="text-gray-400 text-xs ml-2">{m.estado}</span>
            </li>
          ))}
        </ul>
      )}

      {open && query.length >= 2 && filtered.length === 0 && (
        <div className="absolute z-50 top-full mt-1 left-0 right-0 bg-white border border-gray-200 rounded-lg shadow-lg px-3 py-2 text-sm text-gray-500">
          Nenhum município encontrado
        </div>
      )}
    </div>
  );
}
