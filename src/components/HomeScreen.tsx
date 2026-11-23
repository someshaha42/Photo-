import React from 'react';

export interface ProjectSummaryItem {
  id: string;
  title: string;
  updatedAtLabel: string;
}

export interface HomeScreenProps {
  projects: ProjectSummaryItem[];
  isDarkTheme: boolean;
  onToggleTheme: () => void;
  onCreateNewProject: () => void;
  onOpenProject: (id: string) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  projects,
  isDarkTheme,
  onToggleTheme,
  onCreateNewProject,
  onOpenProject,
}) => (
  <main className="pf-home-screen">
    <header className="pf-home-header">
      <div className="pf-home-brand">
        <h1 className="pf-home-title">PixelForge</h1>
        <p className="pf-home-subtitle">Offline Photo & Text Studio</p>
      </div>
      <div style={{ display: 'flex', gap: 6 }}>
        <button type="button" className="pf-touch-btn" onClick={onToggleTheme} aria-label="Toggle Theme">
          <img src="/assets/icons/ic_theme.png" alt={isDarkTheme ? 'Light Mode' : 'Dark Mode'} className="pf-icon" />
        </button>
        <button type="button" className="pf-touch-btn" aria-label="Settings">
          <img src="/assets/icons/ic_settings.png" alt="Settings" className="pf-icon" />
        </button>
      </div>
    </header>

    <button type="button" className="pf-new-project-btn" onClick={onCreateNewProject} data-testid="home_new_project_button">
      <img src="/assets/icons/ic_add.png" alt="New Project" className="pf-icon" />
      <span>New Project</span>
    </button>

    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <img src="/assets/icons/ic_history.png" alt="History" className="pf-icon pf-icon-sm" />
      <strong>History ({projects.length})</strong>
    </div>

    <div className="pf-history-grid">
      {projects.map((proj) => (
        <article key={proj.id} className="pf-project-card" onClick={() => onOpenProject(proj.id)}>
          <div className="pf-checkerboard" style={{ width: '100%', aspectRatio: '1 / 1', borderRadius: 10 }} />
          <div>
            <div style={{ fontWeight: 700, fontSize: 14 }}>{proj.title}</div>
            <div style={{ fontSize: 11, color: 'var(--pf-on-variant)' }}>{proj.updatedAtLabel}</div>
          </div>
        </article>
      ))}
    </div>
  </main>
);
