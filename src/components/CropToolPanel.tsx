import React from 'react';

export interface CropToolPanelProps {
  onCommitCrop: () => void;
}

export const CropToolPanel: React.FC<CropToolPanelProps> = ({ onCommitCrop }) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 10 }} data-testid="crop_tool_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Interactive Crop & Mask</div>
    <p style={{ fontSize: 12, color: 'var(--pf-on-variant)', margin: 0 }}>
      Drag corner or edge handles on the canvas to crop and frame your active layer.
    </p>
    <button
      type="button"
      onClick={onCommitCrop}
      style={{
        width: '100%',
        padding: '10px',
        borderRadius: 10,
        border: 'none',
        background: 'var(--pf-emerald)',
        color: '#ffffff',
        fontWeight: 700,
        cursor: 'pointer',
      }}
      data-testid="crop_done_button"
    >
      Apply Crop
    </button>
  </div>
);
