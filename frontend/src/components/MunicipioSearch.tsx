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
      {selected ? (
        <div className="flex items-center gap-2 border border-blue-300 rounded-lg px-3 py-1.5 bg-blue-50 text-sm">
          <span className="font-medium text-blue-800">{selected.municipioNome}</span>
          <span className="text-blue-500 text-xs">— {selected.estado}</span>
          <button
            onClick={handleClear}
            className="ml-auto text-blue-400 hover:text-blue-700 font-bold text-base leading-none"
            aria-label="Limpar seleção"
          >
            ×
          </button>
        </div>
      ) : (
        <>
          <input
            type="text"
            placeholder={loading ? 'Carregando municípios...' : 'Buscar município...'}
            disabled={loading}
            className="w-full border border-gray-200 rounded-lg px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-50 disabled:text-gray-400"
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setOpen(true);
            }}
            onFocus={() => query.length >= 2 && setOpen(true)}
          />

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
        </>
      )}
    </div>
  );
}
