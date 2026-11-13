---
"@cap-kit/fortress": patch
---

Web persistence now runs on an engine chain (IndexedDB first, LocalStorage fallback, in-memory last resort) with one-time legacy migration, instead of raw LocalStorage. Also hardens non-browser contexts (SSR/Node): guarded storage probes, location-independent crypto scope, and safe capability checks.
