import React from 'react';

export interface AdjustToolPanelProps {
  exposure: number;
  contrast: number;
  saturation: number;
  onUpdateExposure: (val: number) => void;
  onUpdateContrast: (val: number) => void;
  onUpdateSaturation: (val: number) => void;
}

export const AdjustToolPanel: React.FC<AdjustToolPanelProps> = ({
  exposure,
  contrast,
  saturation,
  onUpdateExposure,
  onUpdateContrast,
  onUpdateSaturation,
}) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 12 }} data-testid="adjust_tool_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Photo Color Adjustments</div>
    
    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
        <span>Exposure</span>
        <span>{exposure.toFixed(1)}</span>
      </div>
      <input
        type="range"
        min="-2"
        max="2"
        step="0.1"
        value={exposure}
        onChange={(e) => onUpdateExposure(parseFloat(e.target.value))}
        style={{ width: '100%', cursor: 'pointer' }}
      />
    </div>

    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
        <span>Contrast</span>
        <span>{contrast.toFixed(1)}</span>
      </div>
      <input
        type="range"
        min="-1"
        max="2"
        step="0.1"
        value={contrast}
        onChange={(e) => onUpdateContrast(parseFloat(e.target.value))}
        style={{ width: '100%', cursor: 'pointer' }}
      />
    </div>

    <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
        <span>Saturation</span>
        <span>{saturation.toFixed(1)}</span>
      </div>
      <input
        type="range"
        min="-1"
        max="2"
        step="0.1"
        value={saturation}
        onChange={(e) => onUpdateSaturation(parseFloat(e.target.value))}
        style={{ width: '100%', cursor: 'pointer' }}
      />
    </div>
  </div>
);
