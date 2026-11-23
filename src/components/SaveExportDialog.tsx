import React from 'react';

export interface SaveExportDialogProps {
  onExport: (format: 'png' | 'jpg') => void;
  onDismiss: () => void;
}

export const SaveExportDialog: React.FC<SaveExportDialogProps> = ({ onExport, onDismiss }) => (
  <div
    style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.65)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
    }}
  >
    <div
      style={{
        width: 'min(90vw, 360px)',
        background: 'var(--pf-surface)',
        border: '1px solid var(--pf-outline)',
        borderRadius: 20,
        padding: 20,
        display: 'flex',
        flexDirection: 'column',
        gap: 14,
      }}
    >
      <div style={{ fontSize: 18, fontWeight: 700 }}>Export Project Image</div>
      <div style={{ display: 'flex', gap: 10 }}>
        <button
          type="button"
          onClick={() => onExport('png')}
          style={{
            flex: 1,
            padding: 12,
            borderRadius: 12,
            border: 'none',
            background: 'var(--pf-azure)',
            color: '#ffffff',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          Export PNG (Transparent)
        </button>
        <button
          type="button"
          onClick={() => onExport('jpg')}
          style={{
            flex: 1,
            padding: 12,
            borderRadius: 12,
            border: 'none',
            background: 'var(--pf-teal)',
            color: '#00201b',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          Export JPG (Opaque)
        </button>
      </div>
      <button
        type="button"
        onClick={onDismiss}
        style={{
          alignSelf: 'flex-end',
          padding: '6px 12px',
          background: 'transparent',
          border: 'none',
          color: 'var(--pf-on-variant)',
          cursor: 'pointer',
          fontWeight: 600,
        }}
      >
        Cancel
      </button>
    </div>
  </div>
);
