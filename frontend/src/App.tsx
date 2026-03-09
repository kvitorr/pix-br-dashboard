import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Sidebar } from './components/Navbar';
import { VisaoGeral } from './pages/VisaoGeral';
import { EvolucaoTemporal } from './pages/EvolucaoTemporal';
import { AnaliseMunicipal } from './pages/AnaliseMunicipal';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen bg-gray-50">
        <Sidebar />
        <main className="flex-1 overflow-auto px-6 py-6">
          <Routes>
            <Route path="/" element={<Navigate to="/visao-geral" replace />} />
            <Route path="/visao-geral" element={<VisaoGeral />} />
            <Route path="/evolucao-temporal" element={<EvolucaoTemporal />} />
            <Route path="/analise-municipal" element={<AnaliseMunicipal />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
