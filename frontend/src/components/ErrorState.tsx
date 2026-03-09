interface ErrorStateProps {
  message?: string;
}

export function ErrorState({ message = 'Erro ao carregar dados.' }: ErrorStateProps) {
  return (
    <div className="flex items-center justify-center h-64">
      <div className="bg-neg-bg border border-[#fca5a5] rounded-card p-6 max-w-md text-center">
        <p className="text-neg font-medium">{message}</p>
        <p className="text-neg/80 text-sm mt-2">Verifique se o backend está em execução.</p>
      </div>
    </div>
  );
}
