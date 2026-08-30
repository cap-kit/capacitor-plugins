import { ComponentFixture, TestBed } from '@angular/core/testing';
import { InfoCardComponent } from './info-card.component';

describe('InfoCardComponent', () => {
  let component: InfoCardComponent;
  let fixture: ComponentFixture<InfoCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InfoCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(InfoCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('title', '');
    fixture.componentRef.setInput('content', '');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('initial state', () => {
    it('should be expanded by default', () => {
      fixture.detectChanges();
      expect(component.expanded()).toBe(true);
    });
  });

  describe('inputs', () => {
    it('should display title', () => {
      fixture.componentRef.setInput('title', 'Test Title');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-card-title')?.textContent).toContain('Test Title');
    });

    it('should display subtitle when provided', () => {
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('subtitle', 'Subtitle');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-card-subtitle')?.textContent).toContain('Subtitle');
    });

    it('should not display subtitle when undefined', () => {
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('subtitle', undefined);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-card-subtitle')).toBeFalsy();
    });

    it('should display content when expanded', () => {
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('content', 'Test content');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-card-content ion-text')?.textContent).toContain(
        'Test content',
      );
    });

    it('should hide content when collapsed', () => {
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('content', 'Test content');
      component.expanded.set(false);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-card-content')).toBeFalsy();
    });
  });

  describe('toggle', () => {
    it('should toggle expanded state', () => {
      fixture.detectChanges();
      expect(component.expanded()).toBe(true);

      component.toggle();
      expect(component.expanded()).toBe(false);

      component.toggle();
      expect(component.expanded()).toBe(true);
    });

    it('should toggle via click on icon', () => {
      fixture.detectChanges();
      const toggleIcon = fixture.nativeElement.querySelector('.toggle-icon');
      toggleIcon.click();
      fixture.detectChanges();

      expect(component.expanded()).toBe(false);
    });
  });

  describe('github integration', () => {
    it('should show github icon when showGithubIcon is true', () => {
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('content', 'Content');
      fixture.componentRef.setInput('githubUrl', 'https://github.com/test');
      fixture.componentRef.setInput('showGithubIcon', true);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.github-icon')).toBeTruthy();
    });

    it('should hide github icon when showGithubIcon is false', () => {
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('content', 'Content');
      fixture.componentRef.setInput('githubUrl', 'https://github.com/test');
      fixture.componentRef.setInput('showGithubIcon', false);
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('.github-icon')).toBeFalsy();
    });

    it('should emit githubClick event when icon is clicked', () => {
      const emitSpy = vi.spyOn(component.githubClick, 'emit');
      fixture.componentRef.setInput('title', 'Title');
      fixture.componentRef.setInput('content', 'Content');
      fixture.componentRef.setInput('githubUrl', 'https://github.com/test');
      fixture.componentRef.setInput('showGithubIcon', true);
      fixture.detectChanges();

      const githubIcon = fixture.nativeElement.querySelector('.github-icon');
      githubIcon.click();

      expect(emitSpy).toHaveBeenCalled();
    });
  });

  describe('openGithub', () => {
    it('should do nothing when githubUrl is not set', () => {
      fixture.componentRef.setInput('githubUrl', undefined);

      component.openGithub();
    });

    it('should open window with githubUrl when set', () => {
      const windowOpenSpy = vi.spyOn(window, 'open');
      fixture.componentRef.setInput('githubUrl', 'https://github.com/test');

      component.openGithub();

      expect(windowOpenSpy).toHaveBeenCalledWith('https://github.com/test', '_blank');
    });
  });
});
