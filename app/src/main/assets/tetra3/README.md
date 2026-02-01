# Tetra3 Database

This folder contains the Tetra3 star pattern database used for plate solving.

## File

- `hip_database_fov85.npz` - Hipparcos star catalog database (~50-80 MB)

## Regenerating the Database

If you need to regenerate the database:

```bash
cd tools/
pip install tetra3
python generate_tetra3_database.py
```

## Configuration

The database is configured for:
- **Max FOV**: 85 degrees (covers most smartphone main cameras)
- **Min FOV**: 10 degrees (supports telephoto)
- **Max magnitude**: 7.0 (includes dimmer stars for better matching)
