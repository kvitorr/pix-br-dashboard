import { useState } from 'react';
import { NavLink } from 'react-router-dom';

const LINKS = [
  {
    to: '/visao-geral',
    label: 'Visão Geral',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10" />
        <line x1="2" y1="12" x2="22" y2="12" />
        <path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
      </svg>
    ),
  },
  {
    to: '/evolucao-temporal',
    label: 'Evolução Temporal',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
      </svg>
    ),
  },
  {
    to: '/analise-municipal',
    label: 'Análise Municipal',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
    ),
  },
];

function getInitialOpen(): boolean {
  try {
    const stored = localStorage.getItem('sidebar-open');
    return stored === null ? true : stored === 'true';
  } catch {
    return true;
  }
}

export function Sidebar() {
  const [isOpen, setIsOpen] = useState<boolean>(getInitialOpen);

  function toggle() {
    setIsOpen((prev) => {
      const next = !prev;
      try {
        localStorage.setItem('sidebar-open', String(next));
      } catch {
        // ignore
      }
      return next;
    });
  }

  return (
    <aside
      className={`flex flex-col h-screen sticky top-0 bg-white border-r border-gray-200 shadow-sm shrink-0 transition-all duration-300 ease-in-out ${
        isOpen ? 'w-60' : 'w-14'
      }`}
    >
      {/* Header */}
      <div className="flex items-center h-14 px-3 border-b border-gray-100 overflow-hidden">
        <button
          onClick={toggle}
          className="p-1.5 rounded-md text-gray-500 hover:text-gray-800 hover:bg-gray-100 transition-colors shrink-0"
          title={isOpen ? 'Fechar menu' : 'Abrir menu'}
        >
          <svg xmlns="http://www.w3.org/2000/svg" className="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>

        <span
          className={`ml-3 font-bold text-blue-700 text-sm whitespace-nowrap transition-all duration-300 overflow-hidden ${
            isOpen ? 'opacity-100 max-w-xs' : 'opacity-0 max-w-0'
          }`}
        >
          Pix Brasil
        </span>
      </div>

      {/* Nav links */}
      <nav className="flex-1 py-3 flex flex-col gap-0.5 px-2">
        {LINKS.map((link) => (
          <NavLink
            key={link.to}
            to={link.to}
            title={!isOpen ? link.label : undefined}
            className={({ isActive }) =>
              `flex items-center rounded-lg px-2 py-2.5 transition-colors overflow-hidden ${
                isActive
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
              }`
            }
          >
            {link.icon}
            <span
              className={`ml-3 text-sm font-medium whitespace-nowrap transition-all duration-300 overflow-hidden ${
                isOpen ? 'opacity-100 max-w-xs' : 'opacity-0 max-w-0'
              }`}
            >
              {link.label}
            </span>
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}

/** @deprecated use Sidebar instead */
export function Navbar() {
  return <Sidebar />;
}
