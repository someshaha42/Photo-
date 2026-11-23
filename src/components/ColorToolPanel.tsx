import React from 'react';

export interface ColorToolPanelProps {
  selectedColor: string;
  onSelectColor: (color: string) => void;
}

const SWATCHES = ['#ffffff', '#ff3b30', '#ff9500', '#ffcc00', '#4cd964', '#5ac8fa', '#007aff', '#5856d6', '#ff2d55', '#000000'];

export const ColorToolPanel: React.FC<ColorToolPanelProps> = ({ selectedColor, onSelectColor }) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 10 }} data-testid="color_tool_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Layer Color Swatches</div>
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
      {SWATCHES.map((hex) => (
        <button
          key={hex}
          type="button"
          onClick={() => onSelectColor(hex)}
          style={{
            width: 32,
            height: 32,
            borderRadius: '50%',
            background: hex,
            border: selectedColor === hex ? '3px solid var(--pf-amber)' : '1px solid var(--pf-outline)',
            cursor: 'pointer',
          }}
          data-testid={`color_swatch_${hex}`}
        />
      ))}
    </div>
  </div>
);
