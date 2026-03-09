// ─── Building Blocks ─────────────────────────────────────────────────────────

function SkeletonBlock({ className }: { className: string }) {
  return <div className={`bg-gray-200 rounded animate-pulse ${className}`} />;
}

function KpiCardSkeleton() {
  return (
    <div className="bg-white rounded-card border border-border px-[18px] py-[14px]">
      <SkeletonBlock className="h-3 w-24 mb-2" />
      <SkeletonBlock className="h-7 w-20 mt-1" />
      <SkeletonBlock className="h-3 w-32 mt-2" />
    </div>
  );
}

function ChartCardSkeleton({ height = 220, subtitle = false }: { height?: number; subtitle?: boolean }) {
  return (
    <div className="bg-white rounded-card border border-border">
      <div className="px-[18px] py-[14px] border-b border-border-s">
        <SkeletonBlock className="h-3 w-40" />
        {subtitle && <SkeletonBlock className="h-3 w-28 mt-1.5" />}
      </div>
      <div className="px-[18px] py-[12px]">
        <div className="bg-gray-100 rounded animate-pulse" style={{ height }} />
      </div>
    </div>
  );
}

function MapCardSkeleton({ height = 540 }: { height?: number }) {
  return (
    <div className="bg-white rounded-card border border-border h-full flex flex-col">
      <div className="px-[18px] py-[14px] border-b border-border-s">
        <SkeletonBlock className="h-3 w-40" />
      </div>
      <div className="px-[18px] py-[12px] flex-1">
        <div className="bg-gray-100 rounded animate-pulse" style={{ height }} />
      </div>
    </div>
  );
}

function RankingCardSkeleton() {
  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-center justify-between gap-3">
        <div>
          <SkeletonBlock className="h-3 w-36" />
          <SkeletonBlock className="h-3 w-44 mt-1.5" />
        </div>
        <div className="flex gap-1.5">
          <SkeletonBlock className="h-6 w-16 rounded-badge" />
          <SkeletonBlock className="h-6 w-20 rounded-badge" />
        </div>
      </div>
      <div className="px-[18px] py-[12px]">
        <table className="w-full">
          <tbody>
            {[...Array(10)].map((_, i) => (
              <tr key={i} className="border-b border-border-s last:border-0">
                <td className="py-2 w-6"><SkeletonBlock className="h-3 w-4" /></td>
                <td className="py-2 pr-2">
                  <SkeletonBlock className="h-3 w-28" />
                  <SkeletonBlock className="h-1 w-full mt-2 rounded-full" />
                </td>
                <td className="py-2"><SkeletonBlock className="h-5 w-10 rounded" /></td>
                <td className="py-2 text-right"><SkeletonBlock className="h-3 w-10 ml-auto" /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function AtipicosCardSkeleton() {
  return (
    <div className="bg-white rounded-card border border-border flex-1">
      <div className="px-[18px] py-[14px] border-b border-border-s flex items-start justify-between gap-3">
        <div>
          <SkeletonBlock className="h-3 w-36" />
          <SkeletonBlock className="h-3 w-48 mt-1.5" />
        </div>
        <SkeletonBlock className="h-6 w-40 rounded-badge shrink-0" />
      </div>
      <div className="px-[18px] py-[12px] flex flex-col gap-3">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="flex items-start gap-3 py-2 border-b border-border-s last:border-0">
            <SkeletonBlock className="mt-1.5 w-2.5 h-2.5 rounded-full shrink-0" />
            <div className="flex-1 min-w-0">
              <div className="flex items-baseline gap-1.5">
                <SkeletonBlock className="h-3 w-28" />
                <SkeletonBlock className="h-3 w-20" />
              </div>
              <div className="flex flex-wrap gap-1 mt-1.5">
                <SkeletonBlock className="h-5 w-24 rounded-badge" />
                <SkeletonBlock className="h-5 w-32 rounded-badge" />
              </div>
            </div>
            <div className="shrink-0">
              <SkeletonBlock className="h-5 w-10" />
              <SkeletonBlock className="h-3 w-16 mt-1" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function SpearmanCardSkeleton() {
  return (
    <div className="bg-white rounded-card border-2 border-accent/20 px-[18px] py-[14px] flex flex-col gap-3">
      <div className="flex items-center justify-between">
        <SkeletonBlock className="h-3 w-3/4" />
        <SkeletonBlock className="h-5 w-16 rounded-badge" />
      </div>
      <div className="flex items-baseline gap-1">
        <SkeletonBlock className="h-3 w-8 mr-1" />
        <SkeletonBlock className="h-8 w-16" />
      </div>
      <SkeletonBlock className="h-3 w-1/2" />
    </div>
  );
}

function ScatterCardSkeleton() {
  return (
    <div className="bg-white rounded-card border border-border flex flex-col">
      <div className="px-[18px] py-[14px] border-b border-border-s">
        <SkeletonBlock className="h-3 w-40" />
        <SkeletonBlock className="h-3 w-28 mt-1.5" />
      </div>
      <div className="px-2 py-3 flex-1">
        <div className="bg-gray-100 rounded animate-pulse h-[260px]" />
      </div>
      <div className="px-[18px] pb-[12px]">
        <SkeletonBlock className="h-3 w-40" />
      </div>
    </div>
  );
}

// ─── Page Skeletons ───────────────────────────────────────────────────────────

export function VisaoGeralSkeleton() {
  return (
    <div className="mt-6">
      {/* Hero: Map + Right panel */}
      <div className="flex flex-col lg:flex-row gap-6 mb-6">
        <div className="flex-1">
          <MapCardSkeleton height={540} />
        </div>
        <div className="flex flex-col gap-4 lg:w-[390px]">
          <div className="grid grid-cols-2 gap-3">
            {[...Array(4)].map((_, i) => <KpiCardSkeleton key={i} />)}
          </div>
          <ChartCardSkeleton height={220} />
        </div>
      </div>
      {/* IQR + StdDev charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
        <ChartCardSkeleton height={220} subtitle />
        <ChartCardSkeleton height={220} subtitle />
      </div>
      {/* Rankings */}
      <div className="flex flex-col lg:flex-row gap-6 lg:items-start">
        <RankingCardSkeleton />
        <AtipicosCardSkeleton />
      </div>
    </div>
  );
}

export function EvolucaoTemporalSkeleton() {
  return (
    <div className="mt-6">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {[...Array(4)].map((_, i) => <KpiCardSkeleton key={i} />)}
      </div>
      <div className="mb-6">
        <ChartCardSkeleton height={300} />
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ChartCardSkeleton height={220} />
        <ChartCardSkeleton height={220} />
      </div>
    </div>
  );
}

export function AnaliseMunicipalSkeleton() {
  return (
    <div>
      {/* Municipality header */}
      <div className="flex items-center gap-3 mb-6">
        <SkeletonBlock className="h-6 w-48" />
        <SkeletonBlock className="h-5 w-20 rounded-badge" />
        <SkeletonBlock className="h-4 w-24" />
      </div>
      {/* Hero: Map + KPIs */}
      <div className="flex flex-col lg:flex-row gap-6 mb-6">
        <div className="flex-1">
          <MapCardSkeleton height={440} />
        </div>
        <div className="flex flex-col gap-4 lg:w-[390px]">
          <div className="grid grid-cols-2 gap-3">
            {[...Array(4)].map((_, i) => <KpiCardSkeleton key={i} />)}
          </div>
        </div>
      </div>
      {/* Socioeconomic indicators */}
      <div className="bg-white rounded-card border border-border">
        <div className="px-[18px] py-[14px] border-b border-border-s">
          <SkeletonBlock className="h-3 w-40" />
        </div>
        <div className="px-[18px] py-[12px]">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            {[...Array(3)].map((_, i) => <KpiCardSkeleton key={i} />)}
          </div>
        </div>
      </div>
    </div>
  );
}

export function FatoresSocioeconomicosSkeleton() {
  return (
    <div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        {[...Array(3)].map((_, i) => <SpearmanCardSkeleton key={i} />)}
      </div>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        {[...Array(3)].map((_, i) => <ScatterCardSkeleton key={i} />)}
      </div>
    </div>
  );
}
