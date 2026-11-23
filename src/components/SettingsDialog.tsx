import React from 'react';

export interface SettingsDialogProps {
  isDarkTheme: boolean;
  onToggleTheme: (dark: boolean) => void;
  onDismiss: () => void;
}

export const SettingsDialog: React.FC<SettingsDialogProps> = ({ isDarkTheme, onToggleTheme, onDismiss }) => (
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
    data-testid="settings_dialog_overlay"
  >
    <div
      style={{
        width: 'min(90vw, 420px)',
        background: 'var(--pf-surface)',
        border: '1px solid var(--pf-outline)',
        borderRadius: 20,
        padding: 20,
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}
    >
      <div style={{ fontSize: 18, fontWeight: 700 }}>Studio Settings</div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span>Dark Studio Theme</span>
        <input
          type="checkbox"
          checked={isDarkTheme}
          onChange={(e) => onToggleTheme(e.target.checked)}
          style={{ width: 20, height: 20, cursor: 'pointer' }}
          data-testid="setting_dark_mode_switch"
        />
      </div>
      <div style={{ fontSize: 12, color: 'var(--pf-on-variant)' }}>
        PixelForge Studio v1.0 • ARMv8/ARMv7/x86 • Android SDK 23–37 • HTTPS Secure
      </div>
      <button
        type="button"
        onClick={onDismiss}
        style={{
          alignSelf: 'flex-end',
          padding: '8px 16px',
          borderRadius: 8,
          border: 'none',
          background: 'var(--pf-azure)',
          color: '#ffffff',
          fontWeight: 600,
          cursor: 'pointer',
        }}
      >
        Done
      </button>
    </div>
  </div>
);
