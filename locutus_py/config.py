import os

# Discord bot token and role IDs
DISCORD_TOKEN = os.getenv('DISCORD_TOKEN')
ADMIN_ROLE_ID = int(os.getenv('ADMIN_ROLE_ID', '0'))  # Set in .env
MILCOM_ROLE_ID = int(os.getenv('MILCOM_ROLE_ID', '0'))  # Set in .env

# Database config
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = int(os.getenv('DB_PORT', '3306'))
DB_NAME = os.getenv('DB_NAME', 'locutus')
DB_USER = os.getenv('DB_USER', 'root')
DB_PASS = os.getenv('DB_PASS', '')
