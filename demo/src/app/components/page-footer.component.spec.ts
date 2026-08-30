import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageFooterComponent } from './page-footer.component';

describe('PageFooterComponent', () => {
  let component: PageFooterComponent;
  let fixture: ComponentFixture<PageFooterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageFooterComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(PageFooterComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('version', () => {
    it('should not render version text when version is unset', () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-note span')).toBeNull();
    });

    it('should render label and version using default label', () => {
      fixture.componentRef.setInput('version', '1.2.3');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-note')?.textContent).toContain('Version: 1.2.3');
    });

    it('should render a custom label when provided', () => {
      fixture.componentRef.setInput('version', '1.2.3');
      fixture.componentRef.setInput('label', 'Build');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-note')?.textContent).toContain('Build: 1.2.3');
    });
  });
});
