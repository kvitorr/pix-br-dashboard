import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { VisaoGeral } from './pages/VisaoGeral';
import { DisparidadeRegional } from './pages/DisparidadeRegional';
import { FatoresSocioeconomicos } from './pages/FatoresSocioeconomicos';
import { EvolucaoTemporal } from './pages/EvolucaoTemporal';

export default function App() {
  return (
    <BrowserRouter>
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <main className="container mx-auto px-4 py-6">
          <Routes>
            <Route path="/" element={<Navigate to="/visao-geral" replace />} />
            <Route path="/visao-geral" element={<VisaoGeral />} />
            <Route path="/disparidade-regional" element={<DisparidadeRegional />} />
            <Route path="/fatores-socioeconomicos" element={<FatoresSocioeconomicos />} />
            <Route path="/evolucao-temporal" element={<EvolucaoTemporal />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
