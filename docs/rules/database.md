# Database

Rules for schema changes and migrations. Read before changing any of it; review holds a diff to every line here.

- Schema changes are Room auto-migrations; exported schemas live in `data/schemas`. `MigrationTest` fills every
  column at every version and reads it back. **A new version is covered once its schema file is there; a new
  table has to be added to the test's `order` list.**
