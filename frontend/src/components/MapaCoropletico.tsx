import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import type { Map as LeafletMap, GeoJSON as GeoJSONLayer } from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { feature } from 'topojson-client';
import type { Topology } from 'topojson-specification';
import { CHOROPLETH_SCALE } from '../constants/colors';
import type { MapaMunicipio } from '../types/dashboard';

let geojsonCache: GeoJSON.FeatureCollection | null = null;
let ufCache: GeoJSON.FeatureCollection | null = null;

export type MetricFormato = 'percent' | 'currency' | 'decimal';

// ============================================================================
// DICIONÁRIO DE ESCALAS FIXAS (Ajuste os valores conforme a sua realidade)
// ============================================================================
const ESCALAS_FIXAS: Record<string, number[]> = {
  penetracaoPf: [25, 35, 50, 60],        // Em %
  ticketMedioPf: [153.97, 196.65, 247.45, 326.78],     // Em R$
  vlPerCapitaPf: [1315.29, 2613.39, 4072.74, 5877.84], // Em R$
  razaoPjPf: [0.0288, 0.0477, 0.0702, 0.0990]     // Decimal (proporção)
};

interface MapaCoropléticoProps {
  municipios: MapaMunicipio[];
  metricKey?: keyof MapaMunicipio;
  metricLabel?: string;
  metricFormato?: MetricFormato;
  height?: number;
  showTileLayer?: boolean;
}

function formatMetrica(val: number, formato: MetricFormato): string {
  if (formato === 'percent') return `${val.toFixed(1)}%`;
  if (formato === 'currency')
    return `R$ ${val.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  return val.toFixed(4);
}

function formatLegendValue(val: number, formato: MetricFormato): string {
  if (formato === 'percent') return `${val.toFixed(1)}%`;
  if (formato === 'currency')
    return `R$ ${val.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}`;
  return val.toFixed(4);
}

export function MapaCoropletico({
  municipios,
  metricKey = 'penetracaoPf',
  metricLabel = 'Penetração',
  metricFormato = 'percent',
  height = 480,
  showTileLayer = false,
}: MapaCoropléticoProps) {
  const mapRef = useRef<LeafletMap | null>(null);
  const layerRef = useRef<GeoJSONLayer | null>(null);
  const ufLayerRef = useRef<GeoJSONLayer | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const showTileLayerRef = useRef(showTileLayer);

  const [isLoading, setIsLoading] = useState(!geojsonCache);
  const [legendItems, setLegendItems] = useState<{color: string, label: string}[]>([]);

  // 1. INICIALIZA O MAPA
  useEffect(() => {
    if (!containerRef.current || mapRef.current) return;

    try {
      const map = L.map(containerRef.current, {
        zoomControl: true,
        zoomSnap: 0.1,
        wheelPxPerZoomLevel: 120
      }).setView([-15.78, -47.93], 4.5);

      map.createPane('ufBorders');
      const ufPane = map.getPane('ufBorders');
      if (ufPane) {
        ufPane.style.zIndex = '410';
        ufPane.style.pointerEvents = 'none';
      }

      if (showTileLayerRef.current) {
        L.tileLayer('https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png', {
          attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
          subdomains: 'abcd',
          maxZoom: 19
        }).addTo(map);
      }

      mapRef.current = map;
    } catch (err) {
      console.error("Erro ao inicializar Leaflet:", err);
    }

    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
    };
  }, []);

  // 2. BUSCA E RENDERIZA OS DADOS
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !municipios) return;

    let isMounted = true;

    const updateLayer = (geojson: GeoJSON.FeatureCollection) => {
      if (!isMounted || !map) return;

      if (layerRef.current) map.removeLayer(layerRef.current);

      try {
        const values = municipios
          .map((m) => m[metricKey] as number | null)
          .filter((v): v is number => v != null);

        // Busca a escala fixa da métrica (ou usa uma padrão de segurança)
        const thresholds = ESCALAS_FIXAS[String(metricKey)] || [20, 40, 60, 80];
        const dadosMap = new Map(municipios.map(m => [String(m.municipioIbge), m]));

        // Monta a Legenda usando a escala fixa
        if (values.length > 0) {
          const [t0, t1, t2, t3] = thresholds;
          setLegendItems([
            { color: CHOROPLETH_SCALE[0] ?? '#e5e7eb', label: `Até ${formatLegendValue(t0, metricFormato)}` },
            { color: CHOROPLETH_SCALE[1] ?? '#e5e7eb', label: `${formatLegendValue(t0, metricFormato)} a ${formatLegendValue(t1, metricFormato)}` },
            { color: CHOROPLETH_SCALE[2] ?? '#e5e7eb', label: `${formatLegendValue(t1, metricFormato)} a ${formatLegendValue(t2, metricFormato)}` },
            { color: CHOROPLETH_SCALE[3] ?? '#e5e7eb', label: `${formatLegendValue(t2, metricFormato)} a ${formatLegendValue(t3, metricFormato)}` },
            { color: CHOROPLETH_SCALE[4] ?? '#e5e7eb', label: `Acima de ${formatLegendValue(t3, metricFormato)}` },
          ]);
        }

        const featuresFiltradas = geojson.features.filter(feat => {
          const cod = feat?.properties?.codarea || feat?.properties?.CD_MUN;
          return dadosMap.has(String(cod));
        });

        const geojsonRecortado: GeoJSON.FeatureCollection = {
          ...geojson,
          features: featuresFiltradas
        };

        layerRef.current = L.geoJSON(geojsonRecortado, {
          style: (feat) => {
            const cod = feat?.properties?.codarea || feat?.properties?.CD_MUN;
            const municipioData = cod ? dadosMap.get(String(cod)) : undefined;
            const val = municipioData?.[metricKey] as number | undefined;

            const idx = thresholds.filter((t) => (val ?? 0) > t).length;
            const fillColor = CHOROPLETH_SCALE[idx] ?? CHOROPLETH_SCALE[4];

            return { fillColor, weight: 0.5, color: '#e2e8f0', fillOpacity: 1 };
          },
          onEachFeature: (feat, layer) => {
            const cod = feat?.properties?.codarea || feat?.properties?.CD_MUN;
            const municipioData = cod ? dadosMap.get(String(cod)) : undefined;
            const nome = municipioData?.municipioNome || 'Sem Dados';
            const val = municipioData?.[metricKey] as number | null | undefined;

            const valFormatado = val != null ? formatMetrica(val, metricFormato) : '—';
            layer.bindTooltip(`<b>${nome}</b><br/>${valFormatado}`, {
              sticky: true,
              direction: 'auto',
              className: 'shadow-sm'
            });

            layer.on({
              mouseover: (e) => {
                const target = e.target;
                target.setStyle({
                  weight: 2,
                  color: '#1e293b',
                  fillOpacity: 1
                });
                target.bringToFront();
              },
              mouseout: (e) => {
                if (layerRef.current) {
                  layerRef.current.resetStyle(e.target);
                }
              },
              click: (e) => {
                const currentMap = mapRef.current;
                if (currentMap) {
                  currentMap.fitBounds(e.target.getBounds(), {
                    padding: [50, 50],
                    maxZoom: 9,
                    animate: true
                  });
                }
              }
            });
          }
        }).addTo(map);

        if (featuresFiltradas.length > 0 && layerRef.current) {
          const fitMaxZoom = featuresFiltradas.length === 1 ? 11 : 8;
          map.fitBounds(layerRef.current.getBounds(), {
             padding: [40, 40],
             animate: false,
             maxZoom: fitMaxZoom
          });
        }

        const renderizarEstados = (ufData: GeoJSON.FeatureCollection) => {
          if (!map) return;
          if (ufLayerRef.current) map.removeLayer(ufLayerRef.current);

          const ufsAtivas = new Set(
            Array.from(dadosMap.keys()).map(cod => String(cod).substring(0, 2))
          );

          const ufsFiltradas = {
            ...ufData,
            features: ufData.features.filter(feat => {
              const codUf = feat?.properties?.codarea || feat?.properties?.CD_UF;
              return ufsAtivas.has(String(codUf));
            })
          };

          ufLayerRef.current = L.geoJSON(ufsFiltradas, {
            pane: 'ufBorders',
            style: {
              fill: false,
              color: '#4755694d',
              weight: 1.5,
              interactive: false
            }
          }).addTo(map);
        };

        if (ufCache) {
          renderizarEstados(ufCache);
        } else {
          fetch('https://servicodados.ibge.gov.br/api/v3/malhas/paises/BR?intrarregiao=UF&formato=application/json&qualidade=minima')
            .then(r => r.json())
            .then((data: Topology) => {
              if (!data || data.type !== 'Topology') return;
              const objectKey = Object.keys(data.objects)[0];
              if (!objectKey) return;

              const ufGeojson = feature(data, data.objects[objectKey]) as unknown as GeoJSON.FeatureCollection;
              ufCache = ufGeojson;
              renderizarEstados(ufGeojson);
            })
            .catch(console.error);
        }

        setIsLoading(false);

      } catch (e) {
        console.error("Erro ao processar camada GeoJSON:", e);
        setIsLoading(false);
      }
    };

    if (geojsonCache) {
      updateLayer(geojsonCache);
    } else {
      setIsLoading(true);
      fetch('https://servicodados.ibge.gov.br/api/v3/malhas/paises/BR?intrarregiao=municipio&formato=application/json&qualidade=minima')
        .then(r => r.json())
        .then((data: Topology) => {
          if (!data || data.type !== 'Topology') {
             setIsLoading(false);
             return;
          }
          const objectKey = Object.keys(data.objects)[0];
          if (!objectKey) {
             setIsLoading(false);
             return;
          }
          const geojsonConverted = feature(data, data.objects[objectKey]) as unknown as GeoJSON.FeatureCollection;

          geojsonCache = geojsonConverted;
          updateLayer(geojsonConverted);
        })
        .catch((err) => {
          console.error(err);
          setIsLoading(false);
        });
    }

    return () => { isMounted = false; };
  }, [municipios, metricKey, metricFormato]);

  return (
      <div className="relative rounded-xl overflow-hidden bg-slate-50" style={{ height: `${height}px`, width: '100%' }}>

      {/* Container do Mapa */}
      <div ref={containerRef} className="absolute inset-0 z-0" />

      {/* Legenda */}
      {!isLoading && legendItems.length > 0 && (
        <div className="absolute bottom-4 right-4 z-20 bg-white/90 backdrop-blur-sm p-3.5 rounded-lg shadow-md border border-slate-200 text-xs text-slate-700 pointer-events-none">
          <h4 className="font-bold text-slate-800 mb-2.5 uppercase tracking-wide text-[10px]">{metricLabel}</h4>
          <div className="flex flex-col gap-2">
            {legendItems.map((item, idx) => (
              <div key={idx} className="flex items-center gap-2.5">
                <span
                  className="w-4 h-4 rounded-sm shadow-sm border border-black/10"
                  style={{ backgroundColor: item.color }}
                />
                <span className="font-medium">{item.label}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Loading Spinner */}
      {isLoading && (
        <div className="absolute inset-0 z-30 flex flex-col items-center justify-center bg-white/60 backdrop-blur-[2px] transition-opacity duration-300">
          <div className="w-10 h-10 border-4 border-slate-300 border-t-blue-600 rounded-full animate-spin shadow-md"></div>
          <span className="mt-3 text-sm font-semibold text-slate-700 drop-shadow-sm">
            Montando mapa...
          </span>
        </div>
      )}
    </div>
  );
}