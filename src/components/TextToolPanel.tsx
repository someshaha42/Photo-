import React from 'react';

export interface TextToolPanelProps {
  draftText: string;
  onTextChange: (text: string) => void;
  onConfirm: () => void;
}

export const TextToolPanel: React.FC<TextToolPanelProps> = ({ draftText, onTextChange, onConfirm }) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 10 }} data-testid="text_tool_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Text Layer Editor</div>
    <input
      type="text"
      value={draftText}
      onChange={(e) => onTextChange(e.target.value)}
      placeholder="Type text here..."
      style={{
        width: '100%',
        padding: '10px 12px',
        borderRadius: 10,
        border: '1px solid var(--pf-outline)',
        background: 'var(--pf-surface-variant)',
        color: 'var(--pf-on-surface)',
        fontSize: 14,
        outline: 'none',
      }}
      data-testid="text_layer_editor_input"
    />
    <button
      type="button"
      onClick={onConfirm}
      style={{
        width: '100%',
        padding: '10px',
        borderRadius: 10,
        border: 'none',
        background: 'var(--pf-azure)',
        color: '#ffffff',
        fontWeight: 700,
        cursor: 'pointer',
      }}
      data-testid="text_confirm_button"
    >
      Confirm Text
    </button>
  </div>
);
