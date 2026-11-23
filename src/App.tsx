import React, { useState } from 'react';
import { EditorScreen } from './components/EditorScreen';
import { HomeScreen, ProjectSummaryItem } from './components/HomeScreen';

const INITIAL_PROJECTS: ProjectSummaryItem[] = [
  { id: 'proj-1', title: 'Project 1', updatedAtLabel: 'Saved locally' },
];

export const App: React.FC = () => {
  const [activeProject, setActiveProject] = useState<ProjectSummaryItem | null>(null);
  const [projects, setProjects] = useState<ProjectSummaryItem[]>(INITIAL_PROJECTS);
  const [isDarkTheme, setIsDarkTheme] = useState(true);

  const toggleTheme = () => {
    setIsDarkTheme((prev) => {
      const next = !prev;
      document.body.className = next ? 'pf-theme-dark' : 'pf-theme-light';
      return next;
    });
  };

  const createProject = () => {
    const created: ProjectSummaryItem = {
      id: `proj-${projects.length + 1}`,
      title: `Project ${projects.length + 1}`,
      updatedAtLabel: 'Just now',
    };
    setProjects((prev) => [...prev, created]);
    setActiveProject(created);
  };

  if (activeProject) {
    return (
      <EditorScreen
        projectTitle={activeProject.title}
        onNavigateBack={() => setActiveProject(null)}
      />
    );
  }

  return (
    <HomeScreen
      projects={projects}
      isDarkTheme={isDarkTheme}
      onToggleTheme={toggleTheme}
      onCreateNewProject={createProject}
      onOpenProject={(id) => {
        const found = projects.find((p) => p.id === id) ?? null;
        setActiveProject(found);
      }}
    />
  );
};

export default App;
