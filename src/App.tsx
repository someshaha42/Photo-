import React, { useState, useEffect } from 'react';
import { EditorScreen } from './components/EditorScreen';
import { HomeScreen, ProjectSummaryItem } from './components/HomeScreen';
import { StorageService } from './services/StorageService';
import { AudioService } from './services/AudioService';

export const App: React.FC = () => {
  const [activeProject, setActiveProject] = useState<ProjectSummaryItem | null>(null);
  const [projects, setProjects] = useState<ProjectSummaryItem[]>(() => StorageService.getProjects());
  const [isDarkTheme, setIsDarkTheme] = useState(true);

  useEffect(() => {
    StorageService.saveProjects(projects);
  }, [projects]);

  const toggleTheme = () => {
    setIsDarkTheme((prev) => {
      const next = !prev;
      document.body.className = next ? 'pf-theme-dark' : 'pf-theme-light';
      return next;
    });
  };

  const createProject = () => {
    AudioService.playShutterSound();
    const created: ProjectSummaryItem = {
      id: `proj-${Date.now()}`,
      title: `Project ${projects.length + 1}`,
      updatedAtLabel: 'Just now',
    };
    setProjects((prev) => [created, ...prev]);
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
