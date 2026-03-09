export function LoadingState() {
  return (
    <div className="flex items-center justify-center h-64">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-accent" />
      <span className="ml-3 text-secondary">Carregando...</span>
    </div>
  );
}
