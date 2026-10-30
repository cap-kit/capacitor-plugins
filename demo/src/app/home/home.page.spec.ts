import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';

import { AboutCardComponent } from '../components/about-card.component';
import { HomePage } from './home.page';
import { PLUGINS, PluginConfig, registerPluginIcons } from '../plugins-list';

const FIXTURE_ICON = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg"></svg>';

const buildPlugin = (overrides: Partial<PluginConfig> = {}): PluginConfig => ({
  id: 1,
  name: 'Demo Plugin',
  npmName: '@cap-kit/demo-plugin',
  iconName: 'key',
  icon: FIXTURE_ICON,
  githubUrl: 'https://github.com/cap-kit/capacitor-plugins/tree/main/packages/demo-plugin',
  pathName: '/demo-plugin',
  description: 'Demo plugin for Capacitor apps',
  active: true,
  isVisible: true,
  ...overrides,
});

describe('HomePage', () => {
  let component: HomePage;
  let fixture: ComponentFixture<HomePage>;
  let compiled: HTMLElement;

  const aboutCard = (): AboutCardComponent | undefined =>
    fixture.debugElement.query(By.directive(AboutCardComponent))?.componentInstance;

  const renderPlugins = (plugins: PluginConfig[]): void => {
    component.pluginList.set(plugins);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(HomePage);
    component = fixture.componentInstance;
    compiled = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('renders the app version in the footer', () => {
    expect(compiled.querySelector('ion-footer')?.textContent).toContain('App Version');
  });

  describe('plugins list', () => {
    it('exposes the plugins from plugins-list.ts', () => {
      const expected = PLUGINS.filter((plugin) => plugin.isVisible).sort((a, b) => a.id - b.id);

      expect(component.plugins()).toEqual(expected);
    });

    it('only exposes visible plugins, sorted by id', () => {
      renderPlugins([
        buildPlugin({ id: 3, isVisible: false }),
        buildPlugin({ id: 2 }),
        buildPlugin({ id: 1 }),
      ]);

      expect(component.plugins().map((plugin) => plugin.id)).toEqual([1, 2]);
    });

    it('renders one list item per visible plugin', () => {
      renderPlugins([buildPlugin({ id: 1 }), buildPlugin({ id: 2 })]);

      expect(compiled.querySelectorAll('ion-list ion-item')).toHaveLength(2);
    });

    it('registers each plugin icon under its iconName', () => {
      const icon = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg"></svg>';

      registerPluginIcons([buildPlugin({ iconName: 'demo-icon', icon })]);

      const registry = (window as unknown as { Ionicons?: { map: Map<string, string> } }).Ionicons;

      expect(registry?.map.get('demo-icon')).toBe(icon);
    });
  });

  describe('about card', () => {
    it('renders the plugin details when exactly one plugin is visible', () => {
      const plugin = buildPlugin();
      renderPlugins([plugin]);

      const card = aboutCard();

      expect(card).toBeTruthy();
      expect(card?.title()).toBe('About this demo');
      expect(card?.subtitle()).toBe(plugin.npmName);
      expect(card?.content()).toBe(plugin.description);
      expect(card?.githubUrl()).toBe(plugin.githubUrl);
    });

    it('hides the card when more than one plugin is visible', () => {
      renderPlugins([buildPlugin({ id: 1 }), buildPlugin({ id: 2 })]);

      expect(aboutCard()).toBeUndefined();
    });

    it('hides the card when no plugin is visible', () => {
      renderPlugins([]);

      expect(aboutCard()).toBeUndefined();
    });
  });
});
