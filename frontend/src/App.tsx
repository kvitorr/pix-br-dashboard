import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Sidebar } from './components/Navbar';
import { VisaoGeral } from './pages/VisaoGeral';
import { AnaliseMunicipal } from './pages/AnaliseMunicipal';
import { FatoresSocioeconomicos } from './pages/FatoresSocioeconomicos';

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex min-h-screen bg-page">
        <Sidebar />
        <main className="flex-1 overflow-auto px-6 py-6">
          <Routes>
            <Route path="/" element={<Navigate to="/visao-geral" replace />} />
            <Route path="/visao-geral" element={<VisaoGeral />} />
            <Route path="/analise-municipal" element={<AnaliseMunicipal />} />
            <Route path="/fatores-socioeconomicos" element={<FatoresSocioeconomicos />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
