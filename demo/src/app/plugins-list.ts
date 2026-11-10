import { addIcons } from 'ionicons';

export interface PluginConfig {
  id: number;
  name: string;
  npmName: string;
  /** Ionicons name used by the list template. */
  iconName: string;
  /**
   * Ionicons SVG data, imported from `ionicons/icons` by the plugin that owns
   * this entry.
   *
   * The demo registers it under `iconName`, so `home.page.ts` never imports a
   * plugin icon directly.
   */
  icon: string;
  githubUrl: string;
  pathName: string;
  description: string;
  active: boolean;
  isVisible: boolean;
}

/**
 * Plugins listed on the home page.
 *
 * `pathName` must be absolute, because the router resolves relative commands
 * against the current activated route.
 */
export const PLUGINS: PluginConfig[] = [];

/**
 * Registers every icon declared by `plugins`, keyed by `iconName`.
 *
 * Each plugin entry owns its icon, so the demo never carries icons it does not
 * render. Exported so tests can exercise the registration without mocking the
 * `ionicons` module.
 */
export function registerPluginIcons(plugins: readonly PluginConfig[]): void {
  addIcons(Object.fromEntries(plugins.map((plugin) => [plugin.iconName, plugin.icon])));
}
