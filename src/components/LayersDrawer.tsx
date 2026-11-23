import React from 'react';

export interface LayersDrawerProps {
  onClose: () => void;
}

export const LayersDrawer: React.FC<LayersDrawerProps> = ({ onClose }) => (
  <div
    style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      justifyContent: 'flex-start',
      zIndex: 100,
    }}
    data-testid="layers_side_drawer"
  >
    <div
      style={{
        width: 'min(85vw, 300px)',
        height: '100%',
        background: 'var(--pf-surface)',
        borderRight: '1px solid var(--pf-outline)',
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>Layers</h3>
        <button
          type="button"
          onClick={onClose}
          style={{ background: 'transparent', border: 'none', color: 'var(--pf-on-surface)', fontSize: 18, cursor: 'pointer' }}
        >
          ✕
        </button>
      </div>
      <div
        style={{
          padding: 12,
          borderRadius: 10,
          background: 'var(--pf-surface-variant)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <span>Layer 1 (Text)</span>
        <span style={{ fontSize: 12, color: 'var(--pf-azure)', fontWeight: 600 }}>Active</span>
      </div>
    </div>
  </div>
);
