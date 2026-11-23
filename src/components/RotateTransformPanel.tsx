import React from 'react';

export interface RotateTransformPanelProps {
  onRotate90: () => void;
  onFlipHorizontal: () => void;
  onFlipVertical: () => void;
}

export const RotateTransformPanel: React.FC<RotateTransformPanelProps> = ({ onRotate90, onFlipHorizontal, onFlipVertical }) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 10 }} data-testid="rotate_transform_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Rotate & Flip Transformation</div>
    <div style={{ display: 'flex', gap: 8 }}>
      <button
        type="button"
        onClick={onRotate90}
        style={{ flex: 1, padding: 10, borderRadius: 10, border: '1px solid var(--pf-outline)', background: 'var(--pf-surface-variant)', color: 'var(--pf-on-surface)', fontWeight: 600, cursor: 'pointer' }}
      >
        Rotate 90°
      </button>
      <button
        type="button"
        onClick={onFlipHorizontal}
        style={{ flex: 1, padding: 10, borderRadius: 10, border: '1px solid var(--pf-outline)', background: 'var(--pf-surface-variant)', color: 'var(--pf-on-surface)', fontWeight: 600, cursor: 'pointer' }}
      >
        Flip H
      </button>
      <button
        type="button"
        onClick={onFlipVertical}
        style={{ flex: 1, padding: 10, borderRadius: 10, border: '1px solid var(--pf-outline)', background: 'var(--pf-surface-variant)', color: 'var(--pf-on-surface)', fontWeight: 600, cursor: 'pointer' }}
      >
        Flip V
      </button>
    </div>
  </div>
);
