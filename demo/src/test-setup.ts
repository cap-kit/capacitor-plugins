import '@angular/compiler';
import '@analogjs/vitest-angular/setup-snapshots';
import '@analogjs/vitest-angular/setup-serializers';
import { setupTestBed } from '@analogjs/vitest-angular/setup-testbed';

// Zoneless change detection is the Angular 21+/22 default in this project,
// so we initialize the TestBed without Zone.js helpers.
setupTestBed();
