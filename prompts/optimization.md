# Performance Optimization Protocol

Analyze and improve the performance of a specific module or the entire application.

## 1. UI Optimization
- Audit ViewModels to move non-UI state logic out.
- Use `derivedStateOf` for complex calculations inside Composables.
- Optimize images using Coil's memory and disk cache policies.

## 2. Data Optimization
- Review Room queries for efficient indexing.
- Implement pagination for large lists in the Marketplace.
- Compress all uploads to Firebase Storage.

## 3. Metrics
- Compare startup time (pre vs. post optimization).
- Measure FPS during scrolling on high-density lists.
- Report memory footprint reduction.
