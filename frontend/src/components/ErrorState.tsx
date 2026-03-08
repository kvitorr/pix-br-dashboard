interface ErrorStateProps {
  message?: string;
}

export function ErrorState({ message = 'Erro ao carregar dados.' }: ErrorStateProps) {
  return (
    <div className="flex items-center justify-center h-64">
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-md text-center">
        <p className="text-red-700 font-medium">{message}</p>
        <p className="text-red-500 text-sm mt-2">Verifique se o backend está em execução.</p>
      </div>
    </div>
  );
}
