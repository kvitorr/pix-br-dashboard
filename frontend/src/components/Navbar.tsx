import { NavLink } from 'react-router-dom';

const LINKS = [
  { to: '/visao-geral', label: 'Visão Geral' },
  { to: '/disparidade-regional', label: 'Disparidade Regional' },
  { to: '/fatores-socioeconomicos', label: 'Fatores Socioeconômicos' },
  { to: '/evolucao-temporal', label: 'Evolução Temporal' },
];

export function Navbar() {
  return (
    <nav className="bg-white border-b border-gray-200 shadow-sm">
      <div className="container mx-auto px-4">
        <div className="flex items-center h-14 gap-1">
          <span className="font-bold text-blue-700 mr-4 text-sm whitespace-nowrap">
            Pix Brasil
          </span>
          {LINKS.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `px-3 py-2 rounded-md text-sm font-medium transition-colors whitespace-nowrap ${
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  );
}
