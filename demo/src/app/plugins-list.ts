export interface PluginConfig {
  id: number;
  name: string;
  npmName: string;
  iconName: string;
  githubUrl: string;
  pathName: string;
  description: string;
  active: boolean;
  isVisible: boolean;
}

export const PLUGINS: PluginConfig[] = [];
