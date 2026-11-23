import React from 'react';

export interface CanvasSettingsPanelProps {
  activeRatio: string;
  onSelectRatio: (ratio: string) => void;
}

const RATIOS = [
  { label: '1:1 Square', value: '1:1' },
  { label: '16:9 Landscape', value: '16:9' },
  { label: '9:16 Portrait', value: '9:16' },
  { label: '4:3 Standard', value: '4:3' },
];

export const CanvasSettingsPanel: React.FC<CanvasSettingsPanelProps> = ({ activeRatio, onSelectRatio }) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 10 }} data-testid="canvas_settings_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Canvas Dimensions</div>
    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {RATIOS.map((r) => (
        <button
          key={r.value}
          type="button"
          onClick={() => onSelectRatio(r.value)}
          style={{
            padding: '10px 12px',
            borderRadius: 10,
            border: activeRatio === r.value ? '2px solid var(--pf-azure)' : '1px solid var(--pf-outline)',
            background: activeRatio === r.value ? 'rgba(45,140,255,0.15)' : 'var(--pf-surface-variant)',
            color: 'var(--pf-on-surface)',
            fontWeight: 600,
            textAlign: 'left',
            cursor: 'pointer',
          }}
          data-testid={`canvas_preset_${r.value.replace(':', '_')}`}
        >
          {r.label}
        </button>
      ))}
    </div>
  </div>
);
