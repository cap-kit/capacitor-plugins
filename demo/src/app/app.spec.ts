import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    expect(app).toBeTruthy();
  });
});
