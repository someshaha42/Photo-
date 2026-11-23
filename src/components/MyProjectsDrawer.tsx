import React from 'react';
import { ProjectSummaryItem } from './HomeScreen';

export interface MyProjectsDrawerProps {
  projects: ProjectSummaryItem[];
  onClose: () => void;
  onOpenProject: (id: string) => void;
}

export const MyProjectsDrawer: React.FC<MyProjectsDrawerProps> = ({ projects, onClose, onOpenProject }) => (
  <div
    style={{
      position: 'fixed',
      inset: 0,
      background: 'rgba(0,0,0,0.5)',
      display: 'flex',
      justifyContent: 'flex-end',
      zIndex: 100,
    }}
    data-testid="my_projects_side_drawer"
  >
    <div
      style={{
        width: 'min(85vw, 320px)',
        height: '100%',
        background: 'var(--pf-surface)',
        borderLeft: '1px solid var(--pf-outline)',
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        gap: 12,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <h3 style={{ margin: 0, fontSize: 16, fontWeight: 700 }}>My Projects</h3>
        <button
          type="button"
          onClick={onClose}
          style={{ background: 'transparent', border: 'none', color: 'var(--pf-on-surface)', fontSize: 18, cursor: 'pointer' }}
          data-testid="close_projects_drawer_button"
        >
          ✕
        </button>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8, overflowY: 'auto', flex: 1 }}>
        {projects.map((p) => (
          <div
            key={p.id}
            onClick={() => {
              onOpenProject(p.id);
              onClose();
            }}
            style={{
              padding: 12,
              borderRadius: 12,
              background: 'var(--pf-surface-variant)',
              cursor: 'pointer',
              display: 'flex',
              flexDirection: 'column',
              gap: 4,
            }}
          >
            <div style={{ fontWeight: 600, fontSize: 14 }}>{p.title}</div>
            <div style={{ fontSize: 11, color: 'var(--pf-on-variant)' }}>{p.updatedAtLabel}</div>
          </div>
        ))}
      </div>
    </div>
  </div>
);
