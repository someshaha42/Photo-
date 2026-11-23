import { ProjectSummaryItem } from '../components/HomeScreen';

const STORAGE_KEY = 'pixelforge_projects_history';

export class StorageService {
  static getProjects(): ProjectSummaryItem[] {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      if (data) {
        return JSON.parse(data);
      }
    } catch {
      // Local storage unreadable
    }
    return [{ id: 'proj-1', title: 'Project 1', updatedAtLabel: 'Saved locally' }];
  }

  static saveProjects(projects: ProjectSummaryItem[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(projects));
    } catch {
      // Local storage quota exceeded or unavailable
    }
  }

  static addProject(project: ProjectSummaryItem): void {
    const current = this.getProjects();
    this.saveProjects([project, ...current]);
  }

  static deleteProject(id: string): void {
    const current = this.getProjects();
    this.saveProjects(current.filter((p) => p.id !== id));
  }
}
