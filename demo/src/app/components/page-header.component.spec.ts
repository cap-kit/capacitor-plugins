import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { PageHeaderComponent } from './page-header.component';

describe('PageHeaderComponent', () => {
  let component: PageHeaderComponent;
  let fixture: ComponentFixture<PageHeaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageHeaderComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('title', () => {
    it('should render the title in ion-title', () => {
      fixture.componentRef.setInput('title', 'My Page');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      expect(compiled.querySelector('ion-title')?.textContent).toContain('My Page');
    });

    it('should render the title as the current breadcrumb', () => {
      fixture.componentRef.setInput('title', 'My Page');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const breadcrumbs = compiled.querySelectorAll('ion-breadcrumb');
      expect(breadcrumbs[1]?.textContent).toContain('My Page');
    });
  });

  describe('navigation', () => {
    it('should show a Home breadcrumb as the first entry', () => {
      fixture.componentRef.setInput('title', 'My Page');
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;
      const firstBreadcrumb = compiled.querySelector('ion-breadcrumb');
      expect(firstBreadcrumb?.textContent).toContain('Home');
    });
  });
});
