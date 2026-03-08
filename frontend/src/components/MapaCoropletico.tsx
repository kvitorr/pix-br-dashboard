import { useEffect, useRef } from 'react';
import L from 'leaflet';
import type { Map as LeafletMap, GeoJSON as GeoJSONLayer } from 'leaflet';
import { CHOROPLETH_SCALE } from '../constants/colors';
import type { MapaMunicipio } from '../types/dashboard';

// Cache do GeoJSON no módulo para evitar refetch a cada render
let geojsonCache: GeoJSON.FeatureCollection | null = null;

interface MapaCoropléticoProps {
  municipios: MapaMunicipio[];
}

export function MapaCoropletico({ municipios }: MapaCoropléticoProps) {
  const mapRef = useRef<LeafletMap | null>(null);
  const layerRef = useRef<GeoJSONLayer | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    // Inicializar mapa somente uma vez
    if (!mapRef.current) {
      mapRef.current = L.map(containerRef.current).setView([-15.78, -47.93], 4);
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors',
        opacity: 0.4,
      }).addTo(mapRef.current);
    }

    if (municipios.length === 0) return;

    const penetracaoMap = new Map<string, number | null>(
      municipios.map((m) => [m.municipioIbge, m.penetracaoPf])
    );

    // Calcular quintis dinamicamente
    const values = municipios
      .map((m) => m.penetracaoPf)
      .filter((v): v is number => v != null)
      .sort((a, b) => a - b);

    const thresholds = [1, 2, 3, 4].map((q) => values[Math.floor((q * values.length) / 5)] ?? 0);

    function getColor(val: number | null | undefined): string {
      if (val == null) return '#e5e7eb';
      const idx = thresholds.filter((t) => val > t).length;
      return CHOROPLETH_SCALE[idx] ?? CHOROPLETH_SCALE[4];
    }

    function applyLayer(geojson: GeoJSON.FeatureCollection) {
      if (layerRef.current) layerRef.current.remove();
      layerRef.current = L.geoJSON(geojson, {
        style: (feature) => ({
          fillColor: getColor(penetracaoMap.get(feature?.properties?.CD_MUN ?? '') ?? null),
          weight: 0.3,
          color: '#fff',
          fillOpacity: 0.8,
        }),
        onEachFeature: (feature, layer) => {
          const cod = feature.properties?.CD_MUN as string | undefined;
          const nome = feature.properties?.NM_MUN as string | undefined;
          const val = cod ? penetracaoMap.get(cod) : null;
          layer.bindTooltip(
            `<strong>${nome ?? 'Município'}</strong><br/>Penetração: ${val != null ? val.toFixed(1) + '%' : 'N/D'}`
          );
        },
      }).addTo(mapRef.current!);
    }

    if (geojsonCache) {
      applyLayer(geojsonCache);
    } else {
      fetch(
        'https://servicodados.ibge.gov.br/api/v3/malhas/paises/BR?formato=application/json&resolucao=5&qualidade=intermediaria'
      )
        .then((r) => r.json() as Promise<GeoJSON.FeatureCollection>)
        .then((geojson) => {
          geojsonCache = geojson;
          applyLayer(geojson);
        })
        .catch(console.error);
    }
  }, [municipios]);

  // Cleanup ao desmontar
  useEffect(() => {
    return () => {
      if (mapRef.current) {
        mapRef.current.remove();
        mapRef.current = null;
      }
    };
  }, []);

  return <div ref={containerRef} style={{ height: '480px', width: '100%' }} className="rounded-xl overflow-hidden" />;
}
