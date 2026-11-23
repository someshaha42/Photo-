import React from 'react';

export interface EmojiSettingsPanelProps {
  onSelectEmoji: (emoji: string) => void;
}

const EMOJIS = ['😀', '😂', '🔥', '🚀', '⭐', '❤️', '🎨', '✨', '💎', '🎉', '👍', '😎'];

export const EmojiSettingsPanel: React.FC<EmojiSettingsPanelProps> = ({ onSelectEmoji }) => (
  <div style={{ padding: 14, display: 'flex', flexDirection: 'column', gap: 10 }} data-testid="emoji_settings_panel">
    <div style={{ fontWeight: 700, fontSize: 14 }}>Insert Emoji & Stickers</div>
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8 }}>
      {EMOJIS.map((em) => (
        <button
          key={em}
          type="button"
          onClick={() => onSelectEmoji(em)}
          style={{
            padding: 10,
            borderRadius: 10,
            border: '1px solid var(--pf-outline)',
            background: 'var(--pf-surface-variant)',
            fontSize: 22,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          data-testid={`emoji_item_${em}`}
        >
          {em}
        </button>
      ))}
    </div>
  </div>
);
